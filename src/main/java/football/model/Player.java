package football.model;

public class Player {
    public static final int TOTAL = 24;
    public static final int MIN_STAT = 1;
    public static final int MAX_STAT = 10;

    private final String name;
    private final Position position;
    private final int pace;
    private final int passing;
    private final int heading;
    protected int stamina;
    private final int shoot;
    private final int defense;
    private final int saving;

    /** ผู้เล่นสนาม: 6 ค่ารวม 24 (ห้ามใช้ตำแหน่ง GK ให้ใช้ GoalKeeper) */
    public Player(String name, Position position, int pace, int passing,
                  int heading, int stamina, int shoot, int defense) {
        this(name, requireOutfield(position), pace, passing, heading,
                stamina, shoot, defense, 0);
    }

    /** extraPoints = แต้มที่คลาสลูกใช้กับสถิติพิเศษของตัวเอง (ผู้เล่นสนามเป็น 0) */
    protected Player(String name, Position position, int pace, int passing,
                     int heading, int stamina, int shoot, int defense, int extraPoints) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        checkRange("pace", pace);
        checkRange("passing", passing);
        checkRange("heading", heading);
        checkRange("stamina", stamina);
        checkRange("shoot", shoot);
        checkRange("defense", defense);
        int sum = pace + passing + heading + stamina + shoot + defense + extraPoints;
        if (sum != TOTAL) {
            throw new IllegalArgumentException("stats must sum to " + TOTAL + " but was " + sum);
        }
        this.name = name;
        this.position = position;
        this.pace = pace;
        this.passing = passing;
        this.heading = heading;
        this.stamina = stamina;
        this.shoot = shoot;
        this.defense = defense;
        this.saving = extraPoints;
    }

    private static Position requireOutfield(Position position) {
        if (position == Position.GK) {
            throw new IllegalArgumentException("use GoalKeeper for position GK");
        }
        return position;
    }

    protected static void checkRange(String label, int value) {
        if (value < MIN_STAT || value > MAX_STAT) {
            throw new IllegalArgumentException(
                    label + " must be " + MIN_STAT + "-" + MAX_STAT + " but was " + value);
        }
    }

    public String getName() { return name; }
    public Position getPosition() { return position; }
    public int getPace() { return pace; }
    public int getPassing() { return passing; }
    public int getHeading() { return heading; }
    public int getStamina() { return stamina; }
    public int getShoot() { return shoot; }
    public int getDefense() { return defense; }
    public int getSaving() { return saving; }

    @Override
    public String toString() {
        return String.format("%s [%s] (Pac:%d, Pas:%d, Head:%d, Stam:%d, Sho:%d, Def:%d, Sav:%d)",
                name, position, pace, passing, heading, stamina, shoot, defense, saving);
    }

    /** ⚡ ลด Stamina ตามจำนวนที่ระบุ โดยไม่ให้ต่ำกว่า 0 */
    public void reduceStamina(int amount) {
        this.stamina = Math.max(0, this.stamina - amount);
    }
}