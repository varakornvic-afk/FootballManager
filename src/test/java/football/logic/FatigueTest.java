package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FatigueTest {
    private static final double EPS = 1e-9;

    private static Player outfield(String name, Position position) {
        return new Player(name, position, 4, 4, 4, 4, 4, 4); // stamina 4
    }

    @Test
    void aFreshPlayerHasNoPenalty() {
        assertEquals(1.0, Fatigue.factor(outfield("A", Position.STRIKER)), EPS);
    }

    @Test
    void anExhaustedPlayerPlaysAtTheMinimumFactor() {
        Player p = outfield("A", Position.STRIKER);
        p.setStamina(0);

        assertEquals(Fatigue.MIN_FACTOR, Fatigue.factor(p), EPS);
    }

    @Test
    void factorFallsInStepsBetweenFullAndEmpty() {
        Player p = outfield("A", Position.MIDFIELD);
        p.setStamina(2); // ครึ่งหนึ่งของ 4

        assertEquals(0.85, Fatigue.factor(p), EPS);
        assertTrue(Fatigue.factor(p) < 1.0);
        assertTrue(Fatigue.factor(p) > Fatigue.MIN_FACTOR);
    }

    @Test
    void theSameDropTiresLowStaminaPlayersMore() {
        Player low = new Player("L", Position.STRIKER, 4, 4, 4, 2, 4, 6);  // stamina 2
        Player high = new Player("H", Position.STRIKER, 4, 4, 4, 6, 4, 2); // stamina 6
        low.reduceStamina(1);
        high.reduceStamina(1);

        assertTrue(Fatigue.factor(low) < Fatigue.factor(high));
    }

    @Test
    void goalkeepersNeverTire() {
        GoalKeeper gk = new GoalKeeper("G", 3, 3, 3, 3, 3, 3, 6);
        gk.setStamina(0);

        assertEquals(1.0, Fatigue.factor(gk), EPS);
    }

    @Test
    void teamAverageIgnoresTheGoalkeeper() {
        List<Player> starters = new ArrayList<>();
        GoalKeeper gk = new GoalKeeper("G", 3, 3, 3, 3, 3, 3, 6);
        gk.setStamina(0);
        starters.add(gk);
        Position[] positions = {Position.CENTER_BACK, Position.CENTER_BACK, Position.FULLBACK, Position.FULLBACK,
                Position.MIDFIELD, Position.MIDFIELD, Position.MIDWING, Position.MIDWING,
                Position.STRIKER, Position.STRIKER};
        for (int i = 0; i < positions.length; i++) {
            Player p = outfield("P" + i, positions[i]);
            if (i < 5) p.setStamina(0); // ครึ่งหนึ่งหมดแรง
            starters.add(p);
        }
        List<Player> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) bench.add(outfield("B" + i, Position.CENTER_BACK));
        TeamSetup team = TeamSetup.create(new CounterAttack(), new Formation442(), Stance.NORMAL, starters, bench);

        // 5 คนหมดแรง (0.7) + 5 คนสด (1.0) = เฉลี่ย 0.85 GK ไม่นับ
        assertEquals(0.85, Fatigue.averageFactor(team), EPS);
    }
}