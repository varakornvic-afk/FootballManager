package football.logic;

import football.model.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * ผลได้เปรียบ/เสียเปรียบทางแทคติกของ "ทีมหนึ่ง" เมื่อเจอกับอีกทีม (หน่วยเป็นสัดส่วน เช่น 0.04 = 4 จุดเปอร์เซ็นต์)
 * แยกเป็นองค์ประกอบ เพื่อให้ MatchEngine รวมเป็นโอกาสในแมตช์
 *
 * องค์ประกอบทั้งหมดคิดจากฝั่งตัวเองเทียบกับค่ากลาง จึงลบกันระหว่างสองทีมได้ผลที่สมมาตรกันเสมอ
 */
public final class TacticalEdge {

    // น้ำหนัก (ปรับจูนได้ที่นี่)
    static final double TACTIC_WIN_SHARE = 0.04;        // ชนะทางแผน; ฝั่งแพ้เสีย TACTIC_WIN_SHARE x exposure ของฟอร์เมชัน
    static final double CONTROL_SHARE_PER_POINT = 0.01; // ต่อ 1 แต้ม control ที่เกิน/ขาดจากค่ากลาง
    static final int NEUTRAL_CONTROL = 5;
    static final double FORMATION_FIT_SHARE = 0.20;     // (fitFor - 1.0) x ค่านี้: 1.15 = +3, 1.05 = +1, 0.95 = -1 จุด
    static final double FIT_SHARE_PER_POINT = 0.02;     // ต่อ 1 แต้ม fitScore ที่ตัวจริงดีกว่าตัวสำรอง
    static final double FIT_SHARE_MAX = 0.04;
    static final double STANCE_SHARE = 0.04;            // ALL_OUT_ATTACK +, PARK_THE_BUS -
    static final double FATIGUE_SHARE = 0.10;           // x (ตัวคูณความล้าเฉลี่ย - 1.0): หมดแรงทั้งทีม (0.7) = -3 จุด
    static final double MAN_SHARE = 0.03;               // ต่อผู้เล่นที่ขาดจาก 11 คน (ใบแดง)
    static final double OUT_OF_POSITION_SAME_GROUP = 0.005; // ลงผิดตำแหน่งแต่อยู่กลุ่มเดียวกัน (เช่น MF แทน MW)
    static final double OUT_OF_POSITION_CROSS_GROUP = 0.01; // ลงข้ามกลุ่ม (เช่น ST แทน CB)
    static final double OUT_OF_POSITION_MAX = 0.05;

    // คู่ปะทะตามตำแหน่ง (ฝ่ายบุกของเรา vs ฝ่ายรับของอีกทีม)
    static final double DUEL_SHARE_PER_POINT = 0.015;   // ต่อ 1 แต้มส่วนต่างค่าพลัง
    static final double DUEL_MAX = 0.03;                // เพดานต่อคู่
    static final double DUELS_TOTAL_MAX = 0.06;         // เพดานรวมทุกคู่
    // ส่วนต่างเฉลี่ยของพูลผู้เล่น (ไว้หักออก เพื่อให้ทีมเฉลี่ยเจอทีมเฉลี่ยได้ 0)
    static final double NEUTRAL_ST_CB = 0.5;            // (Pace ST - Pace CB + Heading ST - Heading CB) / 2
    static final double NEUTRAL_FW_FB = -0.25;          // Pace FW - Pace FB
    static final double NEUTRAL_MW_MF = 1.0;            // Passing MW - Defense MF

    private final double tacticMatchup;
    private final double formationControl;
    private final double formationFit;
    private final double squadFit;
    private final double duels;
    private final double stance;
    private final double manCount;
    private final double fatigue;       // ค่าลบเสมอ (หรือ 0)
    private final double outOfPosition; // ค่าลบเสมอ (หรือ 0)

    private TacticalEdge(double tacticMatchup, double formationControl, double formationFit,
                         double squadFit, double duels, double stance, double manCount,
                         double fatigue, double outOfPosition) {
        this.tacticMatchup = tacticMatchup;
        this.formationControl = formationControl;
        this.formationFit = formationFit;
        this.squadFit = squadFit;
        this.duels = duels;
        this.stance = stance;
        this.manCount = manCount;
        this.fatigue = fatigue;
        this.outOfPosition = outOfPosition;
    }

    /** คำนวณผลของ team เมื่อเจอ opponent */
    public static TacticalEdge of(TeamSetup team, TeamSetup opponent) {
        Tactic tactic = team.getTactic();
        Tactic opposing = opponent.getTactic();
        Formation formation = team.getFormation();

        double matchup = 0;
        if (tactic.beats(opposing)) matchup += TACTIC_WIN_SHARE;
        if (opposing.beats(tactic)) matchup -= TACTIC_WIN_SHARE * formation.getExposure();

        double control = (formation.getControl() - NEUTRAL_CONTROL) * CONTROL_SHARE_PER_POINT;
        double fitFor = formation.fitFor(tactic);
        double formationFit = (fitFor - 1.0) * FORMATION_FIT_SHARE;
        double squadFit = squadFit(team);
        double duels = duels(team, opponent);
        double stance = stanceShare(team.getStance());
        double men = (team.getStarters().size() - 11) * MAN_SHARE;
        double fatigue = (Fatigue.averageFactor(team) - 1.0) * FATIGUE_SHARE;
        double position = -outOfPositionPenalty(team);

        return new TacticalEdge(matchup, control, formationFit, squadFit, duels, stance, men, fatigue, position);
    }

    // การเข้าถึงแต่ละองค์ประกอบ (ใช้ในเทสต์และ UI)
    public double tacticMatchup() { return tacticMatchup; }
    public double formationControl() { return formationControl; }
    public double formationFit() { return formationFit; }
    public double squadFit() { return squadFit; }
    public double duels() { return duels; }
    public double stance() { return stance; }
    public double manCount() { return manCount; }
    public double fatigue() { return fatigue; }
    public double outOfPosition() { return outOfPosition; }

    public double total() {
        return tacticMatchup + formationControl + formationFit + squadFit + duels
                + stance + manCount + fatigue + outOfPosition;
    }

    // คู่ปะทะตามตำแหน่ง

    /**
     * ฝ่ายบุกของ team สู้ฝ่ายรับของ opponent (ใช้ตำแหน่งจริงของตัวจริงที่ลงสนาม)
     *   ST สู้ CB : เฉลี่ยของส่วนต่าง Pace กับ Heading
     *   FW สู้ FB : Pace
     *   MW สู้ MF : Passing ของ MW เทียบ Defense ของ MF
     * ข้ามคู่ที่ฝั่งใดฝั่งหนึ่งไม่มีผู้เล่นในกลุ่มนั้น
     */
    private static double duels(TeamSetup team, TeamSetup opponent) {
        double total = 0;

        List<Player> strikers = byPosition(team, Position.STRIKER);
        List<Player> centreBacks = byPosition(opponent, Position.CENTER_BACK);
        if (!strikers.isEmpty() && !centreBacks.isEmpty()) {
            double paceDiff = average(strikers, Player::getPace) - average(centreBacks, Player::getPace);
            double headDiff = average(strikers, Player::getHeading) - average(centreBacks, Player::getHeading);
            total += duelShare((paceDiff + headDiff) / 2.0 - NEUTRAL_ST_CB);
        }

        List<Player> forwards = byPosition(team, Position.FRONTWING);
        List<Player> fullbacks = byPosition(opponent, Position.FULLBACK);
        if (!forwards.isEmpty() && !fullbacks.isEmpty()) {
            double paceDiff = average(forwards, Player::getPace) - average(fullbacks, Player::getPace);
            total += duelShare(paceDiff - NEUTRAL_FW_FB);
        }

        List<Player> wingers = byPosition(team, Position.MIDWING);
        List<Player> midfielders = byPosition(opponent, Position.MIDFIELD);
        if (!wingers.isEmpty() && !midfielders.isEmpty()) {
            double diff = average(wingers, Player::getPassing) - average(midfielders, Player::getDefense);
            total += duelShare(diff - NEUTRAL_MW_MF);
        }

        return clamp(total, -DUELS_TOTAL_MAX, DUELS_TOTAL_MAX);
    }

    private static double duelShare(double pointDiff) {
        return clamp(pointDiff * DUEL_SHARE_PER_POINT, -DUEL_MAX, DUEL_MAX);
    }

    private static List<Player> byPosition(TeamSetup team, Position position) {
        List<Player> result = new ArrayList<>();
        for (Player p : team.getStarters()) {
            if (p.getPosition() == position) result.add(p);
        }
        return result;
    }

    private static double average(List<Player> players, ToIntFunction<Player> stat) {
        double total = 0;
        for (Player p : players) total += stat.applyAsInt(p);
        return total / players.size();
    }

    // ตัวช่วยคำนวณ

    /**
     * ความเข้ากับแผนของ "ตัวจริง" เทียบกับ "ทั้งทีม (ตัวจริง + ตัวสำรอง)" ไม่นับ GK
     * เทียบกับทีมตัวเองเพื่อให้สเกลค่าพลังที่ต่างกันของแต่ละแผน (Pace/Passing/Heading) ไม่ทำให้แผนไหนเสียเปรียบ
     */
    private static double squadFit(TeamSetup team) {
        Tactic tactic = team.getTactic();
        List<Player> starters = outfield(team.getStarters());
        if (starters.isEmpty()) return 0;

        List<Player> everyone = new ArrayList<>(starters);
        everyone.addAll(outfield(team.getBench()));

        double diff = averageFit(starters, tactic) - averageFit(everyone, tactic);
        return clamp(diff * FIT_SHARE_PER_POINT, -FIT_SHARE_MAX, FIT_SHARE_MAX);
    }

    private static List<Player> outfield(List<Player> players) {
        List<Player> result = new ArrayList<>();
        for (Player p : players) {
            if (p.getPosition() != Position.GK) result.add(p);
        }
        return result;
    }

    private static double averageFit(List<Player> players, Tactic tactic) {
        if (players.isEmpty()) return 0;
        double total = 0;
        for (Player p : players) total += tactic.fitScore(p);
        return total / players.size();
    }

    private static double stanceShare(Stance stance) {
        if (stance == Stance.ALL_OUT_ATTACK) return STANCE_SHARE;
        if (stance == Stance.PARK_THE_BUS) return -STANCE_SHARE;
        return 0;
    }

    // กลุ่มตำแหน่ง: 0 = รับ, 1 = กลาง, 2 = บุก
    private static int groupOf(Position position) {
        switch (position) {
            case CENTER_BACK:
            case FULLBACK:
                return 0;
            case MIDFIELD:
            case MIDWING:
                return 1;
            default:
                return 2; // STRIKER, FRONTWING
        }
    }

    /**
     * บทลงโทษที่ตัวจริงลงผิดตำแหน่งจากสล็อตของฟอร์เมชัน
     * นับเฉพาะคน "เกิน" จากสล็อต (คนที่หายไปเพราะใบแดงไม่นับ)
     * จับคู่คนเกินกับสล็อตที่ขาดในกลุ่มเดียวกันก่อน (ลงโทษเบา) ที่เหลือคิดแบบข้ามกลุ่ม
     */
    static double outOfPositionPenalty(TeamSetup team) {
        SlotLayout slots = team.getFormation().getSlots();

        Map<Position, Integer> actual = new EnumMap<>(Position.class);
        for (Player p : team.getStarters()) {
            if (p.getPosition() == Position.GK) continue;
            actual.merge(p.getPosition(), 1, Integer::sum);
        }

        int[] excess = new int[3];
        int[] deficit = new int[3];
        for (Position pos : Position.values()) {
            if (pos == Position.GK) continue;
            int group = groupOf(pos);
            int have = actual.getOrDefault(pos, 0);
            int need = slots.count(pos);
            excess[group] += Math.max(0, have - need);
            deficit[group] += Math.max(0, need - have);
        }

        int sameGroup = 0;
        int totalExcess = 0;
        for (int g = 0; g < 3; g++) {
            sameGroup += Math.min(excess[g], deficit[g]);
            totalExcess += excess[g];
        }

        double cost = sameGroup * OUT_OF_POSITION_SAME_GROUP
                + (totalExcess - sameGroup) * OUT_OF_POSITION_CROSS_GROUP;
        return Math.min(OUT_OF_POSITION_MAX, cost);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}