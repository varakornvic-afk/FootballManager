package football.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationTest {
    private static final double DELTA = 0.0001;

    private final Formation[] all = {
            new Formation532(), new Formation433(), new Formation343(), new Formation442()
    };

    @Test
    void everyFormationTotalsFifteen() {
        for (Formation f : all) {
            assertEquals(15, f.getAttack() + f.getControl() + f.getDefense());
        }
    }

    @Test
    void specialistFitsItsOwnTactic() {
        assertEquals(1.15, new Formation532().fitFor(new CounterAttack()), DELTA);
        assertEquals(1.15, new Formation433().fitFor(new Possession()), DELTA);
        assertEquals(1.15, new Formation343().fitFor(new WidePlay()), DELTA);
    }

    @Test
    void specialistFitsOtherTacticsWorse() {
        assertEquals(0.95, new Formation532().fitFor(new Possession()), DELTA);
        assertEquals(0.95, new Formation433().fitFor(new WidePlay()), DELTA);
        assertEquals(0.95, new Formation343().fitFor(new CounterAttack()), DELTA);
    }

    @Test
    void fiveThreeTwoFitsWidePlayWorstOfAll() {
        Formation f = new Formation532();
        assertEquals(0.90, f.fitFor(new WidePlay()), DELTA);
        assertTrue(f.fitFor(new WidePlay()) < f.fitFor(new Possession()));
    }

    @Test
    void versatileFormationFitsEveryTacticTheSame() {
        Formation f = new Formation442();
        assertEquals(1.05, f.fitFor(new CounterAttack()), DELTA);
        assertEquals(1.05, f.fitFor(new Possession()), DELTA);
        assertEquals(1.05, f.fitFor(new WidePlay()), DELTA);
    }

    @Test
    void specialistsAreMoreExposedThanVersatile() {
        assertTrue(new Formation532().getExposure() > new Formation442().getExposure());
    }
}