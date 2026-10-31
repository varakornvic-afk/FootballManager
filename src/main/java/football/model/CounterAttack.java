package football.model;

public class CounterAttack implements Tactic {
    @Override
    public String getName() { return "Counter Attack"; }

    @Override
    public String getDescription() { return "ตั้งรับลึก รอสวนกลับ"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof Possession; }

    @Override
    public int fitScore(Player p) { return p.getPace(); }
}