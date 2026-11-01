package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static football.model.Position.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchEngineTest {

    private static Player gk(String name) {
        return new GoalKeeper(name, 4, 4, 4, 3, 3, 3, 3);
    }

    private static Player outfield(String name, Position position) {
        return new Player(name, position, 4, 4, 4, 4, 4, 4);
    }

    private static List<Player> lineup(String prefix) {
        return new ArrayList<>(List.of(
                gk(prefix + "_GK1"),
                outfield(prefix + "_CB1", CENTER_BACK), outfield(prefix + "_CB2", CENTER_BACK),
                outfield(prefix + "_FB1", FULLBACK), outfield(prefix + "_FB2", FULLBACK),
                outfield(prefix + "_MF1", MIDFIELD), outfield(prefix + "_MF2", MIDFIELD),
                outfield(prefix + "_MW1", MIDWING), outfield(prefix + "_MW2", MIDWING),
                outfield(prefix + "_ST1", STRIKER), outfield(prefix + "_ST2", STRIKER)));
    }

    private static List<Player> bench(String prefix) {
        return new ArrayList<>(List.of(
                gk(prefix + "_GK2"), outfield(prefix + "_BCB", CENTER_BACK), outfield(prefix + "_BMF", MIDFIELD),
                outfield(prefix + "_BFW", FRONTWING), outfield(prefix + "_BST", STRIKER)));
    }

    private static TeamSetup createTeam(String prefix, Tactic tactic) {
        return TeamSetup.create(tactic, new Formation442(), Stance.NORMAL, lineup(prefix), bench(prefix));
    }

    @Test
    void matchEngineIsDeterministicWithSameSeed() {
        TeamSetup home = createTeam("H", new CounterAttack());
        TeamSetup away = createTeam("A", new Possession());

        MatchEngine engine1 = new MatchEngine(12345L);
        PeriodResult result1 = engine1.simulatePeriod(1, 1, 45, home, away);

        MatchEngine engine2 = new MatchEngine(12345L);
        PeriodResult result2 = engine2.simulatePeriod(1, 1, 45, home, away);

        assertEquals(result1.getHomeGoals(), result2.getHomeGoals());
        assertEquals(result1.getAwayGoals(), result2.getAwayGoals());
        assertEquals(result1.getEvents().size(), result2.getEvents().size());
    }

    @Test
    void periodResultStoresCorrectPeriodNumber() {
        TeamSetup home = createTeam("H", new CounterAttack());
        TeamSetup away = createTeam("A", new Possession());
        MatchEngine engine = new MatchEngine(42L);

        PeriodResult p1 = engine.simulatePeriod(1, 1, 45, home, away);
        PeriodResult p2 = engine.simulatePeriod(2, 46, 90, home, away);

        assertEquals(1, p1.getPeriodNumber());
        assertEquals(2, p2.getPeriodNumber());
    }
}