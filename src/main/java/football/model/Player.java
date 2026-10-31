package football.model;

public final class Player {
    public static final int TOTAL = 15;
    public static final int MIN_STAT = 1;
    public static final int MAX_STAT = 13;

    private final String name;
    private final Position position;
    private final int pace;
    private final int passing;
    private final int heading;

    public Player(String name, Position position, int pace, int passing, int heading) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        checkRange("pace", pace);
        checkRange("passing", passing);
        checkRange("heading", heading);
        if (pace + passing + heading != TOTAL) {
            throw new IllegalArgumentException("stats must sum to " + TOTAL);
        }
        this.name = name;
        this.position = position;
        this.pace = pace;
        this.passing = passing;
        this.heading = heading;
    }

    private static void checkRange(String label, int v) {
        if (v < MIN_STAT || v > MAX_STAT) {
            throw new IllegalArgumentException(
                    label + " must be " + MIN_STAT + "-" + MAX_STAT + " but was " + v);
        }
    }

    public String getName() { return name; }
    public Position getPosition() { return position; }
    public int getPace() { return pace; }
    public int getPassing() { return passing; }
    public int getHeading() { return heading; }
}