package dev.reddragon.ingestion.services.sec;

import java.net.URI;

import dev.reddragon.ingestion.config.SecApiProperties;

class RecordingSecHttpClient extends SecHttpClient {

    private final String responseBody;
    private URI requestedUri;

    RecordingSecHttpClient(String responseBody) {
        super(new SecApiProperties(
                        "red-dragon tests (contact@reddragon.dev)",
                        "https://data.sec.gov/submissions",
                        1),
                new SimpleRateLimiter(1));
        this.responseBody = responseBody;
    }

    @Override
    public String process(URI uri) {
        requestedUri = uri;
        return responseBody;
    }

    URI requestedUri() {
        return requestedUri;
    }
}
