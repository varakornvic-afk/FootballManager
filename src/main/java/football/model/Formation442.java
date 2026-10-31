package football.model;

public class Formation442 extends Formation {
    public Formation442() {
        super("4-4-2", 5, 5, 5, 0.5);
    }

    @Override
    public double fitFor(Tactic tactic) {
        return VERSATILE_FIT;
    }
}