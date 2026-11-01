package football.logic;

import football.model.*;
import java.util.ArrayList;
import java.util.List;

public class Match {
    private TeamSetup homeTeam;
    private TeamSetup awayTeam;
    private final MatchEngine engine;
    private final List<PeriodResult> allResults = new ArrayList<>();
    private boolean firstHalfPlayed = false;
    private boolean secondHalfPlayed = false;

    public Match(TeamSetup homeTeam, TeamSetup awayTeam, long seed) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.engine = new MatchEngine(seed);
    }

    /** เล่นครึ่งแรก (นาที 1 - 45) แล้วอัปเดตความล้าพักครึ่ง */
    public PeriodResult playFirstHalf() {
        if (firstHalfPlayed) {
            throw new IllegalStateException("First half already played.");
        }
        PeriodResult result = engine.simulatePeriod(1, 1, 45, homeTeam, awayTeam);
        allResults.add(result);

        // เมื่อจบครึ่งแรก อัปเดตความล้าของนักเตะทั้งสองทีม
        homeTeam = homeTeam.endFirstHalf();
        awayTeam = awayTeam.endFirstHalf();
        firstHalfPlayed = true;
        return result;
    }

    /** เล่นครึ่งหลัง (นาที 46 - 90) */
    public PeriodResult playSecondHalf() {
        if (!firstHalfPlayed) {
            playFirstHalf();
        }
        if (secondHalfPlayed) {
            throw new IllegalStateException("Second half already played.");
        }
        PeriodResult result = engine.simulatePeriod(2, 46, 90, homeTeam, awayTeam);
        allResults.add(result);
        secondHalfPlayed = true;
        return result;
    }

    public TeamSetup getHomeTeam() { return homeTeam; }
    public TeamSetup getAwayTeam() { return awayTeam; }
    public List<PeriodResult> getAllResults() { return List.copyOf(allResults); }

    public int getTotalHomeGoals() {
        return allResults.stream().mapToInt(PeriodResult::getHomeGoals).sum();
    }

    public int getTotalAwayGoals() {
        return allResults.stream().mapToInt(PeriodResult::getAwayGoals).sum();
    }
}