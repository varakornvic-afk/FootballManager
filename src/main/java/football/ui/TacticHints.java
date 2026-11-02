package football.ui;

import football.model.*;

import java.util.List;

/**
 * ข้อความบอกใบ้บนหน้าเลือกแผน/ฟอร์เมชัน (tooltip) บอกแค่ภาพรวม ไม่บอกตัวเลขหรือโบนัสตามตำแหน่ง
 * ข้อมูลทั้งหมดอ่านจากคลาสโมเดลโดยตรง จึงไม่ซ้ำซ้อนกับตรรกะเกม
 */
final class TacticHints {
    private static final double SPECIALIST_THRESHOLD = 1.10;
    private static final double LOW_EXPOSURE = 1.0;

    private static final List<Tactic> TACTICS = List.of(new CounterAttack(), new Possession(), new WidePlay());
    private static final List<Formation> FORMATIONS =
            List.of(new Formation442(), new Formation433(), new Formation343(), new Formation532());

    private TacticHints() {}

    /** เช่น "ตั้งรับลึก รอสวนกลับ | ชนะทาง: Possession | แพ้ทาง: Wide Play" */
    static String forTactic(String name) {
        Tactic tactic = findTactic(name);
        if (tactic == null) return null;
        StringBuilder wins = new StringBuilder();
        StringBuilder loses = new StringBuilder();
        for (Tactic other : TACTICS) {
            if (tactic.beats(other)) append(wins, other.getName());
            if (other.beats(tactic)) append(loses, other.getName());
        }
        return tactic.getDescription() + " | ชนะทาง: " + wins + " | แพ้ทาง: " + loses;
    }

    /** เช่น "ถนัดแผน Possession | เสี่ยงสูงเมื่อเจอแผนที่ชนะทาง" หรือ "ยืดหยุ่น ใช้ได้ทุกแผน | เสี่ยงต่ำ..." */
    static String forFormation(String name) {
        Formation formation = findFormation(name);
        if (formation == null) return null;
        String suits = null;
        for (Tactic tactic : TACTICS) {
            if (formation.fitFor(tactic) >= SPECIALIST_THRESHOLD) suits = tactic.getName();
        }
        String style = suits != null ? "ถนัดแผน " + suits : "ยืดหยุ่น ใช้ได้ทุกแผน";
        String risk = formation.getExposure() < LOW_EXPOSURE
                ? "เสี่ยงต่ำเมื่อเจอแผนที่ชนะทาง"
                : "เสี่ยงสูงเมื่อเจอแผนที่ชนะทาง";
        return style + " | " + risk;
    }

    private static Tactic findTactic(String name) {
        for (Tactic t : TACTICS) if (t.getName().equals(name)) return t;
        return null;
    }

    private static Formation findFormation(String name) {
        for (Formation f : FORMATIONS) if (f.getName().equals(name)) return f;
        return null;
    }

    private static void append(StringBuilder sb, String text) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(text);
    }
}