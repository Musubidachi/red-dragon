package dev.reddragon.app.config;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CANDIDATE_STREAM_PATH = "/api/review/candidates/stream";

    private final String apiKey;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ApiKeyAuthenticationFilter(String apiKey, AuthenticationEntryPoint authenticationEntryPoint) {
        this.apiKey = apiKey;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (validRequest(request)) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "api-key",
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_API"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Missing or invalid bearer token"));
    }

    private boolean validRequest(HttpServletRequest request) {
        return validAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION))
                || validStreamApiKey(request);
    }

    private boolean validAuthorization(String authorization) {
        return StringUtils.hasText(apiKey)
                && StringUtils.hasText(authorization)
                && authorization.startsWith(BEARER_PREFIX)
                && apiKey.equals(authorization.substring(BEARER_PREFIX.length()));
    }

    private boolean validStreamApiKey(HttpServletRequest request) {
        return StringUtils.hasText(apiKey)
                && CANDIDATE_STREAM_PATH.equals(request.getRequestURI())
                && apiKey.equals(request.getParameter("apiKey"));
    }
}
