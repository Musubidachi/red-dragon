package dev.reddragon.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson DTO for the Schwab {@code /oauth/token} response (both
 * authorization-code and refresh-token grants return the same shape).
 *
 * <p>Field names match Schwab's wire format exactly so deserialization is
 * direct; downstream code converts these into a {@code SchwabTokenEntity}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SchwabTokenResponse(
        @JsonProperty("access_token")  String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type")    String tokenType,
        @JsonProperty("expires_in")    long   expiresInSeconds
) {
}
