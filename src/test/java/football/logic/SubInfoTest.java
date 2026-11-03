package football.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubInfoTest {

    @Test
    void keepsWhoWentOffWhoCameOnAndWhichSide() {
        SubInfo home = new SubInfo("Out", "In", true);
        SubInfo away = new SubInfo("A", "B", false);

        assertEquals("Out", home.playerOut);
        assertEquals("In", home.playerIn);
        assertTrue(home.isHome);
        assertFalse(away.isHome);
    }
}
