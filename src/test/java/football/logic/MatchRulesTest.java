package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void specialistGainsMoreWhenGuessingRight() {
        double specialist = MatchRules.totalModifier(new CounterAttack(), new Possession(), new Formation532());
        double versatile = MatchRules.totalModifier(new CounterAttack(), new Possession(), new Formation442());
        assertTrue(specialist > versatile);
    }

    @Test
    void specialistLosesMoreWhenGuessingWrong() {
        double specialist = MatchRules.totalModifier(new CounterAttack(), new WidePlay(), new Formation532());
        double versatile = MatchRules.totalModifier(new CounterAttack(), new WidePlay(), new Formation442());
        assertTrue(specialist < versatile);
    }

    @Test
    void totalModifierStaysInsideLimits() {
        Tactic[] tactics = {new CounterAttack(), new Possession(), new WidePlay()};
        Formation[] formations = {new Formation532(), new Formation433(), new Formation343(), new Formation442()};
        for (Formation f : formations) {
            for (Tactic mine : tactics) {
                for (Tactic theirs : tactics) {
                    double m = MatchRules.totalModifier(mine, theirs, f);
                    assertTrue(m >= MatchRules.MIN_MODIFIER && m <= MatchRules.MAX_MODIFIER);
                }
            }
        }
    }
}