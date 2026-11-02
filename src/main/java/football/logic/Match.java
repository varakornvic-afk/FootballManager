package football.logic;

import football.model.Player;
import football.model.TeamSetup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Match {
    private TeamSetup homeTeam;
    private TeamSetup awayTeam;
    private final MatchEngine engine;
    private final List<MatchHalfResult> periodResults = new ArrayList<>();

    private boolean firstHalfPlayed = false;
    private boolean secondHalfPlayed = false;

    public Match(TeamSetup homeTeam, TeamSetup awayTeam, long seed) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.engine = new MatchEngine(seed);
    }

    public MatchHalfResult playFirstHalf() {
        if (firstHalfPlayed) {
            throw new IllegalStateException("First half has already been played");
        }

        MatchHalfResult result = engine.simulatePeriod(1, 1, 45, homeTeam, awayTeam);
        periodResults.add(result);
        firstHalfPlayed = true;

        applyFatigue();
        return result;
    }

    public MatchHalfResult playSecondHalf() {
        if (!firstHalfPlayed) {
            throw new IllegalStateException("Cannot play second half before first half");
        }
        if (secondHalfPlayed) {
            throw new IllegalStateException("Second half has already been played");
        }

        MatchHalfResult result = engine.simulatePeriod(2, 46, 90, homeTeam, awayTeam);
        periodResults.add(result);
        secondHalfPlayed = true;

        applyFatigue();
        return result;
    }

    private void applyFatigue() {
        if (homeTeam != null) {
            for (Player p : homeTeam.getStarters()) {
                p.reduceStamina(1);
            }
        }
        if (awayTeam != null) {
            for (Player p : awayTeam.getStarters()) {
                p.reduceStamina(1);
            }
        }
    }

    public List<MatchHalfResult> getAllResults() {
        return Collections.unmodifiableList(periodResults);
    }

    public int getTotalHomeGoals() {
        if (periodResults == null || periodResults.isEmpty()) return 0;
        return periodResults.stream().mapToInt(MatchHalfResult::getHomeGoals).sum();
    }

    public int getTotalAwayGoals() {
        if (periodResults == null || periodResults.isEmpty()) return 0;
        return periodResults.stream().mapToInt(MatchHalfResult::getAwayGoals).sum();
    }

    public TeamSetup getHomeTeam() { return homeTeam; }
    public void setHomeTeam(TeamSetup homeTeam) { this.homeTeam = homeTeam; }

    public TeamSetup getAwayTeam() { return awayTeam; }
    public void setAwayTeam(TeamSetup awayTeam) { this.awayTeam = awayTeam; }
}