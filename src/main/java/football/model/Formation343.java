package football.model;

public class Formation343 extends Formation {
    public Formation343() {
        super("3-4-3", 6, 5, 4, 1.5);
    }

    @Override
    public double fitFor(Tactic tactic) {
        return tactic instanceof WidePlay ? SPECIALIST_FIT : OFF_PLAN_FIT;
    }
}