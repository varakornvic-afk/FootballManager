package football.model;

public class WidePlay implements Tactic {
    @Override
    public String getName() { return "Wide Play"; }

    @Override
    public String getDescription() { return "เปิดบอลริมเส้น เน้นลูกโหม่ง"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof CounterAttack; }

    /** เน้นลูกโหม่ง และปีก (MIDWING) ที่จ่ายเก่งเหมาะกับเกมริมเส้นเป็นพิเศษ (ได้โบนัสค่า Passing) */
    @Override
    public int fitScore(Player p) {
        int score = p.getHeading();
        if (p.getPosition() == Position.MIDWING) score += p.getPassing();
        return score;
    }
}