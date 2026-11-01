package football.logic;

import football.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MatchEngine {
    private final Random random;

    public MatchEngine(long seed) {
        this.random = new Random(seed);
    }

    public MatchEngine(Random random) {
        this.random = random;
    }

    private int getWeightedStoppageTime() {
        int r = random.nextInt(100);
        if (r < 35) return 0;       // 35% -> +0 นาที
        if (r < 60) return 1;       // 25% -> +1 นาที
        if (r < 80) return 2;       // 20% -> +2 นาที
        if (r < 92) return 3;       // 12% -> +3 นาที
        if (r < 97) return 4;       // 5%  -> +4 นาที
        return 5;                   // 3%  -> +5 นาที
    }

    public PeriodResult simulatePeriod(int periodNumber, int startMinute, int baseEndMinute,
                                       TeamSetup home, TeamSetup away) {
        List<MatchEvent> events = new ArrayList<>();
        int homeGoals = 0;
        int awayGoals = 0;

        Map<Player, Integer> yellowCardTracker = new HashMap<>();

        List<Player> homeStarters = new ArrayList<>(home.getStarters());
        List<Player> awayStarters = new ArrayList<>(away.getStarters());

        int stoppage = getWeightedStoppageTime();
        int totalEndMinute = baseEndMinute + stoppage;

        int currMin = startMinute;
        int lastEventMin = startMinute;

        while (currMin <= totalEndMinute) {
            // ⚡ สุ่มข้ามเวลาทีละ 8 - 15 นาที (ทำให้ระยะห่างระหว่าง Event กว้างขึ้น)
            currMin += 8 + random.nextInt(8);
            if (currMin > totalEndMinute) break;

            // การันตีว่าถ้าผ่านไป 25 นาทีแล้วยังไม่มี Event จะบังคับเกิด 1 ครั้ง
            boolean forceEvent = (currMin - lastEventMin) >= 25;
            boolean triggerEvent = forceEvent || (random.nextDouble() < 0.25);

            if (triggerEvent) {
                lastEventMin = currMin;

                double homePlayerRatio = (double) homeStarters.size() / 11.0;
                double awayPlayerRatio = (double) awayStarters.size() / 11.0;

                double homeMod = MatchRules.totalModifier(home.getTactic(), away.getTactic(), home.getFormation())
                        * home.fitMultiplier() * homePlayerRatio;
                double awayMod = MatchRules.totalModifier(away.getTactic(), home.getTactic(), away.getFormation())
                        * away.fitMultiplier() * awayPlayerRatio;

                double actionChance = random.nextDouble();

                // 1. โอกาสเกิดการบุกและทำประตู
                if (actionChance < 0.35 * homeMod) {
                    if (tryAttack(homeStarters, awayStarters, home.fitMultiplier() * homePlayerRatio, away.fitMultiplier() * awayPlayerRatio, currMin, "Home", events)) {
                        homeGoals++;
                    }
                } else if (actionChance < 0.35 * (homeMod + awayMod)) {
                    if (tryAttack(awayStarters, homeStarters, away.fitMultiplier() * awayPlayerRatio, home.fitMultiplier() * homePlayerRatio, currMin, "Away", events)) {
                        awayGoals++;
                    }
                }
                // 2. โอกาสเกิดฟาวล์ / ใบเหลือง / ใบแดง / จุดโทษ
                else if (actionChance < 0.65) {
                    boolean isHomeFoul = random.nextBoolean();
                    String teamName = isHomeFoul ? "Home" : "Away";
                    List<Player> foulingStarters = isHomeFoul ? homeStarters : awayStarters;

                    if (random.nextDouble() < 0.18) {
                        events.add(new MatchEvent(currMin, MatchEvent.EventType.FOUL, teamName, null, "Foul inside the penalty box! Penalty!"));
                        if (random.nextDouble() < 0.75) {
                            if (isHomeFoul) { awayGoals++; } else { homeGoals++; }
                            events.add(new MatchEvent(currMin, MatchEvent.EventType.PENALTY, isHomeFoul ? "Away" : "Home", null, "Penalty goal scored!"));
                        } else {
                            events.add(new MatchEvent(currMin, MatchEvent.EventType.PENALTY, teamName, null, "Penalty missed or saved!"));
                        }
                    } else if (random.nextDouble() < 0.35) {
                        if (!foulingStarters.isEmpty()) {
                            Player cardedPlayer = foulingStarters.get(random.nextInt(foulingStarters.size()));
                            int yellowCount = yellowCardTracker.getOrDefault(cardedPlayer, 0) + 1;
                            yellowCardTracker.put(cardedPlayer, yellowCount);

                            if (yellowCount == 1) {
                                events.add(new MatchEvent(currMin, MatchEvent.EventType.YELLOW_CARD, teamName, cardedPlayer,
                                        "Yellow card issued to " + cardedPlayer.getName() + " 🟨"));
                            } else if (yellowCount >= 2) {
                                events.add(new MatchEvent(currMin, MatchEvent.EventType.RED_CARD, teamName, cardedPlayer,
                                        "Second Yellow Card! RED CARD for " + cardedPlayer.getName() + "! Sent off! 🟥"));

                                foulingStarters.remove(cardedPlayer);

                                if (foulingStarters.size() < 7) {
                                    String winner = isHomeFoul ? "Away" : "Home";
                                    events.add(new MatchEvent(currMin, MatchEvent.EventType.RED_CARD, teamName, cardedPlayer,
                                            "🚨 MATCH FORFEITED! " + teamName + " received 5 RED CARDS! " + winner + " WINS BY FORFEIT!"));
                                    break;
                                }
                            }
                        }
                    }
                }
                // 3. ครองบอลสร้างเกม
                else {
                    String possessionTeam = random.nextBoolean() ? "Home" : "Away";
                    events.add(new MatchEvent(currMin, MatchEvent.EventType.NONE, possessionTeam, null, possessionTeam + " builds up an attack from midfield."));
                }
            }
        }

        return new PeriodResult(periodNumber, homeGoals, awayGoals, events);
    }

    private boolean tryAttack(List<Player> attackingStarters, List<Player> defendingStarters,
                              double attackFitMod, double defenseFitMod,
                              int minute, String teamName, List<MatchEvent> events) {
        events.add(new MatchEvent(minute, MatchEvent.EventType.GOAL, teamName, null, teamName + " creates a clear scoring chance!"));

        double attackPower = 5.0 * attackFitMod;
        double defensePower = 5.0 * defenseFitMod;

        if (random.nextDouble() * attackPower > random.nextDouble() * defensePower) {
            events.add(new MatchEvent(minute, MatchEvent.EventType.GOAL, teamName, null, "GOAL! " + teamName + " scores!"));
            return true;
        } else {
            for (Player p : defendingStarters) {
                if (p instanceof GoalKeeper gk) {
                    gk.onSaveInGame();
                    break;
                }
            }
            events.add(new MatchEvent(minute, MatchEvent.EventType.GOAL, teamName, null, "Shot on target saved brilliantly by the Goalkeeper!"));
            return false;
        }
    }
}