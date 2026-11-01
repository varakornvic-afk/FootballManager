package football.logic;

import football.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MatchEngine {
    private final Random random;

    public MatchEngine(long seed) {
        this.random = new Random(seed);
    }

    public MatchEngine(Random random) {
        this.random = random;
    }

    /** จำลองการแข่งขันในแต่ละช่วงเวลา */
    public PeriodResult simulatePeriod(int periodNumber, int startMinute, int endMinute,
                                       TeamSetup home, TeamSetup away) {
        List<MatchEvent> events = new ArrayList<>();
        int homeGoals = 0;
        int awayGoals = 0;

        double homeMod = MatchRules.totalModifier(home.getTactic(), away.getTactic(), home.getFormation()) * home.fitMultiplier();
        double awayMod = MatchRules.totalModifier(away.getTactic(), home.getTactic(), away.getFormation()) * away.fitMultiplier();

        for (int min = startMinute; min <= endMinute; min += 15) {
            // โอกาสบุกของทีมเหย้า
            if (random.nextDouble() < 0.15 * homeMod) {
                if (tryAttack(home, away, min, "Home", events)) {
                    homeGoals++;
                }
            }
            // โอกาสบุกของทีมเยือน
            else if (random.nextDouble() < 0.15 * awayMod) {
                if (tryAttack(away, home, min, "Away", events)) {
                    awayGoals++;
                }
            }

            // โอกาสเกิดฟาวล์ / ใบเหลือง / จุดโทษ
            if (random.nextDouble() < 0.08) {
                boolean isHomeFoul = random.nextBoolean();
                String teamName = isHomeFoul ? "Home" : "Away";

                // เช็คว่าเป็นจุดโทษหรือไม่ (โอกาส 25% ในเขตโทษ)
                if (random.nextDouble() < 0.25) {
                    events.add(new MatchEvent(min, MatchEvent.EventType.FOUL, teamName, null, "Foul inside the penalty box! Penalty!"));
                    // ดวลจุดโทษ (โอกาสเข้า 75%)
                    if (random.nextDouble() < 0.75) {
                        if (isHomeFoul) { awayGoals++; } else { homeGoals++; }
                        events.add(new MatchEvent(min, MatchEvent.EventType.PENALTY, isHomeFoul ? "Away" : "Home", null, "Penalty goal scored!"));
                    } else {
                        events.add(new MatchEvent(min, MatchEvent.EventType.PENALTY, teamName, null, "Penalty missed or saved!"));
                    }
                } else {
                    events.add(new MatchEvent(min, MatchEvent.EventType.FOUL, teamName, null, "Tactical foul committed."));
                    events.add(new MatchEvent(min, MatchEvent.EventType.YELLOW_CARD, teamName, null, "Yellow card issued."));
                }
            }
        }

        return new PeriodResult(periodNumber, homeGoals, awayGoals, events);
    }

    /** จำลองจังหวะยิงประตูและเช็คการเซฟของผู้รักษาประตู */
    private boolean tryAttack(TeamSetup attacking, TeamSetup defending, int minute, String teamName, List<MatchEvent> events) {
        events.add(new MatchEvent(minute, MatchEvent.EventType.GOAL, teamName, null, teamName + " creates a clear scoring chance!"));

        // สุ่มพลังการยิง vs พลังเซฟ (สามารถดึงค่าเฉลี่ยจากผู้เล่นในสนามมาคำนวณจริงได้)
        double attackPower = 5.0 * attacking.fitMultiplier();
        double defensePower = 5.0 * defending.fitMultiplier();

        if (random.nextDouble() * attackPower > random.nextDouble() * defensePower) {
            events.add(new MatchEvent(minute, MatchEvent.EventType.GOAL, teamName, null, "GOAL! " + teamName + " scores!"));
            return true;
        } else {
            events.add(new MatchEvent(minute, MatchEvent.EventType.GOAL, teamName, null, "Great save by the Goalkeeper!"));
            return false;
        }
    }
}