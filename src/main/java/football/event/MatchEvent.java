package football.event;

import football.model.Player;

public class MatchEvent {
    public enum EventType {
        GOAL,
        SAVE,
        FOUL,
        YELLOW_CARD,
        RED_CARD,
        PENALTY
    }

    private final int minute;
    private final String teamName;
    private final Player player;
    private final EventType eventType;
    private final String description;

    public MatchEvent(int minute, String teamName, Player player, EventType eventType, String description) {
        this.minute = minute;
        this.teamName = teamName;
        this.player = player;
        this.eventType = eventType;
        this.description = description;
    }

    // 📌 เมธอด Public Getter ที่ MainFrame และ IDE กำลังเรียกหา
    public int getMinute() { return minute; }
    public String getTeamName() { return teamName; }
    public Player getPlayer() { return player; }
    public EventType getEventType() { return eventType; }
    public String getDescription() { return description; }
}