package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static football.model.Position.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

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

    private static TeamSetup createTeam(String prefix) {
        return TeamSetup.create(new CounterAttack(), new Formation442(), Stance.NORMAL, lineup(prefix), bench(prefix));
    }

    @Test
    void fullMatchFlowRunsBothHalvesAndAppliesFatigue() {
        TeamSetup home = createTeam("H");
        TeamSetup away = createTeam("A");
        Match match = new Match(home, away, 999L);

        // เล่นครึ่งแรก
        match.playFirstHalf();
        assertEquals(1, match.getAllResults().size());

        // ตรวจสอบว่าหลังเล่นครึ่งแรก นักเตะถูกปรับสถานะความล้า (Tired)
        Player homeCb = match.getHomeTeam().getStarters().get(1);
        double effectiveVal = match.getHomeTeam().effectiveValue(homeCb, 4);
        assertTrue(effectiveVal < 4.0, "Player should be tired after the first half");

        // เล่นครึ่งหลัง
        match.playSecondHalf();
        assertEquals(2, match.getAllResults().size());

        // ผลรวมสกอร์ต้องตรงกับประตูรวม
        int totalHome = match.getAllResults().stream().mapToInt(PeriodResult::getHomeGoals).sum();
        assertEquals(totalHome, match.getTotalHomeGoals());
    }

    @Test
    void cannotPlayFirstHalfTwice() {
        Match match = new Match(createTeam("H"), createTeam("A"), 100L);
        match.playFirstHalf();
        assertThrows(IllegalStateException.class, match::playFirstHalf);
    }
}