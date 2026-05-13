package dev.reddragon.marketdata.provider.schwab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchwabMarketDataPropertiesTest {

    @Test
    void configuredRequiresEnabledAndToken() {
        SchwabMarketDataProperties disabled = new SchwabMarketDataProperties("https://example.com", "token", false, 10, 3, 250);
        SchwabMarketDataProperties missingToken = new SchwabMarketDataProperties("https://example.com", "", true, 10, 3, 250);
        SchwabMarketDataProperties ready = new SchwabMarketDataProperties("https://example.com", "token", true, 10, 3, 250);

        assertFalse(disabled.configured());
        assertFalse(missingToken.configured());
        assertTrue(ready.configured());
    }
}
