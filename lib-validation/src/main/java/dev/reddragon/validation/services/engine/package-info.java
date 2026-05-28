/**
 * L3/L5 validation engine internals: hard-gate evaluation, factor scoring,
 * verdict resolution, and deployment-tier resolution.
 *
 * <p>Public entry point is {@link
 * dev.reddragon.validation.services.engine.DisequilibriumValidationEngine},
 * which composes the other classes in this package. Production callers can
 * pass thresholds, while tests or app wiring can inject subservices directly.
 */
package dev.reddragon.validation.services.engine;
