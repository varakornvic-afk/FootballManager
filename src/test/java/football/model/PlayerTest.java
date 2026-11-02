package football.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void validPlayerKeepsValues() {
        Player p = new Player("Alex", Position.STRIKER, 8, 3, 3, 3, 4, 3);
        assertEquals(8, p.getPace());
        assertEquals(3, p.getPassing());
        assertEquals(3, p.getHeading());
        assertEquals(3, p.getStamina());
        assertEquals(4, p.getShoot());
        assertEquals(3, p.getDefense());
        assertEquals(Position.STRIKER, p.getPosition());
    }

    @Test
    void sumNot24Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MIDFIELD, 8, 3, 3, 3, 4, 2)); // 23
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MIDFIELD, 8, 3, 3, 3, 4, 4)); // 25
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MIDFIELD, 0, 5, 5, 5, 5, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MIDFIELD, 11, 4, 3, 2, 2, 2));
        assertDoesNotThrow(() -> new Player("A", Position.MIDFIELD, 10, 4, 3, 3, 2, 2));
    }

    @Test
    void invalidNameOrPositionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(null, Position.FULLBACK, 4, 4, 4, 4, 4, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(" ", Position.FULLBACK, 4, 4, 4, 4, 4, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", null, 4, 4, 4, 4, 4, 4));
    }

    @Test
    void outfieldPlayerCannotUseGkPosition() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.GK, 4, 4, 4, 4, 4, 4));
    }

    @Test
    void goalKeeperKeepsValuesAndIsGk() {
        GoalKeeper gk = new GoalKeeper("Keeper", 3, 3, 3, 3, 3, 3, 6);
        assertEquals(Position.GK, gk.getPosition());
        assertEquals(6, gk.getSaving());
        assertEquals(3, gk.getStamina());
    }

    @Test
    void goalKeeperSumAndSavingRangeAreChecked() {
        assertThrows(IllegalArgumentException.class,
                () -> new GoalKeeper("G", 3, 3, 3, 3, 3, 3, 5));  // 23
        assertThrows(IllegalArgumentException.class,
                () -> new GoalKeeper("G", 4, 4, 4, 4, 4, 4, 0));  // saving 0
        assertThrows(IllegalArgumentException.class,
                () -> new GoalKeeper("G", 2, 2, 2, 2, 2, 2, 11)); // saving เกิน
        assertDoesNotThrow(() -> new GoalKeeper("G", 3, 3, 2, 2, 2, 2, 10));
    }

    @Test
    void fitScoreReadsMatchingStat() {
        Player p = new Player("A", Position.MIDFIELD, 9, 3, 2, 4, 3, 3);
        assertEquals(9, new CounterAttack().fitScore(p));
        assertEquals(3 + 3 / 2, new Possession().fitScore(p)); // MIDFIELD ได้ Passing เพิ่มครึ่งหนึ่ง
        assertEquals(2, new WidePlay().fitScore(p));
    }

    @Test
    void positionBonusesApplyOnlyToTheirOwnPositions() {
        Player striker = new Player("S", Position.STRIKER, 8, 3, 3, 3, 4, 3);
        Player centreBack = new Player("C", Position.CENTER_BACK, 8, 3, 3, 3, 4, 3);
        assertEquals(8 + 8 / 2, new CounterAttack().fitScore(striker));   // ST เร็วได้โบนัส
        assertEquals(8, new CounterAttack().fitScore(centreBack));        // ตำแหน่งอื่นไม่ได้

        Player winger = new Player("W", Position.MIDWING, 4, 6, 2, 4, 4, 4);
        Player midfielder = new Player("M", Position.MIDFIELD, 4, 6, 2, 4, 4, 4);
        assertEquals(2 + 6, new WidePlay().fitScore(winger));             // MW ได้ Passing บวกเพิ่ม
        assertEquals(2, new WidePlay().fitScore(midfielder));
        assertEquals(6 + 6 / 2, new Possession().fitScore(midfielder));   // MF ได้โบนัส Possession
        assertEquals(6, new Possession().fitScore(winger));
    }

    @Test
    void goalKeeperFitIgnoresSaving() {
        GoalKeeper gk = new GoalKeeper("G", 2, 3, 3, 4, 3, 3, 6);
        assertEquals(2, new CounterAttack().fitScore(gk));
        assertEquals(3, new Possession().fitScore(gk));
        assertEquals(3, new WidePlay().fitScore(gk));
    }
}