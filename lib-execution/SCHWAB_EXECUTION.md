# lib-execution — Schwab Broker Integration

Architecture doc and current implementation notes for broker integration.
`lib-execution` is now a built Maven reactor module with provider-neutral
contracts and a deterministic dry-run implementation. The current code does not
call Schwab trader order endpoints or place live broker orders.

> **Status**: dry-run module implemented; live Schwab execution remains
> deferred and fail-closed. Schwab OAuth and Schwab market-data support live
> elsewhere today: OAuth services/controllers live in `app`, the token entity
> and repository live in `lib-persistence`, and price-history retrieval lives in
> `lib-marketdata`.

Implemented outside this module:

- `GET /api/schwab/oauth/authorize-url`
- `GET /api/schwab/oauth/callback`
- `POST /api/schwab/oauth/refresh`
- persisted `schwab_token` rows
- persisted `broker_call_log` rows for Schwab OAuth token exchanges
- on-demand/scheduled access-token refresh support
- Schwab price-history provider for market data

Still not implemented:

- live Schwab account reads
- live Schwab position reads
- live Schwab order placement
- live Schwab order cancellation
- broker-side fill reconciliation
- persistent broker audit rows for order lifecycle calls
- app-level human confirmation gates

---

## RD-M17 status and promotion checklist

RD-M17 module promotion is complete for the dry-run scope. `lib-execution` is
in the parent Maven reactor and has provider-neutral account, position, order,
cancellation, and fill lifecycle models backed by `DryRunBrokerClient`.

The remaining checklist is for future live Schwab execution. Do not describe
live trading as implemented until a Schwab HTTP adapter, order-lifecycle audit
persistence, app confirmation surface, and fail-closed live-mode controls are
all verified.

Use this checklist to promote the dry-run module into live Schwab execution:

- Confirm Schwab developer approval, a linked brokerage account, and the exact
  Accounts & Trading API scopes available to the app.
- Decide the module boundary: `lib-execution` owns broker account reads,
  position reads, order construction, order placement, order cancellation, fill
  reconciliation, execution audit logging, and dry-run/live mode enforcement.
- Provider-neutral contracts are implemented: `BrokerClient`,
  `AccountSummary`, `Position`, `OrderQuery`, `Order`, `OrderRequest`,
  `OrderResponse`, and `OrderStatus`.
- Dry-run lifecycle is implemented for account reads, positions, order
  placement, idempotency, cancellation, manual fill recording, and order
  queries. Keep this behavior provider-neutral.
- Implement read-only Schwab support first: account summary, positions, orders,
  and a harmless user/account metadata call. Verify with mock HTTP tests before
  any live Schwab request is allowed.
- Define persistent state before write operations: encrypted broker token
  storage, broker call audit rows, local order state, idempotency key storage,
  and fill reconciliation records. The OAuth token path now writes broker call
  audit rows; order lifecycle rows remain future work.
- Keep `executionMode=dry-run|live` semantics fail-closed before any live order
  placement code. Dry-run must remain the default and must return synthetic
  responses without calling Schwab order-placement endpoints.
- Implement order placement in narrow increments: equity market/limit orders,
  equity cancellation, single-leg option orders, then any multi-leg option
  support only after covered-call flows are proven.
- Add human confirmation gates in the application layer before live placement.
  The library should expose explicit intent and response objects, not decide
  whether a trade should be placed.
- Verify live additions with the narrowest meaningful checks: `mvn -pl
  lib-execution test` for the module, then a reactor compile/test command after
  cross-module wiring changes.

Documentation should describe `lib-execution` as a delivered dry-run module and
live Schwab execution as future work.

---

## 1. Purpose

`lib-execution` is the layer that owns broker execution contracts. The current
module models this lifecycle in dry-run mode; future provider adapters will talk
to brokers. Its responsibilities:

- Authenticate to the broker (OAuth flow + token refresh).
- Read account state: cash, buying power, positions, recent orders.
- Construct and submit orders: equity, option, multi-leg.
- Track order lifecycle: pending → working → filled / cancelled / rejected.
- Cancel orders.

What it **does not** do:

- Decide *what* to trade. That's `lib-analytics` + `lib-validation`.
- Decide *when* to trade. That's the human, behind a deliberate confirmation gate.
- Decide *how big* a trade should be. Position sizing is a separate concern that
  requires portfolio context and a risk model.
- Reconcile fills with the trader's mental P&L. Separate operational concern.
- Run autonomously. Every order placement requires explicit human confirmation.

The platform's stated stance from day one: the system augments discretion, it
does not replace it. `lib-execution` is the mechanical layer that carries out
decisions a human already approved.

---

## 2. Provider-agnostic interface

The module mirrors `lib-marketdata`'s structure: a small abstract interface in
the root package, with provider-specific implementations in sub-packages. Today
the only provider is Schwab. The abstraction makes adding another broker (e.g.
IBKR, Tradier) a contained change rather than a rewrite.

Suggested surface (Java 21):

```java
package dev.reddragon.execution;

interface BrokerClient {
    AccountSummary getAccountSummary();
    List<Position> getPositions();
    List<Order> getOrders(OrderQuery query);
    Order getOrder(String orderId);

    OrderResponse placeOrder(OrderRequest request);
    void cancelOrder(String orderId);
}

record AccountSummary(
    String accountHash,           // provider-opaque identifier
    BigDecimal cashBalance,
    BigDecimal buyingPower,
    BigDecimal totalEquity,
    boolean dayTraderFlag
) {}

record Position(
    String symbol,
    AssetType assetType,          // EQUITY, OPTION, etc.
    BigDecimal quantity,          // negative = short
    BigDecimal averageCost,
    BigDecimal currentPrice,
    BigDecimal marketValue,
    String optionDetails          // null for equities; full OCC symbol for options
) {}

sealed interface OrderRequest
    permits EquityOrder, SingleLegOptionOrder, MultiLegOptionOrder {}

record EquityOrder(
    String symbol,
    OrderSide side,               // BUY, SELL, SELL_SHORT, BUY_TO_COVER
    BigDecimal quantity,
    OrderType type,               // MARKET, LIMIT, STOP, STOP_LIMIT
    BigDecimal limitPrice,        // null for MARKET
    Duration duration,            // DAY, GTC
    String clientOrderId          // idempotency key (see §10)
) implements OrderRequest {}

// SingleLegOptionOrder + MultiLegOptionOrder structures TBD during implementation
```

The `schwab` sub-package owns `SchwabBrokerClient implements BrokerClient` and
all Schwab-specific HTTP/JSON plumbing.

---

## 3. Schwab Developer Portal — getting access

Schwab's developer platform (post-TDA-merger) lives at <https://developer.schwab.com>.
Steps to get an API key:

1. Create a developer account at developer.schwab.com (different from your
   brokerage login).
2. Register an "Individual" application. You'll provide:
   - App name (e.g., `red-dragon-local`)
   - Callback URL — see §4
   - APIs to enable: typically **Accounts and Trading Production API** and
     **Market Data Production API**.
3. Submit for approval. **Approval is manual and can take days.** Plan for it.
4. Once approved, you get `app_key` (client_id) and `app_secret` (client_secret).

You also need a real Schwab brokerage account linked to the same identity. The
API does not work against a paper-trading sandbox the way some other brokers do.

---

## 4. OAuth 2.0 — three-legged flow

Schwab uses three-legged OAuth 2.0. The flow:

1. **Authorization request** — open the browser to:
   ```
   https://api.schwabapi.com/v1/oauth/authorize?client_id={app_key}&redirect_uri={callback}
   ```
2. User logs into Schwab, approves the app. Schwab redirects to `{callback}?code=...`
3. **Token exchange** — POST the `code` to:
   ```
   https://api.schwabapi.com/v1/oauth/token
   ```
   with form body `grant_type=authorization_code`, `code=...`, `redirect_uri=...`,
   and a Basic auth header `Authorization: Basic base64(app_key:app_secret)`.
4. Response includes `access_token` (30 min lifetime) and `refresh_token`
   (**7 day lifetime**).

### Callback URL choice

A localhost callback works for a personal tool: `https://127.0.0.1:8182/callback`.
Schwab requires HTTPS even for localhost — you'll either need a self-signed cert
the dev app trusts, or to register a callback URL on a host you control with a
real cert.

A common pattern: a one-shot CLI command (`./red-dragon schwab auth`) that:
- spins up a temporary HTTPS listener on 127.0.0.1:8182,
- opens the browser to the authorization URL,
- receives the redirect with `?code=...`,
- exchanges for tokens,
- persists `refresh_token` securely (see §7),
- exits.

This runs **once per 7 days** (or whenever the refresh token expires). Everything
else is automated against the persisted refresh token.

---

## 5. Token rotation

This is the single most operationally important part of the integration and the
biggest difference from a "set and forget" API key.

| Token         | Lifetime | When to refresh                                        |
| ------------- | -------- | ------------------------------------------------------ |
| access_token  | 30 min   | On every call: if expired or expiring in <60s, refresh |
| refresh_token | 7 days   | When it expires, the user must re-auth in the browser  |

The 7-day refresh window means you must:

- **Monitor refresh-token age.** Warn the user (email / log / dashboard) at
  T-24h to re-auth before it expires.
- **Persist the refresh token securely.** It is a long-lived credential; treat
  it like a password (see §7).
- **Auto-refresh access tokens.** A scheduled `@Scheduled` job that runs every
  25 minutes is fine, or refresh lazily on first-after-expiry call.
- **Recover gracefully on refresh failure.** If a refresh returns `400` or
  `401`, the refresh token is dead. The system must fail closed (refuse to
  place orders) and prompt re-auth.

Failing closed is the right default. Better to miss a trade than to fire one
against the wrong account.

---

## 6. API surface we'll consume

All endpoints sit under `https://api.schwabapi.com/`. The two relevant families
are `/marketdata/v1/...` and `/trader/v1/...`.

### Auth

| Method | Path                            | Purpose                                 |
| ------ | ------------------------------- | --------------------------------------- |
| POST   | `/v1/oauth/token`               | Exchange auth_code OR refresh_token     |

### Accounts (read)

| Method | Path                                              | Purpose                                  |
| ------ | ------------------------------------------------- | ---------------------------------------- |
| GET    | `/trader/v1/accounts/accountNumbers`              | Map account numbers ↔ opaque account hashes |
| GET    | `/trader/v1/accounts`                             | List accounts with balances              |
| GET    | `/trader/v1/accounts/{accountHash}`               | One account, with positions when requested |
| GET    | `/trader/v1/userPreference`                       | Account preferences, default account     |

Note: every other trader endpoint takes the **accountHash**, not the raw
account number. List once at startup, cache the mapping.

### Orders

| Method | Path                                                          | Purpose                  |
| ------ | ------------------------------------------------------------- | ------------------------ |
| GET    | `/trader/v1/accounts/{accountHash}/orders`                    | List orders by status/date |
| GET    | `/trader/v1/accounts/{accountHash}/orders/{orderId}`          | Order detail              |
| POST   | `/trader/v1/accounts/{accountHash}/orders`                    | Place an order            |
| PUT    | `/trader/v1/accounts/{accountHash}/orders/{orderId}`          | Replace an order          |
| DELETE | `/trader/v1/accounts/{accountHash}/orders/{orderId}`          | Cancel an order           |

### Quotes / market data (overlap with `lib-marketdata`)

| Method | Path                                  | Purpose                          |
| ------ | ------------------------------------- | -------------------------------- |
| GET    | `/marketdata/v1/quotes`               | Real-time quotes for one or more symbols |
| GET    | `/marketdata/v1/pricehistory`         | OHLCV bars                       |
| GET    | `/marketdata/v1/chains`               | Option chains                    |
| GET    | `/marketdata/v1/markets`              | Market hours                     |

If both `lib-marketdata` and `lib-execution` ultimately call Schwab, the OAuth
token client should live in **one** place and be injected into both. Suggested
home: `lib-execution`, since trading is the load-bearing user of the credential.

---

## 7. Secrets handling

What we hold, and where it should live:

| Item              | Sensitivity      | Suggested storage                                      |
| ----------------- | ---------------- | ------------------------------------------------------ |
| `client_id`       | Medium           | env var `SCHWAB_CLIENT_ID`                             |
| `client_secret`   | High             | env var `SCHWAB_CLIENT_SECRET`, never logged           |
| `refresh_token`   | High (long-lived) | Encrypted in `lib-persistence`, OR OS keychain         |
| `access_token`    | Medium (30 min)  | In-memory only; never written to disk                  |

Hard rules:

- Nothing secret in version control. `application.yml` references env vars; the
  actual values live in a local `.env` or shell profile.
- `client_secret` must be redacted in all logs, including request-debug logs.
- The refresh token going to disk must be encrypted with a key the user provides
  (`SCHWAB_REFRESH_KEY` env var). Loss of that key = re-auth.
- On startup, fail fast if any required env var is missing.

Storage options for the refresh token, ranked:

1. **Encrypted row in Postgres** via `lib-persistence`. Simple, portable across
   machines that share the DB. Encryption key from env var.
2. **OS keychain** (`java.util.prefs` is not a keychain; this would require
   JNI or a library like `keyring-java`). Most secure on developer machines,
   but doesn't survive machine moves.
3. **Filesystem with restrictive perms** (`~/.red-dragon/schwab-refresh.enc`,
   chmod 600). Simple. Same encryption-key requirement.

Default to option 1 for portability. Option 3 is fine for a single dev machine.

---

## 8. Order schemas

Schwab's order body is one canonical shape with conditional sub-objects. The
high-level structure:

```json
{
  "orderType": "LIMIT",
  "session": "NORMAL",
  "duration": "DAY",
  "orderStrategyType": "SINGLE",
  "price": "47.21",
  "orderLegCollection": [
    {
      "instruction": "BUY",
      "quantity": 100,
      "instrument": {
        "assetType": "EQUITY",
        "symbol": "UAL"
      }
    }
  ]
}
```

### Equity orders (simplest)

- `orderType`: `MARKET` | `LIMIT` | `STOP` | `STOP_LIMIT` | `TRAILING_STOP`
- `instruction`: `BUY` | `SELL` | `BUY_TO_COVER` | `SELL_SHORT`
- `orderStrategyType`: `SINGLE`
- `instrument.assetType`: `EQUITY`

### Single-leg option orders

Same envelope, with:

- `instruction`: `BUY_TO_OPEN` | `SELL_TO_OPEN` | `BUY_TO_CLOSE` | `SELL_TO_CLOSE`
- `instrument.assetType`: `OPTION`
- `instrument.symbol`: full OCC option symbol, e.g. `UAL   240920C00050000`

### Multi-leg option orders (verticals, iron condors, etc.)

- `orderStrategyType`: `OCO` | `TRIGGER` | (other multi-leg strategies)
- For verticals: add multiple entries to `orderLegCollection` with the
  appropriate `BUY_TO_OPEN` / `SELL_TO_OPEN` mix.
- Net debit/credit price is on the top-level `price` field.
- A spread is submitted as a single order with multiple legs, not as N orders.

### OCO and triggered orders

- `OCO`: two child orders, executing one cancels the other.
- `TRIGGER`: parent fills, child fires. Useful for "buy + place stop in same submission."

The data model in §2 starts with `EquityOrder` and `SingleLegOptionOrder`.
`MultiLegOptionOrder` can be deferred to a second implementation pass — it's
where 80% of the order-construction complexity lives.

---

## 9. Rate limits

Schwab publishes a rate limit per API key (historically around 120 req/min for
trader endpoints; verify current limits in the developer portal before
implementation). Practical guidance:

- Run a token bucket sized below the published limit (e.g. 100/min) to leave
  headroom for retries.
- Most endpoints we'll call are low-volume by nature (poll account every few
  minutes, place orders rarely). Rate limit is not the constraint we'll hit.
- Backoff on `429` with exponential + jitter, max 3 retries. After that, fail
  the operation; do not retry order placement automatically (see §10).

---

## 10. Idempotency, dry-run, audit

### Idempotency

Order placement is the one operation where retries are unsafe. If the network
drops after the request hit Schwab but before we got the response, retrying
risks a duplicate order.

Approach:

- Generate a `clientOrderId` UUID at the call site and include it on the request
  (Schwab supports a custom order tag for this purpose — confirm field name
  during implementation).
- On retry attempts after network failure, **do not retry the POST**. Instead,
  GET orders for the account, look for an order with our `clientOrderId`, and
  reconcile. Surface a clear "uncertain — investigate" state to the caller.
- Never auto-retry order placement. Never.

### Dry-run mode

A `BrokerClient` decorator that logs every order it would have placed and
returns a synthetic `OrderResponse(status=SIMULATED)` without calling Schwab.

Wire it via a Spring profile or env flag:

```
RED_DRAGON_EXECUTION_MODE=dry-run | live
```

Default to `dry-run`. The `live` mode must be set explicitly. Failing safe.

### Audit log

Every Schwab API call, request and response, written to a `broker_call_log`
table (or append-only file) with:

- timestamp
- endpoint
- request body (secrets redacted; order details NOT redacted)
- response status + body
- correlation id

Order placements specifically must also log the `clientOrderId` so a
reconciliation pass can verify on-broker presence.

---

## 11. Out of scope (explicit)

- **Autonomous trading.** Every order requires explicit human confirmation in
  whatever UI layer surfaces validation verdicts.
- **Position sizing logic.** That belongs in a future `lib-risk` or similar.
- **P&L computation across multiple fills.** That's an analytics concern.
- **Tax-lot tracking.** Schwab tracks lots; we don't need to duplicate.
- **Cross-broker routing.** One broker (Schwab) only.
- **Streaming quotes / Level 2.** REST snapshots are enough for the assist
  use case.
- **WebSocket order updates.** Polling order status every few seconds while an
  order is pending is fine; streaming is a later optimization.

---

## 12. Open decisions

1. **Refresh token storage backend.** Default: encrypted Postgres row. Alt:
   filesystem. Decide before implementation; the wrong choice is hard to
   migrate.
2. **One Schwab account or many?** The data model supports many; the UX of
   account selection is undefined.
3. **Order-confirmation surface.** CLI prompt? HTTP endpoint that returns a
   pending verdict the user accepts/rejects? Wait for the review UI to exist?
4. **Multi-leg option orders — in scope for v1 or v2?** Most of the complexity
   in order construction lives here. Defer if covered calls (single-leg) cover
   90% of intended use.
5. **How does `lib-marketdata` consume Schwab's market-data endpoints?** Share
   the OAuth client via DI, or have `lib-marketdata` define its own auth
   helper? Recommend: shared via DI; one credential, one rotation story.
6. **What does `lib-validation` need to know about portfolio state for sizing
   checks?** This is the moment we have to decide whether validation gets
   read access to broker positions. The earlier decision was "no portfolio
   awareness until execution exists." Dry-run execution now exists; revisit only
   when live position reads are implemented and audited.

---

## 13. Implementation order and remaining live work

1. **Provider-agnostic interface** in `dev.reddragon.execution`: `BrokerClient`,
   `AccountSummary`, `Position`, the `OrderRequest` hierarchy, `Order`,
   `OrderResponse`, and `OrderStatus`. Implemented for dry-run behavior.
2. **`SchwabHttpClient`** wrapper around Spring's `RestClient` with: bearer
   token injection, gzip, rate limiter, backoff. Test against a harmless
   endpoint like `/trader/v1/userPreference`.
3. **One-shot OAuth bootstrap CLI**: `./red-dragon schwab auth` that performs
   the three-legged flow, persists the refresh token, exits.
4. **Scheduled access-token refresh job** (every 25 min).
5. **Refresh-token aging warning** (T-24h alert when refresh expires soon).
6. **Read-only `SchwabBrokerClient`**: implement `getAccountSummary`,
   `getPositions`, `getOrders`, `getOrder` only. No `placeOrder` yet.
7. **Add `executionMode` config** (`dry-run` | `live`) to app wiring before
   any live broker client is selectable. Dry-run module behavior already
   exists and must stay the default.
8. **`placeOrder` for `EquityOrder`** — market and limit. Idempotency key
   wired in. Confirm reconciliation logic on simulated network failures.
9. **`cancelOrder`.**
10. **`placeOrder` for `SingleLegOptionOrder`** (covered calls are the
    primary use case).
11. **Audit log table + lifecycle**: every call logged.
12. **`MultiLegOptionOrder`** (deferred; only if needed).

Each remaining step is independently testable. The risk profile rises only when
live write endpoints are added.

---

## 14. Cross-module impacts

Adopting this changes assumptions elsewhere:

- **`lib-marketdata`**: Schwab is now the de-facto first market-data provider.
  The provider-agnostic interface stays, but the Schwab impl can share auth
  with `lib-execution`. No code changes required today.
- **`lib-validation`**: an earlier note said the validator does not know about
  current portfolio. When the execution module exists, sizing rules that require
  portfolio context (e.g. "you already hold UUUU at 95% of book — reject
  additional long exposure") become possible. Decide whether to use that
  capability or keep validation portfolio-blind.
- **`lib-persistence`**: new tables needed: `broker_token` (encrypted refresh
  token), `broker_call_log` (audit), optionally `local_order_state` (mirror of
  orders we placed, with our `clientOrderId`).
- **`app`**: needs new endpoints for the confirmation UI flow and the OAuth
  callback handler.

---

## 15. References

- Schwab Developer Portal: <https://developer.schwab.com>
- Schwab Trader API (Accounts & Trading): <https://developer.schwab.com/products/trader-api--individual>
- Schwab Market Data Production API: <https://developer.schwab.com/products/market-data-production>
- OAuth 2.0 RFC 6749 (authorization code flow): <https://www.rfc-editor.org/rfc/rfc6749>
- OCC option symbology (for option symbol construction):
  <https://en.wikipedia.org/wiki/Option_symbol#OCC_Option_Symbology_Initiative>

---

## 16. Notes for future-Claude

- This was scoped as "doc only" by Fab on 2026-05-10. Do not start writing
  Schwab code without re-confirming scope — token rotation and order placement
  are both load-bearing security/correctness work.
- The user trades a Roth IRA at Schwab and runs a covered-call premium engine.
  That use case is single-leg option orders, sell-to-open / buy-to-close, on
  positions already held. It's a much smaller surface than full multi-leg.
- Dry-run mode is not optional. It must be the default until live trading is
  explicitly enabled.
