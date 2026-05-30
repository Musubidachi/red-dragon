package dev.reddragon.app.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "red-dragon.security.api-key=test-key",
                "red-dragon.sample-data.enabled=false"
        })
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiEndpointsRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/candidates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightDoesNotRequireBearerToken() throws Exception {
        mockMvc.perform(options("/api/candidates")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    void openApiDocsDoNotRequireBearerToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void eventSourceCandidateStreamCanAuthenticateWithApiKeyQueryParameter() throws Exception {
        mockMvc.perform(get("/api/review/candidates/stream?apiKey=test-key"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }
}
