package dev.reddragon.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdversarialFindingTest {

    @Test
    void severityClampsAsDerivedScore() {
        assertEquals(1.0, finding(1.4).severity());
        assertEquals(0.0, finding(-0.4).severity());
    }

    @Test
    void severityRejectsNaN() {
        assertThrows(IllegalArgumentException.class, () -> finding(Double.NaN));
    }

    private AdversarialFinding finding(double severity) {
        return new AdversarialFinding(
                AdversarialFindingType.HYPE_WITHOUT_STRUCTURE,
                severity,
                null
        );
    }
}
