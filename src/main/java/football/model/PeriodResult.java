package football.model;

import java.util.List;

public class PeriodResult {
    private final int periodNumber;
    private final int homeGoals;
    private final int awayGoals;
    private final List<MatchEvent> events;

    public PeriodResult(int periodNumber, int homeGoals, int awayGoals, List<MatchEvent> events) {
        this.periodNumber = periodNumber;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.events = List.copyOf(events);
    }

    public int getPeriodNumber() { return periodNumber; }
    public int getHomeGoals() { return homeGoals; }
    public int getAwayGoals() { return awayGoals; }
    public List<MatchEvent> getEvents() { return events; }
}