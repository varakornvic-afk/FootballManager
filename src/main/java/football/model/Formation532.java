package football.model;

public class Formation532 extends Formation {
    public Formation532() {
        super("5-3-2", 3, 5, 7, 1.5);
    }

    @Override
    public double fitFor(Tactic tactic) {
        return tactic instanceof CounterAttack ? SPECIALIST_FIT : OFF_PLAN_FIT;
    }
}