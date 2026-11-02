package football.model;

public class Formation532 extends Formation {
    public Formation532() {
        super("5-3-2", 3, 5, 7, 1.5);
    }

    @Override
    public double fitFor(Tactic tactic) {
        if (tactic instanceof CounterAttack) return SPECIALIST_FIT;
        if (tactic instanceof WidePlay) return POOR_FIT; // ไม่มีช่องปีก (MW) เลย ขัดกับเกมริมเส้นมากกว่าแผนอื่น
        return OFF_PLAN_FIT;
    }

    @Override
    public SlotLayout getSlots() { return new SlotLayout(3, 2, 3, 0, 2, 0); }
}