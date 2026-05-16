package dev.reddragon.app.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring {@code @Conditional} guard that activates the Schwab OAuth bean
 * graph only when the three minimum OAuth properties are set:
 * <ul>
 *   <li>{@code red-dragon.schwab.client-id}</li>
 *   <li>{@code red-dragon.schwab.client-secret}</li>
 *   <li>{@code red-dragon.schwab.token-url}</li>
 * </ul>
 *
 * <p>Used by {@code PipelineConfiguration#schwabOAuthService}. When this
 * condition is false, the system falls back to the static access-token
 * supplier baked into {@code SchwabMarketDataProperties}.
 */
public class SchwabOAuthConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return present(env, "red-dragon.schwab.client-id")
                && present(env, "red-dragon.schwab.client-secret")
                && present(env, "red-dragon.schwab.token-url");
    }

    private boolean present(Environment env, String key) {
        String value = env.getProperty(key);
        return value != null && !value.isBlank();
    }
}
