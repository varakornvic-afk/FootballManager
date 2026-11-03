package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchRulesTest {
    private static final double DELTA = 0.0001;

    @Test
    void winningMatchupGivesFixedBonus() {
        double m = MatchRules.tacticModifier(new CounterAttack(), new Possession(), new Formation442());
        assertEquals(1.25, m, DELTA);
    }

    @Test
    void drawIsNeutral() {
        double m = MatchRules.tacticModifier(new WidePlay(), new WidePlay(), new Formation343());
        assertEquals(1.0, m, DELTA);
    }

    @Test
    void losingCostsMoreForExposedFormation() {
        double safe = MatchRules.tacticModifier(new Possession(), new CounterAttack(), new Formation442());
        double risky = MatchRules.tacticModifier(new Possession(), new CounterAttack(), new Formation433());
        assertEquals(0.9, safe, DELTA);
        assertEquals(0.7, risky, DELTA);
    }

}