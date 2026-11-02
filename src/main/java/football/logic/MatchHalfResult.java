package football.logic;

import football.event.MatchEvent;

import java.util.Collections;
import java.util.List;

public class MatchHalfResult {
    private final int periodNumber;
    private final int homeGoals;
    private final int awayGoals;
    private final List<MatchEvent> events;

    public MatchHalfResult(int periodNumber, int homeGoals, int awayGoals, List<MatchEvent> events) {
        this.periodNumber = periodNumber;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.events = events != null ? List.copyOf(events) : Collections.emptyList();
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }
}