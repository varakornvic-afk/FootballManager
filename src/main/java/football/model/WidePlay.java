package football.model;

public class HighPress implements Tactic {
    @Override
    public String getName() { return "High Press"; }

    @Override
    public boolean beats(Tactic other) { return other instanceof Possession; }
}