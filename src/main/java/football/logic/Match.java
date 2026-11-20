package football.logic;

import football.model.Player;
import football.model.TeamSetup;

/**
 * แมตช์หนึ่งนัด เล่นได้ทีละครึ่ง (ครึ่งแรกแล้วครึ่งหลัง) โดยใช้ MatchEngine จำลองเหตุการณ์
 * และลดความเหนื่อยของผู้เล่นระหว่างทาง
 */
public class Match {
    private TeamSetup homeTeam;
    private TeamSetup awayTeam;
    private final MatchEngine engine;

    private boolean firstHalfPlayed = false;
    private boolean secondHalfPlayed = false;

    public Match(TeamSetup homeTeam, TeamSetup awayTeam, long seed) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.engine = new MatchEngine(seed);
    }

    /** จำลองครึ่งแรก (นาที 1-45) และคืนผลของครึ่งนั้น */
    public MatchHalfResult playFirstHalf() {
        if (firstHalfPlayed) {
            throw new IllegalStateException("First half has already been played");
        }

        MatchHalfResult result = engine.simulatePeriod(1, 45, homeTeam, awayTeam);
        firstHalfPlayed = true;

        applyFatigue();
        return result;
    }

    /** จำลองครึ่งหลัง (นาที 46-90) และคืนผลของครึ่งนั้น */
    public MatchHalfResult playSecondHalf() {
        if (!firstHalfPlayed) {
            throw new IllegalStateException("Cannot play second half before first half");
        }
        if (secondHalfPlayed) {
            throw new IllegalStateException("Second half has already been played");
        }

        MatchHalfResult result = engine.simulatePeriod(46, 90, homeTeam, awayTeam);
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

    public void setHomeTeam(TeamSetup homeTeam) { this.homeTeam = homeTeam; }

    public void setAwayTeam(TeamSetup awayTeam) { this.awayTeam = awayTeam; }
}