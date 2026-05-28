package dev.reddragon.ingestion.services.sec;

import dev.reddragon.ingestion.models.sec.SecFiling;

class CountingSecFilingBodyClient extends SecFilingBodyClient {

    private final String responseBody;
    private int calls;

    CountingSecFilingBodyClient(String responseBody) {
        super(new RecordingSecHttpClient(responseBody));
        this.responseBody = responseBody;
    }

    @Override
    public String process(SecFiling filing) {
        calls++;
        return responseBody;
    }

    int calls() {
        return calls;
    }
}
