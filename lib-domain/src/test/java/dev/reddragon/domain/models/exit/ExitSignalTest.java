package dev.reddragon.domain.models.exit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ExitSignal's clamping and defensive copy of the notes list.
 */
class ExitSignalTest {

    @Test
    void compressionScoreIsClampedToOne() {
        ExitSignal s = new ExitSignal(ExitRecommendation.EXIT_NOW, 1.5, List.of("note"));
        assertEquals(1.0, s.compressionScore());
    }

    @Test
    void compressionScoreIsClampedToZero() {
        ExitSignal s = new ExitSignal(ExitRecommendation.HOLD, -0.3, List.of());
        assertEquals(0.0, s.compressionScore());
    }

    @Test
    void nullNotesBecomeEmptyList() {
        ExitSignal s = new ExitSignal(ExitRecommendation.HOLD, 0.1, null);
        assertTrue(s.notes().isEmpty());
    }

    @Test
    void notesListIsDefensivelyCopied() {
        List<String> mutable = new ArrayList<>();
        mutable.add("first");
        ExitSignal s = new ExitSignal(ExitRecommendation.HOLD, 0.2, mutable);

        // mutating the source after construction must not affect the signal
        mutable.add("second");
        assertEquals(1, s.notes().size());
        assertEquals("first", s.notes().get(0));

        // the held list itself must be immutable
        assertThrows(UnsupportedOperationException.class, () -> s.notes().add("third"));
    }

    @Test
    void nullRecommendationRejected() {
        assertThrows(NullPointerException.class,
                () -> new ExitSignal(null, 0.5, List.of()));
    }
}
