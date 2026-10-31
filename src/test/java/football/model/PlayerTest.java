package football.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void validPlayerKeepsValues() {
        Player p = new Player("Alex", Position.FWD, 8, 4, 3);
        assertEquals(8, p.getPace());
        assertEquals(4, p.getPassing());
        assertEquals(3, p.getHeading());
        assertEquals(Position.FWD, p.getPosition());
    }

    @Test
    void sumNot15Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MID, 5, 5, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MID, 5, 5, 6));
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MID, 0, 8, 7));
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", Position.MID, 14, 1, 0));
    }

    @Test
    void invalidNameOrPositionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(null, Position.GK, 5, 5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(" ", Position.GK, 5, 5, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new Player("A", null, 5, 5, 5));
    }

    @Test
    void fitScoreReadsMatchingStat() {
        Player p = new Player("A", Position.MID, 9, 4, 2);
        assertEquals(9, new CounterAttack().fitScore(p));
        assertEquals(4, new Possession().fitScore(p));
        assertEquals(2, new WidePlay().fitScore(p));
    }
}