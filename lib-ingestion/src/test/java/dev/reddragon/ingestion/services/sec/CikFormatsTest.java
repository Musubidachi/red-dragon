package dev.reddragon.ingestion.services.sec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for {@link CikFormats} — the single source of truth for
 * CIK and accession-number normalisation across lib-ingestion.
 */
class CikFormatsTest {

    // ---- padCik ----------------------------------------------------------

    @Test
    void padCikPadsBareDigits() {
        assertEquals("0000320193", CikFormats.padCik("320193"));
    }

    @Test
    void padCikAcceptsAlreadyPadded() {
        assertEquals("0000320193", CikFormats.padCik("0000320193"));
    }

    @Test
    void padCikStripsCikPrefix() {
        assertEquals("0000320193", CikFormats.padCik("CIK0000320193"));
        assertEquals("0000320193", CikFormats.padCik("cik320193"));
        assertEquals("0000320193", CikFormats.padCik(" Cik 0000320193 ".replace(" ", "")));
    }

    @Test
    void padCikTrimsWhitespace() {
        assertEquals("0000320193", CikFormats.padCik("  320193  "));
    }

    @Test
    void padCikRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CikFormats.padCik(null));
    }

    @Test
    void padCikRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> CikFormats.padCik("   "));
    }

    @Test
    void padCikRejectsNonDigits() {
        assertThrows(IllegalArgumentException.class, () -> CikFormats.padCik("12abc"));
        assertThrows(IllegalArgumentException.class, () -> CikFormats.padCik("-320193"));
    }

    @Test
    void padCikRejectsOverlongInput() {
        assertThrows(IllegalArgumentException.class, () -> CikFormats.padCik("12345678901"));
    }

    // ---- stripLeadingZeros ----------------------------------------------

    @Test
    void stripLeadingZerosStripsPadded() {
        assertEquals("320193", CikFormats.stripLeadingZeros("0000320193"));
    }

    @Test
    void stripLeadingZerosPassesThroughBareDigits() {
        assertEquals("320193", CikFormats.stripLeadingZeros("320193"));
    }

    @Test
    void stripLeadingZerosKeepsSingleZero() {
        // "0" is a degenerate but valid CIK input — must not become "".
        assertEquals("0", CikFormats.stripLeadingZeros("0000000000"));
    }

    @Test
    void stripLeadingZerosAcceptsCikPrefix() {
        assertEquals("320193", CikFormats.stripLeadingZeros("CIK0000320193"));
    }

    @Test
    void stripLeadingZerosRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CikFormats.stripLeadingZeros(null));
    }

    // ---- accessionNoDashes ----------------------------------------------

    @Test
    void accessionNoDashesStripsDashes() {
        assertEquals("000123456725001234",
                CikFormats.accessionNoDashes("0001234567-25-001234"));
    }

    @Test
    void accessionNoDashesNullSafe() {
        assertEquals("", CikFormats.accessionNoDashes(null));
    }

    @Test
    void accessionNoDashesPreservesAlreadyJoined() {
        assertEquals("000123456725001234",
                CikFormats.accessionNoDashes("000123456725001234"));
    }

    // ---- encodePathSegment ----------------------------------------------

    @Test
    void encodePathSegmentEncodesSpacesAsPercent20() {
        // Form-URL-encoded "+" must be replaced with %20 since this is a path
        // segment, not a form field.
        assertEquals("Form%208-K%20Exhibit.pdf",
                CikFormats.encodePathSegment("Form 8-K Exhibit.pdf"));
    }

    @Test
    void encodePathSegmentEncodesAccentedCharacters() {
        assertEquals("r%C3%A9sum%C3%A9.htm",
                CikFormats.encodePathSegment("résumé.htm"));
    }

    @Test
    void encodePathSegmentPassesThroughSafeAscii() {
        assertEquals("ndnc-20250501x10q.htm",
                CikFormats.encodePathSegment("ndnc-20250501x10q.htm"));
    }

    @Test
    void encodePathSegmentNullSafe() {
        assertEquals("", CikFormats.encodePathSegment(null));
    }

    // ---- looksLikeCik ----------------------------------------------------

    @Test
    void looksLikeCikTrueForValid() {
        assertTrue(CikFormats.looksLikeCik("320193"));
        assertTrue(CikFormats.looksLikeCik("0000320193"));
        assertTrue(CikFormats.looksLikeCik("CIK0000320193"));
    }

    @Test
    void looksLikeCikFalseForInvalid() {
        assertFalse(CikFormats.looksLikeCik(null));
        assertFalse(CikFormats.looksLikeCik(""));
        assertFalse(CikFormats.looksLikeCik("12abc"));
        assertFalse(CikFormats.looksLikeCik("12345678901"));
    }
}
