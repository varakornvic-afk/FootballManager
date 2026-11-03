package football.logic;

import football.event.MatchEvent;

import java.util.Collections;
import java.util.List;

public class MatchHalfResult {
    private final int homeGoals;
    private final List<MatchEvent> events;

    public MatchHalfResult(int homeGoals, List<MatchEvent> events) {
        this.homeGoals = homeGoals;
        this.events = events != null ? List.copyOf(events) : Collections.emptyList();
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }
}