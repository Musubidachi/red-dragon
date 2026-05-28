package dev.reddragon.ingestion.services.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.ingestion.models.sec.SecFiling;

class SecFilingBodyClientTest {

    @Test
    void fetchesPrimaryDocumentThroughSecHttpClient() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        URI uri = URI.create(
                "https://www.sec.gov/Archives/edgar/data/320193/000032019326000001/form4.xml");
        when(httpClient.process(uri)).thenReturn("<ownershipDocument/>");

        SecFilingBodyClient client = new SecFilingBodyClient(httpClient);

        assertEquals("<ownershipDocument/>", client.process(form4Filing("form4.xml")));
        verify(httpClient).process(uri);
    }

    @Test
    void rejectsFilingsWithoutPrimaryDocument() {
        SecHttpClient httpClient = mock(SecHttpClient.class);
        SecFilingBodyClient client = new SecFilingBodyClient(httpClient);

        assertThrows(IllegalArgumentException.class, () -> client.process(form4Filing(" ")));
        verifyNoInteractions(httpClient);
    }

    private SecFiling form4Filing(String primaryDocument) {
        return new SecFiling(
                "0000320193",
                "Apple Inc.",
                "AAPL",
                "0000320193-26-000001",
                "4",
                LocalDate.of(2026, 5, 21),
                primaryDocument,
                "Form 4",
                List.of()
        );
    }
}
