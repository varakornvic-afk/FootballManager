package football.model;

public class MatchEvent {
    public enum EventType {
        GOAL, YELLOW_CARD, RED_CARD, FOUL, PENALTY, SUBSTITUTION
    }

    private final int minute;
    private final EventType type;
    private final String teamName;
    private final Player player;
    private final String description;

    public MatchEvent(int minute, EventType type, String teamName, Player player, String description) {
        this.minute = minute;
        this.type = type;
        this.teamName = teamName;
        this.player = player;
        this.description = description;
    }

    public int getMinute() { return minute; }
    public EventType getType() { return type; }
    public String getTeamName() { return teamName; }
    public Player getPlayer() { return player; }
    public String getDescription() { return description; }
}