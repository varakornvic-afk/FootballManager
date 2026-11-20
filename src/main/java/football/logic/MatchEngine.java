package football.logic;

import football.event.MatchEvent;
import football.model.*;
import java.util.*;

/**
 * ตัวจำลองแมตช์ สุ่มเหตุการณ์ (ยิง เซฟ ฟาวล์ ใบเหลือง/แดง จุดโทษ) เป็นช่วงเวลา
 * โอกาสของแต่ละอย่างขึ้นกับแผน ฟอร์เมชัน ท่าทีเกม ฝีมือและความเหนื่อยของผู้เล่น
 * ใช้ seed เดียวกันจะได้ผลเหมือนเดิมทุกครั้ง
 */
public class MatchEngine {
    // "share" = ความน่าจะเป็นที่เหตุการณ์หนึ่งครั้งจะเกิดกับทีมเหย้า (ฐาน 50/50)
    // ตัวปรับแต่ละอย่าง (แผน, ฟอร์เมชัน, ตัวผู้เล่น, คู่ปะทะตามตำแหน่ง, Stance, ใบแดง) อยู่ใน TacticalEdge
    static final double BASE_EVENT_SHARE = 0.50;
    static final double MIN_SHARE = 0.30;
    static final double MAX_SHARE = 0.70;

    // "goal chance" = โอกาสที่การยิงปกติจะเป็นประตู (ฐาน 35% เหมือนเดิม)
    static final double BASE_GOAL_CHANCE = 0.35;
    static final double ATTACK_VS_DEFENSE_GOAL = 0.01; // ต่อ 1 แต้มที่ attack ของฟอร์เมชันผู้ยิงสูงกว่า defense ของฟอร์เมชันทีมรับ
    static final double TACTIC_WIN_GOAL = 0.03;       // ผู้ยิงชนะทางแผนผู้รับ: เข้าง่ายขึ้น (แพ้ทาง: ยากขึ้น)
    static final double STANCE_GOAL = 0.04;           // ทีมรับเล่น ALL_OUT_ATTACK: เสียง่ายขึ้น, PARK_THE_BUS: เสียยากขึ้น
    // ฝีมือผู้ยิง (Shoot) สู้แนวรับของทีมรับ (Defense เฉลี่ยของตัวสนาม + Saving ของ GK)
    static final double SHOT_SKILL_PER_POINT = 0.02;
    static final double SHOT_SKILL_MAX = 0.06;
    static final double NEUTRAL_SHOOT = 3.8;          // Shoot เฉลี่ยของผู้เล่นสนามในพูล
    static final double NEUTRAL_BLOCK = 4.3;          // แนวรับเฉลี่ยของพูล: (Defense เฉลี่ย 3.9 + Saving GK เฉลี่ย 9.25 / 2) / 2
    // โบนัสแผน Counter Attack: กองหน้าตัวเป้า (STRIKER) ที่ Shoot สูงกว่าค่าเฉลี่ยจบสกอร์คมขึ้น (ได้เฉพาะโบนัส ไม่มีโทษ)
    static final double COUNTER_STRIKER_PER_POINT = 0.01;
    static final double COUNTER_STRIKER_MAX = 0.03;
    // โบนัสแผน Possession: กองกลาง (MF) ในสนามที่ Passing เฉลี่ยสูงกว่าค่าเฉลี่ยของพูล สร้างจังหวะยิงให้ทั้งทีม
    static final double POSSESSION_PASSING_PER_POINT = 0.01;
    static final double POSSESSION_PASSING_MAX = 0.02;
    static final double NEUTRAL_MF_PASSING = 6.7;
    // โบนัสแผน Wide Play: ST ที่โหม่งเก่งกว่าค่าเฉลี่ยของพูล ได้ลูกโหม่งจากเปิดริมเส้น
    static final double WIDE_HEADING_PER_POINT = 0.01;
    static final double WIDE_HEADING_MAX = 0.02;
    static final double NEUTRAL_ST_HEADING = 3.5;
    static final double MIN_GOAL_CHANCE = 0.15;
    static final double MAX_GOAL_CHANCE = 0.60;

    private final Random rand;

    public MatchEngine() {
        this(System.currentTimeMillis());
    }

    public MatchEngine(long seed) {
        this.rand = new Random(seed);
    }

    /** จำลองการแข่งขันตามช่วงเวลา (Period) และคืนค่าผลการแข่งขันครึ่งนั้นๆ เป็น MatchHalfResult */
    public MatchHalfResult simulatePeriod(int startMinute, int endMinute, TeamSetup homeTeam, TeamSetup awayTeam) {
        List<MatchEvent> events = generateHalfEvents(startMinute, endMinute, homeTeam, awayTeam);

        int homeGoals = 0;

        for (MatchEvent ev : events) {
            if (ev.getEventType() == MatchEvent.EventType.GOAL) {
                if ("Home".equalsIgnoreCase(ev.getTeamName())) {
                    homeGoals++;
                }
            }
        }

        return new MatchHalfResult(homeGoals, events);
    }

    /** คำนวณและสุ่มเหตุการณ์ที่จะเกิดขึ้นในแต่ละครึ่ง */
    public List<MatchEvent> generateHalfEvents(int startMinute, int endMinute, TeamSetup homeTeam, TeamSetup awayTeam) {
        List<MatchEvent> events = new ArrayList<>();

        // ความน่าจะเป็นที่เหตุการณ์จะเกิดกับทีมเหย้า (คิดจากแผน/ฟอร์เมชัน/ตัวผู้เล่น/Stance/จำนวนคนตอนต้นครึ่ง)
        double homeShare = homeEventShare(homeTeam, awayTeam);

        for (int minute = startMinute; minute <= endMinute; minute += rand.nextInt(3) + 2) {
            double roll = rand.nextDouble();
            boolean isHomeEvent = rand.nextDouble() < homeShare;

            TeamSetup currentTeam = isHomeEvent ? homeTeam : awayTeam;
            TeamSetup defendingTeam = isHomeEvent ? awayTeam : homeTeam;
            String teamName = isHomeEvent ? "Home" : "Away";
            List<Player> activeStarters = currentTeam.getStarters();

            if (activeStarters.isEmpty()) continue;

            if (roll < 0.15) {
                double foulRoll = rand.nextDouble();

                if (foulRoll < 0.05) {
                    // 1. เลือกคนยิงจุดโทษตามค่ายิง (ยิ่ง Shoot เยอะ ยิ่งมีโอกาสถูกเลือก)
                    Player penaltyTaker = selectByShoot(activeStarters);
                    applyEventFatigue(penaltyTaker); // สุ่มลดความเหนื่อยคนยิงจุดโทษ

                    // 2. คำนวณโอกาสเข้าตามค่า Shoot ของคนยิง (ฐาน 50% + ค่า Shoot * 5%)
                    // ล็อกเพดานโอกาสเข้าสูงสุดไว้ที่ 95%
                    double scoreChance = Math.min(0.95, 0.50 + (penaltyTaker.getShoot() * 0.05));
                    boolean isPenaltyGoal = rand.nextDouble() < scoreChance;

                    if (isPenaltyGoal) {
                        events.add(new MatchEvent(minute, teamName, penaltyTaker, MatchEvent.EventType.GOAL,
                                "PENALTY GOAL! 🎯 scored by " + penaltyTaker.getName()));
                    } else {
                        events.add(new MatchEvent(minute, teamName, penaltyTaker, MatchEvent.EventType.PENALTY,
                                "PENALTY MISSED! ❌ saved by GK (Taker: " + penaltyTaker.getName() + ")"));

                        // บวกยอดเซฟให้ผู้รักษาประตูฝั่งตรงข้าม
                        for (Player p : defendingTeam.getStarters()) {
                            if (p instanceof GoalKeeper) {
                                ((GoalKeeper) p).onSaveInGame();
                                break;
                            }
                        }
                    }
                } else {
                    // เหตุการณ์ฟาวล์หรือโดนใบ
                    Player player = activeStarters.get(rand.nextInt(activeStarters.size()));
                    applyEventFatigue(player); // สุ่มลดความเหนื่อยคนทำฟาวล์

                    if (foulRoll < 0.08) {
                        Player sentOff = notGoalkeeper(activeStarters, player);
                        events.add(new MatchEvent(minute, teamName, sentOff, MatchEvent.EventType.RED_CARD,
                                "Red card for " + sentOff.getName() + " 🟥 (Sent off!)"));
                    } else if (foulRoll < 0.23) {
                        events.add(new MatchEvent(minute, teamName, player, MatchEvent.EventType.YELLOW_CARD,
                                "Yellow card for " + player.getName() + " 🟨"));
                    } else {
                        events.add(new MatchEvent(minute, teamName, player, MatchEvent.EventType.FOUL,
                                "Foul by " + player.getName() + " 🛑"));
                    }
                }
            } else if (roll < 0.30) {
                // เหตุการณ์ยิงประตูแบบปกติ: คนที่ Shoot สูงมีโอกาสได้ยิงมากกว่า (รวม GK ด้วย แต่ Shoot ต่ำจึงยิงน้อยมาก)
                Player shooter = selectByShoot(activeStarters);
                applyEventFatigue(shooter); // สุ่มลดความเหนื่อยคนยิง

                boolean isGoal = rand.nextDouble() < shotConversionChance(currentTeam, defendingTeam, shooter);

                if (isGoal) {
                    events.add(new MatchEvent(minute, teamName, shooter, MatchEvent.EventType.GOAL,
                            "GOAL! scored by " + shooter.getName() + " ⚽"));
                } else {
                    events.add(new MatchEvent(minute, teamName, shooter, MatchEvent.EventType.SAVE,
                            "Shot saved by GK 🧤"));

                    // เรียกระบบสะสมจำนวนเซฟของ GoalKeeper ทีมรับ
                    for (Player p : defendingTeam.getStarters()) {
                        if (p instanceof GoalKeeper) {
                            ((GoalKeeper) p).onSaveInGame();
                            break;
                        }
                    }
                }
            }
        }
        return events;
    }

    // ผลของแผน / ฟอร์เมชัน / ตัวผู้เล่น

    /**
     * ความน่าจะเป็นที่เหตุการณ์หนึ่งครั้งจะเกิดกับทีมเหย้า
     * ฐาน 50% แล้วบวกส่วนต่างของ TacticalEdge ระหว่างสองทีม (ผลสองฝั่งรวมกันได้ 100% เสมอ)
     */
    static double homeEventShare(TeamSetup home, TeamSetup away) {
        double delta = TacticalEdge.of(home, away).total() - TacticalEdge.of(away, home).total();
        return clamp(BASE_EVENT_SHARE + delta, MIN_SHARE, MAX_SHARE);
    }

    /** โอกาสที่การยิงปกติของ shooter (ทีม attacker) ต่อทีม defender จะเป็นประตู */
    static double shotConversionChance(TeamSetup attacker, TeamSetup defender, Player shooter) {
        double chance = BASE_GOAL_CHANCE;

        // ฟอร์เมชันบุกของผู้ยิง เทียบกับฟอร์เมชันรับของทีมรับ
        chance += (attacker.getFormation().getAttack() - defender.getFormation().getDefense())
                * ATTACK_VS_DEFENSE_GOAL;

        // ฝีมือผู้ยิง (Shoot) เทียบกับแนวรับของทีมรับ (Defense ของตัวสนาม + Saving ของ GK)
        double shoot = shooter.getShoot() * Fatigue.factor(shooter); // คนเหนื่อยยิงแม่นน้อยลง
        double skill = (shoot - NEUTRAL_SHOOT) - (defenseBlock(defender) - NEUTRAL_BLOCK);
        chance += clamp(skill * SHOT_SKILL_PER_POINT, -SHOT_SKILL_MAX, SHOT_SKILL_MAX);

        // Counter Attack: ST ที่ยิงแม่นกว่าค่าเฉลี่ยจบสกอร์คมขึ้นเล็กน้อย
        if (attacker.getTactic() instanceof CounterAttack && shooter.getPosition() == Position.STRIKER) {
            double sharp = shoot - NEUTRAL_SHOOT;
            if (sharp > 0) chance += Math.min(sharp * COUNTER_STRIKER_PER_POINT, COUNTER_STRIKER_MAX);
        }

        // Possession: MF ที่จ่ายเก่งช่วยสร้างจังหวะยิงให้ทั้งทีม (ได้เฉพาะโบนัส ไม่มีโทษ)
        if (attacker.getTactic() instanceof Possession) {
            double passing = averageMidfieldPassing(attacker);
            if (passing > NEUTRAL_MF_PASSING) {
                chance += Math.min((passing - NEUTRAL_MF_PASSING) * POSSESSION_PASSING_PER_POINT, POSSESSION_PASSING_MAX);
            }
        }

        // Wide Play: ST ที่โหม่งเก่งจบจากลูกเปิดริมเส้นได้ดีขึ้นเล็กน้อย
        if (attacker.getTactic() instanceof WidePlay && shooter.getPosition() == Position.STRIKER) {
            double heading = shooter.getHeading() - NEUTRAL_ST_HEADING;
            if (heading > 0) chance += Math.min(heading * WIDE_HEADING_PER_POINT, WIDE_HEADING_MAX);
        }

        if (attacker.getTactic().beats(defender.getTactic())) chance += TACTIC_WIN_GOAL;
        if (defender.getTactic().beats(attacker.getTactic())) chance -= TACTIC_WIN_GOAL;

        // ทีมรับที่บุกหมดตัวเปิดช่องให้เสียง่ายขึ้น / ทีมที่จอดรถบัสเสียยากขึ้น
        if (defender.getStance() == Stance.ALL_OUT_ATTACK) chance += STANCE_GOAL;
        if (defender.getStance() == Stance.PARK_THE_BUS) chance -= STANCE_GOAL;

        return clamp(chance, MIN_GOAL_CHANCE, MAX_GOAL_CHANCE);
    }

    /** Passing เฉลี่ยของกองกลาง (MIDFIELD) ที่อยู่ในสนาม; ไม่มีกองกลางเลยคืน 0 */
    static double averageMidfieldPassing(TeamSetup team) {
        return team.getStarters().stream()
                .filter(p -> p.getPosition() == Position.MIDFIELD)
                .mapToInt(Player::getPassing)
                .average().orElse(0);
    }

    /** แนวรับของทีม = เฉลี่ยของ (Defense เฉลี่ยของตัวสนาม) กับ (Saving ของ GK ที่ปรับสเกลลงครึ่งหนึ่ง) */
    static double defenseBlock(TeamSetup team) {
        double defenseTotal = 0;
        int outfield = 0;
        double saving = 0;
        for (Player p : team.getStarters()) {
            if (p.getPosition() == Position.GK) {
                saving = p.getSaving();
            } else {
                defenseTotal += p.getDefense();
                outfield++;
            }
        }
        double averageDefense = outfield == 0 ? 0 : defenseTotal / outfield;
        return (averageDefense + saving / 2.0) / 2.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** สุ่มเลือกผู้เล่นโดยให้น้ำหนักกับคนที่มีค่า Shoot สูงกว่า (ใช้กับทั้งจุดโทษและการยิงปกติ) */
    private Player selectByShoot(List<Player> players) {
        double totalWeight = 0;
        for (Player p : players) totalWeight += shootWeight(p);

        if (totalWeight <= 0) return players.get(rand.nextInt(players.size()));

        double roll = rand.nextDouble() * totalWeight;
        double currentSum = 0;

        for (Player p : players) {
            currentSum += shootWeight(p);
            if (roll < currentSum) {
                return p;
            }
        }
        return players.get(0);
    }

    /** น้ำหนักการถูกเลือกยิง = Shoot x ตัวคูณความล้า (คนหมดแรงยิงน้อยลง) */
    private static double shootWeight(Player p) {
        return p.getShoot() * Fatigue.factor(p);
    }

    /**
     * ผู้รักษาประตูไม่โดนใบแดง (ทีมต้องมี GK 1 คนเสมอ):
     * ถ้าสุ่มได้ GK ให้ใช้ผู้เล่นสนามคนถัดไปในลิสต์แทน (ไม่ใช้ rand เพื่อไม่ให้ผลสุ่มตาม seed เปลี่ยน)
     */
    private Player notGoalkeeper(List<Player> players, Player chosen) {
        if (chosen.getPosition() != Position.GK && !(chosen instanceof GoalKeeper)) return chosen;
        int start = players.indexOf(chosen);
        for (int i = 1; i < players.size(); i++) {
            Player p = players.get((start + i) % players.size());
            if (p.getPosition() != Position.GK && !(p instanceof GoalKeeper)) return p;
        }
        return chosen;
    }

    /** สุ่มลดค่า Stamina (โอกาส 10%) เฉพาะนักเตะที่ไม่ใช่ผู้รักษาประตู */
    private void applyEventFatigue(Player player) {
        if (player.getPosition() != Position.GK && rand.nextDouble() < 0.10) {
            player.setStamina(Math.max(0, player.getStamina() - 1));
        }
    }
}