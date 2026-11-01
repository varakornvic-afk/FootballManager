package football.logic;

import football.model.MatchEvent;
import java.util.List;

public class MatchStatistics {
    private int homeShots = 0;
    private int awayShots = 0;
    private int homeShotsOnTarget = 0;
    private int awayShotsOnTarget = 0;
    private int homeYellowCards = 0;
    private int awayYellowCards = 0;
    private int homeRedCards = 0;
    private int awayRedCards = 0;
    private int homePossessionPercent = 50;
    private int awayPossessionPercent = 50;

    public static MatchStatistics calculate(List<MatchEvent> events) {
        MatchStatistics stats = new MatchStatistics();
        int homeAttacks = 0;
        int awayAttacks = 0;

        for (MatchEvent ev : events) {
            String team = ev.getTeamName() != null ? ev.getTeamName() : "";
            boolean isHome = team.equalsIgnoreCase("Home");
            String desc = ev.getDescription() != null ? ev.getDescription() : "";

            // นับโอกาสบุก/ครองบอล
            if (desc.contains("creates a clear scoring chance") || desc.contains("builds up an attack")) {
                if (isHome) homeAttacks++; else awayAttacks++;
            }

            // นับจำนวนการสับไกยิงและยิงเข้ากรอบ
            if (desc.contains("creates a clear scoring chance")) {
                if (isHome) stats.homeShots++; else stats.awayShots++;
            }
            if (desc.contains("GOAL!") || desc.contains("saved brilliant") || desc.contains("Penalty goal")) {
                if (isHome) {
                    stats.homeShotsOnTarget++;
                } else {
                    stats.awayShotsOnTarget++;
                }
            }

            // นับใบเตือนและใบแดง
            if (ev.getType() == MatchEvent.EventType.YELLOW_CARD) {
                if (isHome) stats.homeYellowCards++; else stats.awayYellowCards++;
            } else if (ev.getType() == MatchEvent.EventType.RED_CARD) {
                if (isHome) stats.homeRedCards++; else stats.awayRedCards++;
            }
        }

        // คำนวณ % การครองบอลอัตโนมัติ
        int totalAttacks = homeAttacks + awayAttacks;
        if (totalAttacks > 0) {
            stats.homePossessionPercent = (int) Math.round(((double) homeAttacks / totalAttacks) * 100);
            stats.awayPossessionPercent = 100 - stats.homePossessionPercent;
        }

        return stats;
    }

    // Getters
    public int getHomeShots() { return homeShots; }
    public int getAwayShots() { return awayShots; }
    public int getHomeShotsOnTarget() { return homeShotsOnTarget; }
    public int getAwayShotsOnTarget() { return awayShotsOnTarget; }
    public int getHomeYellowCards() { return homeYellowCards; }
    public int getAwayYellowCards() { return awayYellowCards; }
    public int getHomeRedCards() { return homeRedCards; }
    public int getAwayRedCards() { return awayRedCards; }
    public int getHomePossessionPercent() { return homePossessionPercent; }
    public int getAwayPossessionPercent() { return awayPossessionPercent; }
}