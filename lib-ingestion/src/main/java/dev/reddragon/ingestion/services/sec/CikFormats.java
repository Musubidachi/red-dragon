package dev.reddragon.ingestion.services.sec;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * SEC CIK and accession-number normalisation in one place.
 *
 * <p>EDGAR exposes the same identifiers in two formats depending on the
 * endpoint: the submissions API expects 10-digit zero-padded CIKs
 * ({@code 0000320193}), while the Archives URLs strip leading zeros
 * ({@code 320193}). Accession numbers appear both with dashes
 * ({@code 0001234567-25-001234}) and without
 * ({@code 0001234567250012345}). Forcing every call site to handle both
 * conversions inline is error-prone — see {@code lib-ingestion/REVIEW.md}
 * findings #12, #15, #16. This utility owns the rules.
 *
 * <p>All methods are stateless and safe to call from any thread.
 */
public final class CikFormats {

    /** Width of the canonical zero-padded CIK (submissions endpoint form). */
    public static final int PADDED_WIDTH = 10;

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

    private CikFormats() {
        // utility
    }

    /**
     * Normalise any reasonable CIK string ({@code "320193"},
     * {@code "0000320193"}, {@code "CIK0000320193"}, {@code "  cik320193 "})
     * to the 10-digit zero-padded form ({@code "0000320193"}).
     *
     * @throws IllegalArgumentException on null, blank, non-digit characters
     *         after stripping the optional {@code CIK} prefix, or values that
     *         exceed {@link #PADDED_WIDTH} digits.
     */
    public static String padCik(String cikInput) {
        String digits = digits(cikInput);
        if (digits.length() > PADDED_WIDTH) {
            throw new IllegalArgumentException(
                    "CIK has more than " + PADDED_WIDTH + " digits: \"" + cikInput + "\"");
        }
        return "0".repeat(PADDED_WIDTH - digits.length()) + digits;
    }

    /**
     * Normalise the input to the leading-zeros-stripped form
     * ({@code "320193"}) used by Archives URLs. {@code "0"} stays as
     * {@code "0"} — never returns an empty string for a valid CIK.
     *
     * @throws IllegalArgumentException on null, blank, or non-digit input
     *         (after stripping the optional {@code CIK} prefix).
     */
    public static String stripLeadingZeros(String cikInput) {
        String digits = digits(cikInput);
        int firstNonZero = 0;
        while (firstNonZero < digits.length() - 1 && digits.charAt(firstNonZero) == '0') {
            firstNonZero++;
        }
        return digits.substring(firstNonZero);
    }

    /**
     * Strip the dashes from an accession number. Null-tolerant: returns
     * {@code ""} when the input is null or blank so URL builders can fold
     * the missing segment without an NPE.
     */
    public static String accessionNoDashes(String accessionNumber) {
        if (accessionNumber == null) {
            return "";
        }
        return accessionNumber.replace("-", "");
    }

    /**
     * URL-encode a primary-document filename for inclusion in an Archives
     * path segment. SEC document names occasionally contain spaces or
     * accented characters (e.g. {@code "Form 8-K Exhibit.pdf"}) — the raw
     * value cannot be concatenated into a URL safely.
     *
     * <p>The encoding follows {@code application/x-www-form-urlencoded}
     * rules with one fixup: {@code +} is replaced with {@code %20} since
     * the result is a path segment, not a form value, and SEC servers
     * expect a literal space encoded as {@code %20}.
     *
     * <p>Null-tolerant: returns {@code ""} for null input.
     */
    public static String encodePathSegment(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Returns true if the input is non-null, non-blank, and parses as a CIK
     * (digits with optional {@code CIK} prefix and within {@link #PADDED_WIDTH}
     * digits). Useful when a call site wants to silently skip rather than
     * throw on a malformed identifier.
     */
    public static boolean looksLikeCik(String cikInput) {
        try {
            String digits = digits(cikInput);
            return digits.length() <= PADDED_WIDTH;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String digits(String cikInput) {
        if (cikInput == null) {
            throw new IllegalArgumentException("CIK is required");
        }
        String trimmed = cikInput.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("CIK is required");
        }
        // Tolerant of "CIK", "cik", "Cik" prefix.
        if (trimmed.length() > 3) {
            String head = trimmed.substring(0, 3);
            if (head.equalsIgnoreCase("CIK")) {
                trimmed = trimmed.substring(3);
            }
        }
        if (trimmed.isEmpty() || !DIGITS_ONLY.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "CIK must be digits (with optional \"CIK\" prefix): \"" + cikInput + "\"");
        }
        return trimmed;
    }
}
