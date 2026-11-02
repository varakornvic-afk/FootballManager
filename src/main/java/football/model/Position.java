package football.model;

public enum Position {
    GK(0, false),
    CENTER_BACK(1, false),
    FULLBACK(1, true),
    MIDFIELD(2, false),
    MIDWING(2, true),
    STRIKER(3, false),
    FRONTWING(3, true);

    private final int line;     // 0 = GK, 1 = back, 2 = middle, 3 = front
    private final boolean wide; // true = ริม, false = กลาง

    Position(int line, boolean wide) {
        this.line = line;
        this.wide = wide;
    }

    /** ระยะห่าง = ต่างกันกี่แถว + (ต่างริม/กลางไหม 1 หรือ 0) GK เทียบได้กับ GK เท่านั้น */
    public int distanceTo(Position other) {
        if (this == GK || other == GK) {
            if (this == other) {
                return 0;
            }
            throw new IllegalArgumentException("GK cannot be compared with outfield positions");
        }
        return Math.abs(line - other.line) + (wide == other.wide ? 0 : 1); // 📌 แก้ไข Syntax Error
    }

    public int getLine() { return line; }
    public boolean isWide() { return wide; }
}