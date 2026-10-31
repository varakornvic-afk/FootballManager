package football.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticTest {
    private final Tactic counter = new CounterAttack();
    private final Tactic possession = new Possession();
    private final Tactic wide = new WidePlay();

    @Test
    void counterBeatsPossession() {
        assertTrue(counter.beats(possession));
    }

    @Test
    void possessionBeatsWidePlay() {
        assertTrue(possession.beats(wide));
    }

    @Test
    void widePlayBeatsCounter() {
        assertTrue(wide.beats(counter));
    }

    @Test
    void reverseDirectionIsNotAWin() {
        assertFalse(possession.beats(counter));
        assertFalse(wide.beats(possession));
        assertFalse(counter.beats(wide));
    }

    @Test
    void tacticNeverBeatsItself() {
        Tactic[] all = {counter, possession, wide};
        for (Tactic t : all) {
            assertFalse(t.beats(t));
        }
    }

    @Test
    void everyTacticBeatsExactlyOneOther() {
        Tactic[] all = {counter, possession, wide};
        for (Tactic a : all) {
            int wins = 0;
            for (Tactic b : all) {
                if (a.beats(b)) wins++;
            }
            assertEquals(1, wins);
        }
    }
}