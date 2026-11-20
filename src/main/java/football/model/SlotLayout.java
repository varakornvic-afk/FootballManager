package football.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * จำนวนช่องของผู้เล่นสนามแต่ละตำแหน่งในฟอร์เมชัน รวมต้องได้ 10 ช่อง (ผู้รักษาประตูแยกอีก 1 คน)
 * เป็นออบเจกต์ที่เปลี่ยนค่าไม่ได้ และเทียบกันด้วยค่า
 */
public final class SlotLayout {
    public static final int OUTFIELD_SLOTS = 10;

    private final Map<Position, Integer> counts = new EnumMap<>(Position.class);

    public SlotLayout(int centerBack, int fullback, int midfield,
                      int midwing, int striker, int frontwing) {
        int[] values = {centerBack, fullback, midfield, midwing, striker, frontwing};
        int sum = 0;
        for (int v : values) {
            if (v < 0) {
                throw new IllegalArgumentException("slot count must not be negative");
            }
            sum += v;
        }
        if (sum != OUTFIELD_SLOTS) {
            throw new IllegalArgumentException(
                    "outfield slots must total " + OUTFIELD_SLOTS + " but was " + sum);
        }
        counts.put(Position.CENTER_BACK, centerBack);
        counts.put(Position.FULLBACK, fullback);
        counts.put(Position.MIDFIELD, midfield);
        counts.put(Position.MIDWING, midwing);
        counts.put(Position.STRIKER, striker);
        counts.put(Position.FRONTWING, frontwing);
    }

    public int count(Position position) {
        return position == Position.GK ? 1 : counts.get(position);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SlotLayout) {
            SlotLayout other = (SlotLayout) o;
            return counts.equals(other.counts);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return counts.hashCode();
    }
}