package football.model;

import org.junit.jupiter.api.Test;
import static football.model.Position.*;
import static org.junit.jupiter.api.Assertions.*;

class StanceTest {

    private static final SlotLayout[] BASES = {
            new SlotLayout(3, 2, 3, 0, 2, 0), // 5-3-2
            new SlotLayout(2, 2, 3, 0, 1, 2), // 4-3-3
            new SlotLayout(3, 0, 2, 2, 1, 2), // 3-4-3
            new SlotLayout(2, 2, 2, 2, 2, 0)  // 4-4-2
    };

    @Test
    void slotLayoutMustTotalTenAndBeNonNegative() {
        assertThrows(IllegalArgumentException.class, () -> new SlotLayout(3, 2, 3, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SlotLayout(-1, 3, 3, 0, 3, 2));
    }

    @Test
    void goalkeeperAlwaysHasOneSlot() {
        assertEquals(1, BASES[0].count(GK));
    }

    @Test
    void normalReturnsSameLayout() {
        for (SlotLayout base : BASES) {
            assertEquals(base, Stance.NORMAL.apply(base));
        }
    }

    @Test
    void parkTheBusKeepsOneStrikerAndNoFrontwing() {
        for (SlotLayout base : BASES) {
            SlotLayout result = Stance.PARK_THE_BUS.apply(base);
            assertEquals(1, result.count(STRIKER));
            assertEquals(0, result.count(FRONTWING));
        }
        assertEquals(new SlotLayout(3, 2, 4, 0, 1, 0),
                Stance.PARK_THE_BUS.apply(BASES[0]));
        assertEquals(new SlotLayout(3, 0, 2, 4, 1, 0),
                Stance.PARK_THE_BUS.apply(BASES[2]));
    }

    @Test
    void allOutAttackKeepsOneCenterBackAndNoFullback() {
        for (SlotLayout base : BASES) {
            SlotLayout result = Stance.ALL_OUT_ATTACK.apply(base);
            assertEquals(1, result.count(CENTER_BACK));
            assertEquals(0, result.count(FULLBACK));
        }
        assertEquals(new SlotLayout(1, 0, 2, 2, 5, 0),
                Stance.ALL_OUT_ATTACK.apply(BASES[0]));
        assertEquals(new SlotLayout(1, 0, 1, 2, 4, 2),
                Stance.ALL_OUT_ATTACK.apply(BASES[1]));
        assertEquals(new SlotLayout(1, 0, 2, 0, 3, 4),
                Stance.ALL_OUT_ATTACK.apply(BASES[2]));
    }

    @Test
    void everyStanceHasADisplayLabel() {
        for (Stance s : Stance.values()) {
            assertFalse(s.getLabel().isBlank());
        }
    }
}