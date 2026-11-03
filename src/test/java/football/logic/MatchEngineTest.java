package football.logic;

import football.model.*;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Simulation ผลลัพธ์ต้องเหมือนเดิม 100% เมื่อใช้ Seed เดียวกัน")
    void matchEngineIsDeterministicWithSameSeed() {
        TeamSetup home = createTeam("H", new CounterAttack());
        TeamSetup away = createTeam("A", new Possession());

        MatchEngine engine1 = new MatchEngine(12345L);
        MatchHalfResult result1 = engine1.simulatePeriod(1, 45, home, away);

        MatchEngine engine2 = new MatchEngine(12345L);
        MatchHalfResult result2 = engine2.simulatePeriod(1, 45, home, away);

        assertAll("Deterministic Results Match",
                () -> assertEquals(result1.getHomeGoals(), result2.getHomeGoals(), "Home goals should match"),
                () -> assertEquals(result1.getEvents().size(), result2.getEvents().size(), "Event count should match")
        );
    }
}