package football.model;

import static football.model.Position.*;

public enum Stance {
    NORMAL("ปกติ"),
    PARK_THE_BUS("อุด"),
    ALL_OUT_ATTACK("พับสนามบุก");

    private final String label;

    Stance(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

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