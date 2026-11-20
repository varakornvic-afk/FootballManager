package football.model;

/**
 * แผนสวนกลับ ตั้งรับลึกแล้วเปิดเกมเร็ว ชนะแผน Possession
 * ความเหมาะของผู้เล่นวัดจากความเร็ว (Pace) และกองหน้าที่เร็วได้โบนัสเพิ่ม
 */
public class CounterAttack implements Tactic {
    @Override
    public String getName() { return "Counter Attack"; }

    @Override
    public String getDescription() { return "ตั้งรับลึก รอสวนกลับ"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof Possession; }

    /** เน้นความเร็ว (Pace) และกองหน้า (STRIKER, FRONTWING) ที่เร็วเป็นตัวสวนกลับ (ได้โบนัสครึ่งหนึ่งของ Pace) */
    @Override
    public int fitScore(Player p) {
        int score = p.getPace();
        if (p.getPosition() == Position.STRIKER || p.getPosition() == Position.FRONTWING) {
            score += p.getPace() / 2;
        }
        return score;
    }
}