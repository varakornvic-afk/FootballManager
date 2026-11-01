package football.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void samePositionIsZero() {
        assertEquals(0, Position.MIDFIELD.distanceTo(Position.MIDFIELD));
        assertEquals(0, Position.GK.distanceTo(Position.GK));
    }

    @Test
    void wingToCentreOnSameLineIsOne() {
        assertEquals(1, Position.FRONTWING.distanceTo(Position.STRIKER));
        assertEquals(1, Position.MIDWING.distanceTo(Position.MIDFIELD));
    }

    @Test
    void differentLineAndWidthAddUp() {
        assertEquals(2, Position.CENTER_BACK.distanceTo(Position.STRIKER));
        assertEquals(3, Position.FULLBACK.distanceTo(Position.STRIKER));
    }

    @Test
    void distanceIsSymmetric() {
        assertEquals(Position.FULLBACK.distanceTo(Position.FRONTWING),
                Position.FRONTWING.distanceTo(Position.FULLBACK));
    }

    @Test
    void goalkeeperCannotBeComparedWithOutfield() {
        assertThrows(IllegalArgumentException.class,
                () -> Position.GK.distanceTo(Position.STRIKER));
        assertThrows(IllegalArgumentException.class,
                () -> Position.STRIKER.distanceTo(Position.GK));
    }
}