package football.model;

public class WidePlay implements Tactic {
    @Override
    public String getName() { return "Wide Play"; }

    @Override
    public String getDescription() { return "เปิดบอลริมเส้น เน้นลูกโหม่ง"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof CounterAttack; }

    @Override
    public int fitScore(Player p) { return p.getHeading(); }
}