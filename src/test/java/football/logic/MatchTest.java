package football.logic;

import football.model.*;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("จำลองการแข่งขันครบสองครึ่ง ตรวจสอบสถานะผลลัพธ์ และ Domain Logic เรื่อง Fatigue")
    void fullMatchFlowRunsBothHalvesAndAppliesFatigue() {
        TeamSetup home = createTeam("H");
        TeamSetup away = createTeam("A");
        Match match = new Match(home, away, 999L);

        // 1. เล่นครึ่งแรก
        MatchHalfResult firstHalf = match.playFirstHalf();
        assertNotNull(firstHalf, "ควรมีผลลัพธ์ครึ่งแรก");

        // 2. ตรวจสอบ Fatigue Logic ของ Domain Layer
        Player sampleStarter = home.getStarters().get(1);
        sampleStarter.setStamina(1); // จำลองว่านักเตะล้าจน Stamina เหลือ 1

        double effectiveValWithFatigue = home.effectiveValue(sampleStarter, 4)
                * TeamSetup.fatigueFactor(sampleStarter.getStamina());
        assertTrue(effectiveValWithFatigue < 4.0, "ผู้เล่นที่มีค่า Stamina ต่ำจากความล้า ต้องมีค่าประสิทธิภาพรวมลดลง");

        // 3. เล่นครึ่งหลัง
        MatchHalfResult secondHalf = match.playSecondHalf();
        assertNotNull(secondHalf, "ควรมีผลลัพธ์ครึ่งหลัง");
    }

    @Test
    @DisplayName("ไม่อนุญาตให้สั่งเล่นครึ่งแรกซ้ำเป็นครั้งที่สอง")
    void cannotPlayFirstHalfTwice() {
        Match match = new Match(createTeam("H"), createTeam("A"), 100L);
        match.playFirstHalf();
        assertThrows(IllegalStateException.class, match::playFirstHalf, "ต้องโยน Exception เมื่อเล่นครึ่งแรกซ้ำ");
    }
}