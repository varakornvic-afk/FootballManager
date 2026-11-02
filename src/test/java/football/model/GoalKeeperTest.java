package football.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GoalKeeperTest {
    private GoalKeeper gk;

    @BeforeEach
    public void setUp() {
        // ผลรวมค่าพลัง: pace=3, passing=3, heading=3, stamina=4, shoot=3, defense=4, saving=4 (รวม 24)
        gk = new GoalKeeper("Check", 3, 3, 3, 4, 3, 4, 4);
    }

    @Test
    @DisplayName("Stamina ของ GK ต้องยังไม่ลดหากเซฟยังไม่ครบ 3 ครั้ง")
    public void testSaveLessThanThreeTimesDoesNotReduceStamina() {
        int initialStamina = gk.getStamina();

        gk.onSaveInGame();
        assertEquals(1, gk.getSaveCount(), "saveCount ควรเพิ่มเป็น 1");
        assertEquals(initialStamina, gk.getStamina(), "Stamina ยังไม่ควรลดเมื่อเซฟครั้งที่ 1");

        gk.onSaveInGame();
        assertEquals(2, gk.getSaveCount(), "saveCount ควรเพิ่มเป็น 2");
        assertEquals(initialStamina, gk.getStamina(), "Stamina ยังไม่ควรลดเมื่อเซฟครั้งที่ 2");
    }

    @Test
    @DisplayName("Stamina ของ GK ต้องลดลง 1 หน่วย เมื่อเซฟครบ 3 ครั้ง และรีเซ็ต saveCount")
    public void testSaveThreeTimesReducesStaminaAndResetsCount() {
        int initialStamina = gk.getStamina();

        gk.onSaveInGame(); // ครั้งที่ 1
        gk.onSaveInGame(); // ครั้งที่ 2
        gk.onSaveInGame(); // ครั้งที่ 3 -> หัก Stamina และรีเซ็ตตัวนับ

        assertEquals(initialStamina - 1, gk.getStamina(), "Stamina ควรลดลง 1 หน่วยหลังเซฟครบ 3 ครั้ง");
        assertEquals(0, gk.getSaveCount(), "saveCount ควรถูกรีเซ็ตกลับเป็น 0");
    }

    @Test
    @DisplayName("Stamina ของ GK ต้องไม่ลดลงจนติดลบ")
    public void testStaminaDoesNotGoBelowZero() {
        // เซฟ 30 ครั้ง (เท่ากับตัด Stamina 10 ครั้ง) เพื่อทดสอบขอบเขตล่างสุด
        for (int i = 0; i < 30; i++) {
            gk.onSaveInGame();
        }

        assertTrue(gk.getStamina() >= 0, "Stamina ต้องไม่น้อยกว่า 0");
    }
}