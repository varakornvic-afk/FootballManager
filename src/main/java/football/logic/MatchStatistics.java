package football.logic;

import football.event.MatchEvent;
import java.util.List;

/**
 * สถิติของแมตช์ ได้แก่ จำนวนยิง ยิงเข้ากรอบ ใบเหลือง ใบแดง และเปอร์เซ็นต์การครองบอล
 * คำนวณจากรายการเหตุการณ์ด้วยเมธอด calculate
 */
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

    /** คำนวณสถิติทั้งหมดจากรายการเหตุการณ์ของแมตช์ */
    public static MatchStatistics calculate(List<MatchEvent> events) {
        MatchStatistics stats = new MatchStatistics();
        if (events == null || events.isEmpty()) {
            return stats;
        }

        int homeAttacks = 0;
        int awayAttacks = 0;

        for (MatchEvent ev : events) {
            String team = ev.getTeamName() != null ? ev.getTeamName() : "";
            boolean isHome = team.equalsIgnoreCase("Home");
            String desc = ev.getDescription() != null ? ev.getDescription() : "";
            MatchEvent.EventType type = ev.getEventType();

            // 1. นับโอกาสบุกและจำนวนการยิงทั้งหมด
            if (type == MatchEvent.EventType.GOAL ||
                    type == MatchEvent.EventType.SAVE ||
                    type == MatchEvent.EventType.PENALTY ||
                    desc.contains("creates a clear scoring chance") ||
                    desc.contains("builds up an attack")) {

                if (isHome) {
                    homeAttacks++;
                    stats.homeShots++;
                } else {
                    awayAttacks++;
                    stats.awayShots++;
                }
            }

            // 2. นับการยิงเข้ากรอบ (Goals, Saves, Penalties)
            if (type == MatchEvent.EventType.GOAL ||
                    type == MatchEvent.EventType.SAVE ||
                    type == MatchEvent.EventType.PENALTY) {

                if (isHome) {
                    stats.homeShotsOnTarget++;
                } else {
                    stats.awayShotsOnTarget++;
                }
            }

            // 3. นับใบเหลือง และ ใบแดง
            if (type == MatchEvent.EventType.YELLOW_CARD) {
                if (isHome) stats.homeYellowCards++; else stats.awayYellowCards++;
            } else if (type == MatchEvent.EventType.RED_CARD) {
                if (isHome) stats.homeRedCards++; else stats.awayRedCards++;
            }
        }

        // 4. คำนวณ % การครองบอลอัตโนมัติ
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