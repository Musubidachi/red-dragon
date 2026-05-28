/**
 * Provider-neutral broker execution contracts and dry-run lifecycle support.
 *
 * <p>This package intentionally does not contain live broker HTTP integration.
 * Applications must use {@link dev.reddragon.execution.DryRunBrokerClient}
 * until a provider-specific live client is implemented and explicitly wired.
 */
package dev.reddragon.execution;
