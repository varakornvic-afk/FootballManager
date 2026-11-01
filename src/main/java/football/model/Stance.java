package football.model;

import static football.model.Position.*;

public enum Stance {
    NORMAL("ปกติ", 0, 0, 1.0),
    PARK_THE_BUS("อุด", -3, 3, 0.7),
    ALL_OUT_ATTACK("พับสนามบุก", 3, -3, 1.5);

    private final String label;
    private final int attackDelta;
    private final int defenseDelta;
    private final double exposureFactor;

    Stance(String label, int attackDelta, int defenseDelta, double exposureFactor) {
        this.label = label;
        this.attackDelta = attackDelta;
        this.defenseDelta = defenseDelta;
        this.exposureFactor = exposureFactor;
    }

    public String getLabel() { return label; }
    public int getAttackDelta() { return attackDelta; }
    public int getDefenseDelta() { return defenseDelta; }
    public double getExposureFactor() { return exposureFactor; }

    /** แปลงช่องตำแหน่งของฟอร์เมชันตามโหมด */
    public SlotLayout apply(SlotLayout base) {
        int cb = base.count(CENTER_BACK);
        int fb = base.count(FULLBACK);
        int mf = base.count(MIDFIELD);
        int mw = base.count(MIDWING);
        int st = base.count(STRIKER);
        int fw = base.count(FRONTWING);
        switch (this) {
            case PARK_THE_BUS:
                // เหลือ Striker 1 ช่อง ที่เหลือถอยลงแดนกลาง
                requireAtLeastOne(st, "striker");
                return new SlotLayout(cb, fb, mf + (st - 1), mw + fw, 1, 0);
            case ALL_OUT_ATTACK:
                // เหลือ Center back 1 ช่อง ที่เหลือขยับขึ้นหนึ่งแถว
                requireAtLeastOne(cb, "center back");
                return new SlotLayout(1, 0, cb - 1, fb, st + mf, fw + mw);
            default:
                return base;
        }
    }

    private static void requireAtLeastOne(int count, String label) {
        if (count < 1) {
            throw new IllegalArgumentException("formation needs at least one " + label);
        }
    }
}