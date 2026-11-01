package football.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GoalKeeperTest {
    private GoalKeeper gk;

    @BeforeEach
    public void setUp() {
        // ผลรวมค่าพลัง = 3+3+3+5+2+3+5 = 24 พอดี
        gk = new GoalKeeper("Check", 3, 3, 3, 5, 2, 3, 5);
    }

    @Test
    @DisplayName("Stamina ของ GK ต้องลดลงครั้งละ 1 เมื่อเซฟในเกมปกติ")
    public void testOnSaveInGameReducesStamina() {
        int initialStamina = gk.getStamina();
        gk.onSaveInGame();

        assertEquals(initialStamina - 1, gk.getStamina(), "Stamina ควรลดลง 1 หน่วยหลังการเซฟ");
    }

    @Test
    @DisplayName("Stamina ของ GK ต้องไม่ลดลงจนติดลบ")
    public void testStaminaDoesNotGoBelowZero() {
        for (int i = 0; i < 10; i++) {
            gk.onSaveInGame();
        }

        assertTrue(gk.getStamina() >= 0, "Stamina ต้องไม่น้อยกว่า 0");
    }
}