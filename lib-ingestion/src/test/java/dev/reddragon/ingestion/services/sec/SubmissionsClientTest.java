package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.reddragon.ingestion.config.SecApiProperties;
import dev.reddragon.ingestion.models.sec.SubmissionsFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsRecentFilings;
import dev.reddragon.ingestion.models.sec.SubmissionsResponse;

class SubmissionsClientTest {

    @Test
    void usesInjectedObjectMapperForParsing() throws Exception {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        SubmissionsResponse response = response();

        when(httpClient.process(URI.create("https://data.sec.gov/submissions/CIK0000320193.json")))
                .thenReturn("{}");
        when(objectMapper.readValue("{}", SubmissionsResponse.class)).thenReturn(response);

        SubmissionsClient client = new SubmissionsClient(properties(), httpClient, objectMapper);

        assertSame(response, client.process("320193"));
        verify(objectMapper).readValue("{}", SubmissionsResponse.class);
    }

    private SecApiProperties properties() {
        return new SecApiProperties(
                "red-dragon (ops@reddragon.dev)",
                "https://data.sec.gov/submissions",
                10
        );
    }

    private SubmissionsResponse response() {
        return new SubmissionsResponse(
                "0000320193",
                "Apple Inc.",
                List.of("AAPL"),
                new SubmissionsFilings(new SubmissionsRecentFilings(
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
                ))
        );
    }
}
