package football.model;

public class Formation433 extends Formation {
    public Formation433() {
        super("4-3-3", 5, 6, 4, 1.5);
    }

    @Override
    public double fitFor(Tactic tactic) {
        return tactic instanceof Possession ? SPECIALIST_FIT : OFF_PLAN_FIT;
    }
}