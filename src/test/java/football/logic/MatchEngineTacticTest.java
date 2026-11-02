package football.logic;

import football.event.MatchEvent;
import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * เช็กว่าแผน (beats/exposure), ฟอร์เมชัน (control/attack/defense/fitFor), ความเข้ากับแผนของตัวผู้เล่น,
 * คู่ปะทะตามตำแหน่ง (ST-CB, FW-FB, MW-MF), ฝีมือผู้ยิงกับแนวรับ, Stance, ใบแดง และการลงผิดตำแหน่ง
 * มีผลต่อ MatchEngine / TacticalEdge จริง
 */
class MatchEngineTacticTest {

    private static final double EPS = 1e-9;

    private static final List<Position> OUTFIELD_POSITIONS = Arrays.stream(Position.values())
            .filter(p -> p != Position.GK)
            .toList();

    // ค่าพลังรวม 24 เหมือนกันทุกแบบ (pace, passing, heading, stamina, shoot, defense)
    private static final int[] NORMAL = {4, 4, 4, 4, 4, 4};
    private static final int[] FAST   = {8, 3, 3, 4, 2, 4};
    private static final int[] SLOW   = {2, 5, 4, 4, 5, 4};

    // ---------- ตัวช่วยสร้างผู้เล่นและทีม ----------

    /** สร้างชุดค่าพลังรวม 24 (stamina 4 คงที่ shoot คิดจากส่วนที่เหลือ) */
    private static int[] stats(int pace, int passing, int heading, int defense) {
        int shoot = 20 - pace - passing - heading - defense;
        if (shoot < 1) throw new IllegalArgumentException("stats exceed the 24-point budget");
        return new int[]{pace, passing, heading, 4, shoot, defense};
    }

    private static Player outfield(String name, Position position, int[] s) {
        return new Player(name, position, s[0], s[1], s[2], s[3], s[4], s[5]);
    }

    private static Player defaultKeeper(String prefix) {
        return new GoalKeeper(prefix + "GK", 2, 3, 2, 3, 2, 3, 9);
    }

    private static Player strongKeeper(String prefix) {
        return new GoalKeeper(prefix + "GK", 2, 2, 2, 3, 2, 3, 10); // saving 10
    }

    private static Player weakKeeper(String prefix) {
        return new GoalKeeper(prefix + "GK", 6, 5, 4, 3, 2, 1, 3);  // saving 3
    }

    /** ตำแหน่งผู้เล่นสนาม 10 คน ตรงตามสล็อตของฟอร์เมชันพอดี */
    private static List<Position> slotPositions(Formation formation) {
        List<Position> list = new ArrayList<>();
        for (Position p : OUTFIELD_POSITIONS) {
            for (int i = 0; i < formation.getSlots().count(p); i++) list.add(p);
        }
        return list;
    }

    /** ตัวจริงสนาม 10 คนตามสล็อต ใช้ค่าพลัง base ยกเว้นตำแหน่งที่กำหนดเองใน custom */
    private static List<Player> lineup(String prefix, Formation f, int[] base, Map<Position, int[]> custom) {
        List<Position> positions = slotPositions(f);
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            Position pos = positions.get(i);
            list.add(outfield(prefix + "F" + i, pos, custom.getOrDefault(pos, base)));
        }
        return list;
    }

    private static List<Player> lineup(String prefix, Formation f, Map<Position, int[]> custom) {
        return lineup(prefix, f, NORMAL, custom);
    }

    /** สร้างทีมจากผู้เล่นสนาม 10 คน (ตัวสำรอง 5 คนมีสัดส่วนตำแหน่งเหมือนตัวจริง ค่าพลังปกติ) */
    private static TeamSetup teamOf(String prefix, Tactic tactic, Formation formation, Stance stance,
                                    List<Player> outfield10, Player keeper) {
        List<Player> starters = new ArrayList<>();
        starters.add(keeper);
        starters.addAll(outfield10);
        List<Player> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bench.add(outfield(prefix + "B" + i, outfield10.get(2 * i).getPosition(), NORMAL));
        }
        return TeamSetup.create(tactic, formation, stance, starters, bench);
    }

    private static TeamSetup team(String prefix, Tactic tactic, Formation formation, Stance stance,
                                  int[] starterStats, int[] benchStats, List<Position> positions) {
        List<Player> starters = new ArrayList<>();
        starters.add(defaultKeeper(prefix));
        for (int i = 0; i < 10; i++) {
            starters.add(outfield(prefix + "F" + i, positions.get(i), starterStats));
        }
        List<Player> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bench.add(outfield(prefix + "B" + i, positions.get(2 * i), benchStats));
        }
        return TeamSetup.create(tactic, formation, stance, starters, bench);
    }

    private static TeamSetup team(String prefix, Tactic tactic, Formation formation, Stance stance) {
        return team(prefix, tactic, formation, stance, NORMAL, NORMAL, slotPositions(formation));
    }

    private static TeamSetup team(String prefix, Tactic tactic, Stance stance) {
        return team(prefix, tactic, new Formation442(), stance);
    }

    /** ทีม 4-4-2 แผน Counter Attack ค่าพลังปกติ ตัวสนามค่าพลัง base ทั้งหมด */
    private static TeamSetup plainTeam(String prefix, Formation f, int[] base, Player keeper) {
        return teamOf(prefix, new CounterAttack(), f, Stance.NORMAL, lineup(prefix, f, base, Map.of()), keeper);
    }

    // ---------- ชนะทางแผน / exposure ----------

    @Test
    void identicalTeamsAreEven() {
        TeamSetup a = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup b = team("A", new CounterAttack(), Stance.NORMAL);

        assertEquals(0.5, MatchEngine.homeEventShare(a, b), EPS);
    }

    @Test
    void winningTheTacticMatchupGivesMoreChances() {
        TeamSetup counter = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup possession = team("A", new Possession(), Stance.NORMAL);

        assertTrue(MatchEngine.homeEventShare(counter, possession) > 0.5);
        assertTrue(MatchEngine.homeEventShare(possession, counter) < 0.5);
    }

    @Test
    void tacticCycleIsComplete() {
        Tactic counter = new CounterAttack();
        Tactic possession = new Possession();
        Tactic wide = new WidePlay();

        assertTrue(counter.beats(possession));
        assertTrue(possession.beats(wide));
        assertTrue(wide.beats(counter));
        assertFalse(possession.beats(counter));
        assertFalse(wide.beats(possession));
        assertFalse(counter.beats(wide));
    }

    @Test
    void sharesAreMirrorImages() {
        TeamSetup counter = team("H", new CounterAttack(), new Formation532(), Stance.ALL_OUT_ATTACK);
        TeamSetup possession = team("A", new Possession(), new Formation433(), Stance.NORMAL);

        double homeSide = MatchEngine.homeEventShare(counter, possession);
        double awaySide = MatchEngine.homeEventShare(possession, counter);

        assertEquals(1.0, homeSide + awaySide, EPS);
    }

    @Test
    void beingBeatenCostsMoreWithAHighExposureFormation() {
        TeamSetup counter = team("H", new CounterAttack(), Stance.NORMAL);
        // Possession ถูก Counter Attack ชนะทาง: 4-4-2 (exposure 0.5) vs 3-4-3 (exposure 1.5)
        TeamSetup safe = team("A", new Possession(), new Formation442(), Stance.NORMAL);
        TeamSetup exposed = team("B", new Possession(), new Formation343(), Stance.NORMAL);

        double lossWhenSafe = TacticalEdge.of(safe, counter).tacticMatchup();
        double lossWhenExposed = TacticalEdge.of(exposed, counter).tacticMatchup();

        assertEquals(-TacticalEdge.TACTIC_WIN_SHARE * 0.5, lossWhenSafe, EPS);
        assertEquals(-TacticalEdge.TACTIC_WIN_SHARE * 1.5, lossWhenExposed, EPS);
        assertTrue(MatchEngine.homeEventShare(counter, exposed) > MatchEngine.homeEventShare(counter, safe));
    }

    // ---------- ฟอร์เมชัน ----------

    @Test
    void specialistFormationGivesABonusAndOffPlanFormationACost() {
        TeamSetup possessionIn433 = team("H", new Possession(), new Formation433(), Stance.NORMAL);
        TeamSetup counterIn433 = team("H", new CounterAttack(), new Formation433(), Stance.NORMAL);
        TeamSetup versatile = team("H", new Possession(), new Formation442(), Stance.NORMAL);
        TeamSetup opponent = team("A", new CounterAttack(), Stance.NORMAL);

        assertEquals(0.03, TacticalEdge.of(possessionIn433, opponent).formationFit(), EPS);
        assertEquals(-0.01, TacticalEdge.of(counterIn433, opponent).formationFit(), EPS);
        assertEquals(0.01, TacticalEdge.of(versatile, opponent).formationFit(), EPS);
    }

    @Test
    void highControlFormationGainsChances() {
        TeamSetup possessionIn433 = team("H", new Possession(), new Formation433(), Stance.NORMAL); // control 6
        TeamSetup opponent = team("A", new Possession(), new Formation442(), Stance.NORMAL);         // control 5

        assertEquals(0.01, TacticalEdge.of(possessionIn433, opponent).formationControl(), EPS);
        assertEquals(0.0, TacticalEdge.of(opponent, possessionIn433).formationControl(), EPS);
    }

    @Test
    void formationAttackAgainstDefenseChangesTheShotChance() {
        // 3-4-3 (attack 6) ยิงเข้าง่ายกว่า 5-3-2 (attack 3) 3 แต้ม => ต่างกัน 3 จุด เมื่อเจอทีมรับเดียวกัน
        TeamSetup attack343 = team("H", new CounterAttack(), new Formation343(), Stance.NORMAL);
        TeamSetup attack532 = team("H", new CounterAttack(), new Formation532(), Stance.NORMAL);
        TeamSetup defender = team("A", new CounterAttack(), new Formation442(), Stance.NORMAL);

        double with343 = MatchEngine.shotConversionChance(attack343, defender, attack343.getStarters().get(1));
        double with532 = MatchEngine.shotConversionChance(attack532, defender, attack532.getStarters().get(1));

        assertEquals(0.03, with343 - with532, EPS);
    }

    // ---------- ฝีมือผู้ยิงกับแนวรับ ----------

    @Test
    void anAverageTeamHasAnAverageDefenseBlock() {
        TeamSetup average = team("A", new CounterAttack(), Stance.NORMAL);

        assertEquals(4.25, MatchEngine.defenseBlock(average), EPS);
        assertEquals(MatchEngine.NEUTRAL_BLOCK, MatchEngine.defenseBlock(average), 0.1);
    }

    @Test
    void aBetterShooterScoresMoreOften() {
        Player sharp = outfield("HI", Position.STRIKER, new int[]{3, 3, 3, 4, 7, 4});
        Player blunt = outfield("LO", Position.STRIKER, new int[]{5, 5, 4, 4, 1, 5});
        TeamSetup attacker = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup defender = team("A", new CounterAttack(), Stance.NORMAL);

        assertTrue(MatchEngine.shotConversionChance(attacker, defender, sharp)
                > MatchEngine.shotConversionChance(attacker, defender, blunt));
    }

    @Test
    void counterAttackGivesSharpStrikersASmallBonusOnly() {
        Player sharpStriker = outfield("ST", Position.STRIKER, new int[]{3, 3, 3, 4, 7, 4});     // Shoot 7
        Player sharpFrontwing = outfield("FW", Position.FRONTWING, new int[]{3, 3, 3, 4, 7, 4});
        Player bluntStriker = outfield("BS", Position.STRIKER, new int[]{5, 5, 4, 4, 1, 5});     // Shoot 1
        Player bluntFrontwing = outfield("BF", Position.FRONTWING, new int[]{5, 5, 4, 4, 1, 5});
        TeamSetup counter = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup wide = team("W", new WidePlay(), Stance.NORMAL);
        TeamSetup defender = team("A", new CounterAttack(), Stance.NORMAL);

        double counterBonus = MatchEngine.shotConversionChance(counter, defender, sharpStriker)
                - MatchEngine.shotConversionChance(counter, defender, sharpFrontwing);
        double otherTacticBonus = MatchEngine.shotConversionChance(wide, defender, sharpStriker)
                - MatchEngine.shotConversionChance(wide, defender, sharpFrontwing);
        double bluntDiff = MatchEngine.shotConversionChance(counter, defender, bluntStriker)
                - MatchEngine.shotConversionChance(counter, defender, bluntFrontwing);

        // (7 - 3.8) * 0.01 = 0.032 => ชนเพดาน 0.03; FW และแผนอื่นไม่ได้โบนัสนี้
        assertEquals(MatchEngine.COUNTER_STRIKER_MAX, counterBonus, EPS);
        assertEquals(0.0, otherTacticBonus, EPS);
        // ST ที่ยิงต่ำกว่าค่าเฉลี่ยไม่ถูกหักเพิ่มจากโบนัสนี้
        assertEquals(0.0, bluntDiff, EPS);
    }

    @Test
    void possessionRewardsStrongPassingMidfieldersWithASmallBonus() {
        Player shooter = outfield("S", Position.STRIKER, NORMAL);
        Formation f = new Formation442();
        TeamSetup defender = team("A", new Possession(), Stance.NORMAL); // แผนเดียวกัน ไม่มีผลชนะทาง
        TeamSetup plain = teamOf("P", new Possession(), f, Stance.NORMAL, lineup("P", f, Map.of()), defaultKeeper("P"));
        TeamSetup creative = teamOf("C", new Possession(), f, Stance.NORMAL,
                lineup("C", f, Map.of(Position.MIDFIELD, stats(4, 9, 3, 2))), defaultKeeper("C")); // MF Passing 9
        TeamSetup counterCreative = teamOf("D", new CounterAttack(), f, Stance.NORMAL,
                lineup("D", f, Map.of(Position.MIDFIELD, stats(4, 9, 3, 2))), defaultKeeper("D"));
        TeamSetup counterPlain = teamOf("E", new CounterAttack(), f, Stance.NORMAL, lineup("E", f, Map.of()), defaultKeeper("E"));

        double possessionBonus = MatchEngine.shotConversionChance(creative, defender, shooter)
                - MatchEngine.shotConversionChance(plain, defender, shooter);
        // Counter Attack ไม่ได้โบนัสนี้ (เทียบ Counter เจอ Counter ไม่มีผลชนะทาง)
        TeamSetup counterDefender = team("B", new CounterAttack(), Stance.NORMAL);
        double counterDiff = MatchEngine.shotConversionChance(counterCreative, counterDefender, shooter)
                - MatchEngine.shotConversionChance(counterPlain, counterDefender, shooter);

        // (9 - 6.7) * 0.01 = 0.023 => ชนเพดาน 0.02
        assertEquals(MatchEngine.POSSESSION_PASSING_MAX, possessionBonus, EPS);
        assertEquals(0.0, counterDiff, EPS);
        // MF จ่ายธรรมดา (Passing 4 ต่ำกว่าค่าเฉลี่ย) ไม่ได้โบนัสและไม่ถูกหัก
        assertEquals(4.0, MatchEngine.averageMidfieldPassing(plain), EPS);
    }

    @Test
    void widePlayGivesHeadingStrikersASmallBonusOnly() {
        Player aerial = outfield("ST", Position.STRIKER, new int[]{3, 3, 6, 4, 4, 4});        // Heading 6
        Player aerialFrontwing = outfield("FW", Position.FRONTWING, new int[]{3, 3, 6, 4, 4, 4});
        Player weakAir = outfield("WK", Position.STRIKER, new int[]{3, 3, 2, 4, 4, 8});       // Heading 2
        Player weakAirFrontwing = outfield("WF", Position.FRONTWING, new int[]{3, 3, 2, 4, 4, 8});
        TeamSetup wide = team("W", new WidePlay(), Stance.NORMAL);
        TeamSetup counter = team("C", new CounterAttack(), Stance.NORMAL);
        TeamSetup defender = team("A", new WidePlay(), Stance.NORMAL);

        double wideBonus = MatchEngine.shotConversionChance(wide, defender, aerial)
                - MatchEngine.shotConversionChance(wide, defender, aerialFrontwing);
        double weakDiff = MatchEngine.shotConversionChance(wide, defender, weakAir)
                - MatchEngine.shotConversionChance(wide, defender, weakAirFrontwing);

        // (6 - 3.5) * 0.01 = 0.025 => ชนเพดาน 0.02; FW ไม่ได้ ST ที่โหม่งต่ำไม่โดนหัก
        assertEquals(MatchEngine.WIDE_HEADING_MAX, wideBonus, EPS);
        assertEquals(0.0, weakDiff, EPS);
        assertTrue(counter.getTactic() instanceof CounterAttack);
    }

    @Test
    void wideStrikerBonusDoesNotApplyToOtherTactics() {
        Player aerial = outfield("ST", Position.STRIKER, new int[]{3, 3, 6, 4, 4, 4});
        Player aerialFrontwing = outfield("FW", Position.FRONTWING, new int[]{3, 3, 6, 4, 4, 4});
        TeamSetup possession = team("P", new Possession(), Stance.NORMAL);
        TeamSetup defender = team("A", new Possession(), Stance.NORMAL);

        assertEquals(0.0, MatchEngine.shotConversionChance(possession, defender, aerial)
                - MatchEngine.shotConversionChance(possession, defender, aerialFrontwing), EPS);
    }

    @Test
    void widePlayInFiveThreeTwoLosesTwoPercent() {
        TeamSetup wide532 = team("H", new WidePlay(), new Formation532(), Stance.NORMAL);
        TeamSetup possession532 = team("H", new Possession(), new Formation532(), Stance.NORMAL);
        TeamSetup opponent = team("A", new Possession(), Stance.NORMAL);

        assertEquals(-0.02, TacticalEdge.of(wide532, opponent).formationFit(), EPS);
        assertEquals(-0.01, TacticalEdge.of(possession532, opponent).formationFit(), EPS); // Possession ไม่โดน -2%
    }

    @Test
    void aTiredTeamGetsFewerChances() {
        TeamSetup fresh = team("A", new CounterAttack(), Stance.NORMAL);
        TeamSetup tired = team("H", new CounterAttack(), Stance.NORMAL);
        for (Player p : tired.getStarters()) {
            if (p.getPosition() != Position.GK) p.setStamina(0);
        }

        // หมดแรงทั้งทีม (0.7) => (0.7 - 1.0) x 0.10 = -3 จุด
        assertEquals(-0.03, TacticalEdge.of(tired, fresh).fatigue(), EPS);
        assertEquals(0.0, TacticalEdge.of(fresh, tired).fatigue(), EPS);
        assertEquals(0.5 - 0.03, MatchEngine.homeEventShare(tired, fresh), EPS);
        assertEquals(0.5 + 0.03, MatchEngine.homeEventShare(fresh, tired), EPS);
    }

    @Test
    void aTiredShooterScoresLessOften() {
        Player fresh = outfield("F", Position.STRIKER, new int[]{3, 3, 3, 4, 7, 4});
        Player tired = outfield("T", Position.STRIKER, new int[]{3, 3, 3, 4, 7, 4});
        tired.setStamina(0);
        TeamSetup attacker = team("H", new Possession(), Stance.NORMAL);
        TeamSetup defender = team("A", new Possession(), Stance.NORMAL);

        assertTrue(MatchEngine.shotConversionChance(attacker, defender, tired)
                < MatchEngine.shotConversionChance(attacker, defender, fresh));
    }

    @Test
    void tiredPlayersTakeFewerShots() {
        int freshShots = 0;
        int tiredShots = 0;
        for (long seed = 0; seed < 1500; seed++) {
            Formation f = new Formation442();
            List<Player> outfield = lineup("H", f, Map.of());
            Position pos0 = outfield.get(0).getPosition();
            Position pos1 = outfield.get(1).getPosition();
            Player fresh = outfield("FRESH", pos0, new int[]{3, 3, 3, 4, 7, 4});
            Player tired = outfield("TIRED", pos1, new int[]{3, 3, 3, 4, 7, 4});
            tired.setStamina(0);
            outfield.set(0, fresh);
            outfield.set(1, tired);
            TeamSetup home = teamOf("H", new CounterAttack(), f, Stance.NORMAL, outfield, defaultKeeper("H"));
            TeamSetup away = team("A", new CounterAttack(), Stance.NORMAL);

            for (MatchEvent ev : new MatchEngine(seed).generateHalfEvents(1, 45, home, away)) {
                if (ev.getEventType() != MatchEvent.EventType.GOAL && ev.getEventType() != MatchEvent.EventType.SAVE) continue;
                if (ev.getPlayer() == null) continue;
                if ("FRESH".equals(ev.getPlayer().getName())) freshShots++;
                if ("TIRED".equals(ev.getPlayer().getName())) tiredShots++;
            }
        }

        assertTrue(freshShots > tiredShots,
                "a rested player should shoot more than an exhausted one: " + freshShots + " vs " + tiredShots);
    }

    @Test
    void strongDefendersMakeScoringHarder() {
        TeamSetup attacker = team("H", new CounterAttack(), Stance.NORMAL);
        Player shooter = attacker.getStarters().get(1);

        TeamSetup solid = plainTeam("S", new Formation442(), stats(4, 4, 4, 7), defaultKeeper("S"));
        TeamSetup leaky = plainTeam("L", new Formation442(), stats(4, 4, 4, 2), defaultKeeper("L"));

        assertTrue(MatchEngine.shotConversionChance(attacker, solid, shooter)
                < MatchEngine.shotConversionChance(attacker, leaky, shooter));
    }

    @Test
    void aBetterGoalkeeperMakesScoringHarder() {
        TeamSetup attacker = team("H", new CounterAttack(), Stance.NORMAL);
        Player shooter = attacker.getStarters().get(1);

        TeamSetup withStrongKeeper = plainTeam("S", new Formation442(), NORMAL, strongKeeper("S"));
        TeamSetup withWeakKeeper = plainTeam("W", new Formation442(), NORMAL, weakKeeper("W"));

        assertTrue(MatchEngine.shotConversionChance(attacker, withStrongKeeper, shooter)
                < MatchEngine.shotConversionChance(attacker, withWeakKeeper, shooter));
    }

    @Test
    void shotsAreMoreLikelyToScoreWhenTheAttackerWinsTheTacticMatchup() {
        TeamSetup counter = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup possession = team("A", new Possession(), Stance.NORMAL);

        double counterShoots = MatchEngine.shotConversionChance(counter, possession, counter.getStarters().get(1));
        double possessionShoots = MatchEngine.shotConversionChance(possession, counter, possession.getStarters().get(1));

        // ตัวผู้ยิง/แนวรับเหมือนกัน ต่างกันแค่ผลชนะทาง (+3 จุด vs -3 จุด)
        assertEquals(2 * MatchEngine.TACTIC_WIN_GOAL, counterShoots - possessionShoots, EPS);
    }

    // ---------- คู่ปะทะตามตำแหน่ง ----------

    @Test
    void strikerPaceAndHeadingAgainstCentreBacksChangesTheEdge() {
        Formation f = new Formation442(); // ST 2, CB 2
        TeamSetup defender = teamOf("D", new CounterAttack(), f, Stance.NORMAL, lineup("D", f, Map.of()), defaultKeeper("D"));
        TeamSetup normal = teamOf("A", new CounterAttack(), f, Stance.NORMAL, lineup("A", f, Map.of()), defaultKeeper("A"));
        TeamSetup sharp = teamOf("B", new CounterAttack(), f, Stance.NORMAL,
                lineup("B", f, Map.of(Position.STRIKER, stats(7, 3, 5, 1))), defaultKeeper("B"));

        // ST: Pace 7 / Heading 5 เทียบ CB 4 / 4 => ((3 + 1) / 2 - 0.5) = 1.5 แต้ม; ST ปกติ => -0.5 แต้ม
        double diff = TacticalEdge.of(sharp, defender).duels() - TacticalEdge.of(normal, defender).duels();

        assertEquals(2.0 * TacticalEdge.DUEL_SHARE_PER_POINT, diff, EPS);
    }

    @Test
    void forwardPaceAgainstFullbacksChangesTheEdge() {
        Formation attackF = new Formation433(); // FW 2
        Formation defendF = new Formation442(); // FB 2
        TeamSetup defender = teamOf("D", new CounterAttack(), defendF, Stance.NORMAL, lineup("D", defendF, Map.of()), defaultKeeper("D"));
        TeamSetup normal = teamOf("A", new CounterAttack(), attackF, Stance.NORMAL, lineup("A", attackF, Map.of()), defaultKeeper("A"));
        TeamSetup quick = teamOf("B", new CounterAttack(), attackF, Stance.NORMAL,
                lineup("B", attackF, Map.of(Position.FRONTWING, stats(5, 4, 4, 4))), defaultKeeper("B"));

        // FW Pace 5 เทียบ FB 4 => ต่าง 1 แต้ม (เพิ่มขึ้นจาก 0 แต้ม)
        double diff = TacticalEdge.of(quick, defender).duels() - TacticalEdge.of(normal, defender).duels();

        assertEquals(1.0 * TacticalEdge.DUEL_SHARE_PER_POINT, diff, EPS);
    }

    @Test
    void wingerPassingAgainstMidfieldDefenseChangesTheEdge() {
        Formation attackF = new Formation343(); // MW 2
        Formation defendF = new Formation442(); // MF 2
        TeamSetup defender = teamOf("D", new CounterAttack(), defendF, Stance.NORMAL, lineup("D", defendF, Map.of()), defaultKeeper("D"));
        TeamSetup normal = teamOf("A", new CounterAttack(), attackF, Stance.NORMAL, lineup("A", attackF, Map.of()), defaultKeeper("A"));
        TeamSetup creative = teamOf("B", new CounterAttack(), attackF, Stance.NORMAL,
                lineup("B", attackF, Map.of(Position.MIDWING, stats(4, 6, 4, 4))), defaultKeeper("B"));

        // MW Passing 6 เทียบ MF Defense 4 => ต่าง 2 แต้ม (เพิ่มขึ้นจาก 0 แต้ม)
        double diff = TacticalEdge.of(creative, defender).duels() - TacticalEdge.of(normal, defender).duels();

        assertEquals(2.0 * TacticalEdge.DUEL_SHARE_PER_POINT, diff, EPS);
    }

    @Test
    void aDuelIsSkippedWhenTheDefendingGroupIsMissing() {
        Formation attackF = new Formation433(); // FW 2
        Formation defendF = new Formation343(); // FB 0 => ไม่มีคู่ FW-FB
        TeamSetup defender = teamOf("D", new CounterAttack(), defendF, Stance.NORMAL, lineup("D", defendF, Map.of()), defaultKeeper("D"));
        TeamSetup normal = teamOf("A", new CounterAttack(), attackF, Stance.NORMAL, lineup("A", attackF, Map.of()), defaultKeeper("A"));
        TeamSetup quick = teamOf("B", new CounterAttack(), attackF, Stance.NORMAL,
                lineup("B", attackF, Map.of(Position.FRONTWING, stats(8, 3, 3, 3))), defaultKeeper("B"));

        assertEquals(TacticalEdge.of(normal, defender).duels(), TacticalEdge.of(quick, defender).duels(), EPS);
    }

    @Test
    void totalDuelEdgeIsCapped() {
        Formation f = new Formation442(); // ST 2, CB 2, MW 2, MF 2
        TeamSetup defender = teamOf("D", new CounterAttack(), f, Stance.NORMAL,
                lineup("D", f, Map.of(Position.CENTER_BACK, stats(2, 3, 2, 8))), defaultKeeper("D"));
        TeamSetup attacker = teamOf("A", new CounterAttack(), f, Stance.NORMAL,
                lineup("A", f, Map.of(Position.STRIKER, stats(8, 3, 5, 1), Position.MIDWING, stats(4, 8, 2, 2))),
                defaultKeeper("A"));

        double duels = TacticalEdge.of(attacker, defender).duels();

        assertEquals(TacticalEdge.DUELS_TOTAL_MAX, duels, EPS);
    }

    // ---------- ความเข้ากับแผนของตัวผู้เล่น ----------

    @Test
    void startersThatFitTheTacticBetterThanTheBenchGiveAnEdge() {
        // CounterAttack วัดจาก pace: ตัวจริงเร็ว สำรองช้า
        TeamSetup wellPicked = team("H", new CounterAttack(), new Formation442(), Stance.NORMAL,
                FAST, SLOW, slotPositions(new Formation442()));
        TeamSetup poorlyPicked = team("H", new CounterAttack(), new Formation442(), Stance.NORMAL,
                SLOW, FAST, slotPositions(new Formation442()));
        TeamSetup opponent = team("A", new CounterAttack(), Stance.NORMAL);

        double good = TacticalEdge.of(wellPicked, opponent).squadFit();
        double bad = TacticalEdge.of(poorlyPicked, opponent).squadFit();

        assertTrue(good > 0);
        assertTrue(good <= TacticalEdge.FIT_SHARE_MAX + EPS);
        assertTrue(bad < 0);
    }

    @Test
    void anEvenSquadGetsNoFitBonusRegardlessOfTheTactic() {
        TeamSetup opponent = team("A", new CounterAttack(), Stance.NORMAL);

        // ตัวจริง/สำรองค่าพลังเท่ากันและสัดส่วนตำแหน่งเหมือนกัน => 0 ไม่ว่าแผนจะวัดจาก Pace, Passing หรือ Heading
        for (Tactic tactic : List.of(new CounterAttack(), new Possession(), new WidePlay())) {
            TeamSetup even = team("H", tactic, new Formation442(), Stance.NORMAL);
            assertEquals(0.0, TacticalEdge.of(even, opponent).squadFit(), EPS, tactic.getName());
        }
    }

    @Test
    void widePlayRewardsWingersWithGoodPassing() {
        Tactic wide = new WidePlay();
        Player passingWinger = new Player("W1", Position.MIDWING, 3, 7, 2, 4, 6, 2);
        Player plainWinger = new Player("W2", Position.MIDWING, 3, 3, 2, 4, 6, 6);
        Player midfielder = new Player("M1", Position.MIDFIELD, 3, 7, 2, 4, 6, 2);

        assertEquals(2 + 7, wide.fitScore(passingWinger));
        assertTrue(wide.fitScore(passingWinger) > wide.fitScore(plainWinger));
        // กองกลางธรรมดาที่ Passing สูงไม่ได้โบนัสนี้
        assertEquals(2, wide.fitScore(midfielder));
    }

    @Test
    void possessionRewardsMidfieldersWithGoodPassing() {
        Tactic possession = new Possession();
        Player midfielder = new Player("M1", Position.MIDFIELD, 3, 7, 2, 4, 6, 2);
        Player winger = new Player("W1", Position.MIDWING, 3, 7, 2, 4, 6, 2);

        assertEquals(7 + 7 / 2, possession.fitScore(midfielder));
        assertEquals(7, possession.fitScore(winger)); // ตำแหน่งอื่นได้เฉพาะ Passing ปกติ
    }

    @Test
    void counterAttackRewardsFastForwards() {
        Tactic counter = new CounterAttack();
        Player striker = new Player("S1", Position.STRIKER, 8, 3, 3, 4, 2, 4);
        Player frontwing = new Player("F1", Position.FRONTWING, 8, 3, 3, 4, 2, 4);
        Player centreBack = new Player("C1", Position.CENTER_BACK, 8, 3, 3, 4, 2, 4);

        assertEquals(8 + 8 / 2, counter.fitScore(striker));
        assertEquals(8 + 8 / 2, counter.fitScore(frontwing));
        assertEquals(8, counter.fitScore(centreBack));
    }

    // ---------- Stance ----------

    @Test
    void allOutAttackGetsMoreChancesAndParkTheBusGetsFewer() {
        TeamSetup normal = team("A", new CounterAttack(), Stance.NORMAL);

        TeamSetup attacking = team("H", new CounterAttack(), Stance.ALL_OUT_ATTACK);
        TeamSetup defending = team("H", new CounterAttack(), Stance.PARK_THE_BUS);

        assertTrue(MatchEngine.homeEventShare(attacking, normal) > 0.5);
        assertTrue(MatchEngine.homeEventShare(defending, normal) < 0.5);
    }

    @Test
    void allOutAttackConcedesEasierAndParkTheBusConcedesHarder() {
        TeamSetup shooter = team("A", new CounterAttack(), Stance.NORMAL);
        Player taker = shooter.getStarters().get(1);

        TeamSetup exposed = team("H", new CounterAttack(), Stance.ALL_OUT_ATTACK);
        TeamSetup normal = team("N", new CounterAttack(), Stance.NORMAL);
        TeamSetup parked = team("P", new CounterAttack(), Stance.PARK_THE_BUS);

        double base = MatchEngine.shotConversionChance(shooter, normal, taker);
        assertEquals(base + MatchEngine.STANCE_GOAL, MatchEngine.shotConversionChance(shooter, exposed, taker), EPS);
        assertEquals(base - MatchEngine.STANCE_GOAL, MatchEngine.shotConversionChance(shooter, parked, taker), EPS);
    }

    // ---------- ใบแดง / จำนวนคน ----------

    @Test
    void playingWithTenMenLowersTheShare() {
        TeamSetup full = team("A", new CounterAttack(), Stance.NORMAL);
        TeamSetup home = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup tenMen = home.withoutPlayer(home.getStarters().get(1));

        assertTrue(MatchEngine.homeEventShare(tenMen, full) < 0.5);
        assertTrue(MatchEngine.homeEventShare(full, tenMen) > 0.5);
    }

    @Test
    void shareNeverLeavesTheAllowedRange() {
        TeamSetup strong = team("H", new CounterAttack(), new Formation532(), Stance.ALL_OUT_ATTACK,
                FAST, SLOW, slotPositions(new Formation532()));
        TeamSetup weak = team("A", new Possession(), new Formation343(), Stance.PARK_THE_BUS);
        for (int i = 1; i <= 4; i++) weak = weak.withoutPlayer(weak.getStarters().get(1));

        assertTrue(MatchEngine.homeEventShare(strong, weak) <= MatchEngine.MAX_SHARE + EPS);
        assertTrue(MatchEngine.homeEventShare(weak, strong) >= MatchEngine.MIN_SHARE - EPS);
    }

    // ---------- ลงผิดตำแหน่ง ----------

    @Test
    void exactSlotLineupHasNoPositionPenalty() {
        for (Formation f : List.of(new Formation442(), new Formation433(), new Formation343(), new Formation532())) {
            TeamSetup t = team("H", new CounterAttack(), f, Stance.NORMAL);
            assertEquals(0.0, TacticalEdge.outOfPositionPenalty(t), EPS, f.getName());
        }
    }

    @Test
    void playingTheSameGroupOutOfPositionCostsHalf() {
        // 4-4-2 ต้องการ MF 2 + MW 2 : ใส่ MF 4 แทน (กลางทั้งคู่) => ผิด 2 คน กลุ่มเดียวกัน
        Formation f = new Formation442();
        List<Position> positions = new ArrayList<>(slotPositions(f));
        positions.replaceAll(p -> p == Position.MIDWING ? Position.MIDFIELD : p);
        TeamSetup t = team("H", new CounterAttack(), f, Stance.NORMAL, NORMAL, NORMAL, positions);

        assertEquals(2 * TacticalEdge.OUT_OF_POSITION_SAME_GROUP, TacticalEdge.outOfPositionPenalty(t), EPS);
    }

    @Test
    void playingAcrossGroupsCostsFull() {
        // 4-4-2 ต้องการ CB 2 : ใส่ ST แทน 2 คน => ข้ามกลุ่ม
        Formation f = new Formation442();
        List<Position> positions = new ArrayList<>(slotPositions(f));
        int replaced = 0;
        for (int i = 0; i < positions.size() && replaced < 2; i++) {
            if (positions.get(i) == Position.CENTER_BACK) {
                positions.set(i, Position.STRIKER);
                replaced++;
            }
        }
        TeamSetup t = team("H", new CounterAttack(), f, Stance.NORMAL, NORMAL, NORMAL, positions);

        assertEquals(2 * TacticalEdge.OUT_OF_POSITION_CROSS_GROUP, TacticalEdge.outOfPositionPenalty(t), EPS);
    }

    @Test
    void positionPenaltyIsCapped() {
        Formation f = new Formation442();
        List<Position> allStrikers = Collections.nCopies(10, Position.STRIKER);
        TeamSetup t = team("H", new CounterAttack(), f, Stance.NORMAL, NORMAL, NORMAL, allStrikers);

        assertEquals(TacticalEdge.OUT_OF_POSITION_MAX, TacticalEdge.outOfPositionPenalty(t), EPS);
    }

    @Test
    void aRedCardDoesNotCountAsPlayingOutOfPosition() {
        TeamSetup t = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup tenMen = t.withoutPlayer(t.getStarters().get(1));

        assertEquals(0.0, TacticalEdge.outOfPositionPenalty(tenMen), EPS);
    }

    // ---------- โบนัสที่ดึงไปแสดงใน UI ได้ (ตอนนี้เกมไม่แสดง pop-up) ----------

    @Test
    void evenTeamsListNoBonuses() {
        TeamSetup a = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup b = team("A", new CounterAttack(), Stance.NORMAL);

        assertTrue(TacticalEdge.of(a, b).bonuses().isEmpty());
    }

    @Test
    void winningTheMatchupListsABonusWithItsPercentage() {
        TeamSetup counter = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup possession = team("A", new Possession(), Stance.NORMAL);

        List<TacticalEdge.Bonus> bonuses = TacticalEdge.of(counter, possession).bonuses();

        assertEquals(1, bonuses.size());
        assertEquals("Counter Attack beats Possession", bonuses.get(0).label());
        assertEquals("+4%", bonuses.get(0).percentText());
        assertTrue(TacticalEdge.of(possession, counter).bonuses().isEmpty());
    }

    @Test
    void aSpecialistFormationAndAGoodLineupAreListedAsBonuses() {
        TeamSetup well = team("H", new Possession(), new Formation433(), Stance.NORMAL,
                new int[]{3, 8, 3, 4, 2, 4}, new int[]{3, 2, 3, 4, 8, 4}, slotPositions(new Formation433()));
        TeamSetup opponent = team("A", new WidePlay(), new Formation442(), Stance.NORMAL);

        List<String> labels = TacticalEdge.of(well, opponent).bonuses().stream()
                .map(TacticalEdge.Bonus::label).toList();

        assertTrue(labels.contains("4-3-3 suits Possession"), labels.toString());
        assertTrue(labels.contains("Lineup fits Possession"), labels.toString());
        assertTrue(labels.contains("Possession beats Wide Play"), labels.toString());
    }

    // ---------- ผลจริงในหลายแมตช์ ----------

    @Test
    void theFavouredTacticScoresMoreOverManyMatches() {
        TeamSetup counter = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup possession = team("A", new Possession(), Stance.NORMAL);

        int counterGoals = 0;
        int possessionGoals = 0;
        for (long seed = 0; seed < 3000; seed++) {
            MatchEngine engine = new MatchEngine(seed);
            for (MatchEvent ev : engine.generateHalfEvents(1, 45, counter, possession)) {
                if (ev.getEventType() != MatchEvent.EventType.GOAL) continue;
                if ("Home".equalsIgnoreCase(ev.getTeamName())) counterGoals++;
                else possessionGoals++;
            }
        }

        assertTrue(counterGoals > possessionGoals,
                "Counter Attack should out-score Possession: " + counterGoals + " vs " + possessionGoals);
        // ไม่ใช่ว่าชนะขาดทุกครั้ง: ฝั่งแพ้ทางต้องยังทำประตูได้
        assertTrue(possessionGoals > 0);
    }

    @Test
    void playersWithHigherShootTakeMoreShots() {
        Formation f = new Formation442();
        List<Player> outfield = lineup("H", f, Map.of());
        Position hiPos = outfield.get(0).getPosition();
        Position loPos = outfield.get(1).getPosition();
        outfield.set(0, outfield("HI", hiPos, new int[]{3, 3, 3, 4, 7, 4}));
        outfield.set(1, outfield("LO", loPos, new int[]{5, 5, 4, 4, 1, 5}));
        TeamSetup home = teamOf("H", new CounterAttack(), f, Stance.NORMAL, outfield, defaultKeeper("H"));
        TeamSetup away = team("A", new CounterAttack(), Stance.NORMAL);

        int highShootShots = 0;
        int lowShootShots = 0;
        for (long seed = 0; seed < 2000; seed++) {
            MatchEngine engine = new MatchEngine(seed);
            for (MatchEvent ev : engine.generateHalfEvents(1, 45, home, away)) {
                if (ev.getEventType() != MatchEvent.EventType.GOAL && ev.getEventType() != MatchEvent.EventType.SAVE) continue;
                if (ev.getPlayer() == null) continue;
                if ("HI".equals(ev.getPlayer().getName())) highShootShots++;
                if ("LO".equals(ev.getPlayer().getName())) lowShootShots++;
            }
        }

        assertTrue(highShootShots > lowShootShots,
                "Shoot 7 should shoot more than Shoot 1: " + highShootShots + " vs " + lowShootShots);
    }

    @Test
    void goalkeepersCanShootButVeryRarely() {
        TeamSetup home = team("H", new CounterAttack(), Stance.NORMAL);
        TeamSetup away = team("A", new CounterAttack(), Stance.NORMAL);

        int goalkeeperShots = 0;
        int allShots = 0;
        for (long seed = 0; seed < 3000; seed++) {
            MatchEngine engine = new MatchEngine(seed);
            for (MatchEvent ev : engine.generateHalfEvents(1, 45, home, away)) {
                if (ev.getEventType() != MatchEvent.EventType.GOAL && ev.getEventType() != MatchEvent.EventType.SAVE) continue;
                if (ev.getPlayer() == null) continue;
                allShots++;
                if (ev.getPlayer().getPosition() == Position.GK) goalkeeperShots++;
            }
        }

        assertTrue(goalkeeperShots > 0, "goalkeepers should sometimes take a shot");
        assertTrue(goalkeeperShots < allShots / 10, "goalkeepers took too many shots: " + goalkeeperShots + " of " + allShots);
    }
}