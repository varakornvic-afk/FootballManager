package football.logic;

import football.model.Player;
import football.model.Position;
import football.model.TeamSetup;

/**
 * ผลของความล้า (Stamina) ต่อการเล่น: ยิ่ง Stamina ต่ำเมื่อเทียบกับค่าเต็มของตัวเอง ยิ่งเล่นแย่ลง
 * ตัวคูณอยู่ระหว่าง MIN_FACTOR (หมดแรง) ถึง 1.0 (เต็ม) ผู้รักษาประตูไม่เหนื่อย (ไม่ลด Stamina) จึงได้ 1.0 เสมอ
 */
public final class Fatigue {
    public static final double MIN_FACTOR = 0.7;

    private Fatigue() { }

    /** ตัวคูณความล้าของผู้เล่นหนึ่งคน (1.0 = สดเต็มที่) */
    public static double factor(Player player) {
        if (player.getPosition() == Position.GK || player.getMaxStamina() <= 0) return 1.0;
        double ratio = (double) player.getStamina() / player.getMaxStamina();
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        return MIN_FACTOR + (1.0 - MIN_FACTOR) * ratio;
    }

    /** ตัวคูณเฉลี่ยของผู้เล่นสนามที่อยู่ในสนาม; ไม่มีผู้เล่นสนามเลยคืน 1.0 */
    public static double averageFactor(TeamSetup team) {
        double total = 0;
        int count = 0;
        for (Player p : team.getStarters()) {
            if (p.getPosition() == Position.GK) continue;
            total += factor(p);
            count++;
        }
        return count == 0 ? 1.0 : total / count;
    }
}