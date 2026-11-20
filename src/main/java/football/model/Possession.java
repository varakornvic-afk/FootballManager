package football.model;

/**
 * แผนครองบอล ต่อบอลขึ้นหน้าทั้งทีม ชนะแผน Wide Play
 */
public class Possession implements Tactic {
    @Override
    public String getName() { return "Possession"; }

    @Override
    public String getDescription() { return "ต่อบอล ขึ้นหมด ลงหมด"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof WidePlay; }

    /** เน้นการจ่าย (Passing) และกองกลาง (MIDFIELD) ที่จ่ายเก่งเป็นหัวใจของเกมครองบอล (ได้โบนัสครึ่งหนึ่งของ Passing) */
    @Override
    public int fitScore(Player p) {
        int score = p.getPassing();
        if (p.getPosition() == Position.MIDFIELD) score += p.getPassing() / 2;
        return score;
    }
}