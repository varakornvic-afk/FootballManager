package football.model;

public class Possession implements Tactic {
    @Override
    public String getName() { return "Possession"; }

    @Override
    public String getDescription() { return "ต่อบอล ขึ้นหมด ลงหมด"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof WidePlay; }

    @Override
    public int fitScore(Player p) { return p.getPassing(); }
}