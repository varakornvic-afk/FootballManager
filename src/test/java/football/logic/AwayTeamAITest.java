package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AwayTeamAITest {

    private static final List<Position> OUTFIELD_POSITIONS = Arrays.stream(Position.values())
            .filter(p -> p != Position.GK)
            .toList();

    // ตัวช่วยสร้างข้อมูลทดสอบ

    /** แบ่ง total ให้ parts ช่องใกล้เคียงกัน (ทำให้ผลรวมสถิติ = 24 พอดี) */
    private static int[] spread(int total, int parts) {
        int[] r = new int[parts];
        for (int i = 0; i < parts; i++) {
            r[i] = total / parts + (i < total % parts ? 1 : 0);
        }
        return r;
    }

    private static Player outfield(String name, int index, int stamina) {
        int[] s = spread(24 - stamina, 5);
        Position pos = OUTFIELD_POSITIONS.get(index % OUTFIELD_POSITIONS.size());
        return new Player(name, pos, s[0], s[1], s[2], stamina, s[3], s[4]);
    }

    private static GoalKeeper keeper(String name, int stamina) {
        int[] s = spread(24 - stamina, 6);
        return new GoalKeeper(name, s[0], s[1], s[2], stamina, s[3], s[4], s[5]);
    }

    /** ผู้เล่นสนาม 10 คน stamina เต็ม 8 */
    private static List<Player> field() {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) list.add(outfield("F" + i, i, 8));
        return list;
    }

    /** ตัวสำรอง 5 คน: GK 1 + ผู้เล่นสนาม 4 (เต็มทุกคน) */
    private static List<Player> bench() {
        List<Player> list = new ArrayList<>();
        list.add(keeper("BenchGK", 8));
        for (int i = 0; i < 4; i++) list.add(outfield("B" + i, i, 8));
        return list;
    }

    private static List<Player> lineup(Player gk, List<Player> field) {
        List<Player> list = new ArrayList<>();
        list.add(gk);
        list.addAll(field);
        return list;
    }

    private static TeamSetup setup(Formation formation, List<Player> starters, List<Player> bench) {
        return TeamSetup.create(new CounterAttack(), formation, Stance.NORMAL, starters, bench);
    }

    private static int subsMade(TeamSetup before, TeamSetup after) {
        return SubstitutionDetector.detect(before.getStarters(), after.getStarters(), false).size();
    }

    // โควตาเปลี่ยนตัว

    @Test
    void halftimeSubsNeverExceedRemainingQuota() {
        List<Player> field = field();
        for (int i = 0; i < 5; i++) field.get(i).setStamina(1); // เหนื่อย 5 คน
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 0, 0, 3);

        assertEquals(3, subsMade(away, result));
    }

    @Test
    void noSubsWhenQuotaIsZero() {
        List<Player> field = field();
        for (int i = 0; i < 5; i++) field.get(i).setStamina(1);
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 0, 0, 0);

        assertEquals(0, subsMade(away, result));
    }

    @Test
    void noSubsWhenNobodyIsTired() {
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field()), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 0, 0, 3);

        assertEquals(0, subsMade(away, result));
    }

    @Test
    void mostTiredPlayerIsSubstitutedFirst() {
        List<Player> field = field();
        field.get(0).setStamina(2);
        field.get(1).setStamina(0); // เหนื่อยสุด
        field.get(2).setStamina(1);
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 0, 0, 1);

        assertFalse(result.getStarters().contains(field.get(1)));
        assertTrue(result.getStarters().contains(field.get(0)));
        assertTrue(result.getStarters().contains(field.get(2)));
    }

    @Test
    void tiredGoalkeeperIsReplacedOnlyByGoalkeeper() {
        GoalKeeper oldGk = keeper("OldGK", 8);
        oldGk.setStamina(1);
        List<Player> bench = bench();
        Player benchGk = bench.get(0);
        TeamSetup away = setup(new Formation442(), lineup(oldGk, field()), bench);

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 0, 0, 3);

        assertTrue(result.getStarters().contains(benchGk));
        assertFalse(result.getStarters().contains(oldGk));
        long keepers = result.getStarters().stream()
                .filter(p -> p.getPosition() == Position.GK).count();
        assertEquals(1, keepers);
    }

    // ปรับแผนตามสกอร์

    @Test
    void losingTeamGoesAllOutAttackWith433() {
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field()), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 2, 0, 3);

        assertEquals(Stance.ALL_OUT_ATTACK, result.getStance());
        assertInstanceOf(Formation433.class, result.getFormation());
    }

    @Test
    void winningTeamParksTheBusWith442() {
        TeamSetup away = setup(new Formation433(), lineup(keeper("GK", 8), field()), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 0, 2, 3);

        assertEquals(Stance.PARK_THE_BUS, result.getStance());
        assertInstanceOf(Formation442.class, result.getFormation());
    }

    @Test
    void drawKeepsFormationAndNormalStance() {
        Formation original = new Formation532();
        TeamSetup away = setup(original, lineup(keeper("GK", 8), field()), bench());

        TeamSetup result = AwayTeamAI.adaptAtHalftime(away, 1, 1, 3);

        assertEquals(Stance.NORMAL, result.getStance());
        assertSame(original, result.getFormation());
    }

    // เปลี่ยนตัวตอนโดนใบเหลือง

    @Test
    void outfieldPlayerIsNotReplacedByGoalkeeper() {
        List<Player> field = field();
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench()); // สำรองคนแรกเป็น GK

        Player replacement = AwayTeamAI.findCardReplacement(away, field.get(0));

        assertNotNull(replacement);
        assertNotEquals(Position.GK, replacement.getPosition());
    }

    @Test
    void noReplacementWhenBenchHasOnlyGoalkeepers() {
        List<Player> allGkBench = new ArrayList<>();
        for (int i = 0; i < 5; i++) allGkBench.add(keeper("BGK" + i, 8));
        List<Player> field = field();
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), allGkBench);

        assertNull(AwayTeamAI.findCardReplacement(away, field.get(0)));
    }

    @Test
    void applySubstitutionSwapsStarterAndBenchPlayer() {
        List<Player> field = field();
        List<Player> bench = bench();
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench);
        Player out = field.get(0);
        Player in = bench.get(1);

        TeamSetup result = AwayTeamAI.applySubstitution(away, out, in);

        assertFalse(result.getStarters().contains(out));
        assertTrue(result.getStarters().contains(in));
        assertFalse(result.getBench().contains(in));
        assertEquals(11, result.getStarters().size());
    }

    @Test
    void noReplacementWhenCardedPlayerIsNoLongerOnPitch() {
        List<Player> field = field();
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench());

        assertNull(AwayTeamAI.findCardReplacement(away, outfield("Ghost", 0, 8)));
    }

    @Test
    void halftimeStillWorksAfterACardSubstitution() {
        List<Player> field = field();
        List<Player> bench = bench();
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench);
        // สำรองเหลือ 4 คนหลังเปลี่ยนตัวตอนโดนใบเหลือง
        TeamSetup afterCardSub = AwayTeamAI.applySubstitution(away, field.get(0), bench.get(1));

        assertDoesNotThrow(() -> AwayTeamAI.adaptAtHalftime(afterCardSub, 0, 1, 2));
    }

    @Test
    void halftimeStillWorksAfterARedCard() {
        List<Player> field = field();
        TeamSetup away = setup(new Formation442(), lineup(keeper("GK", 8), field), bench());
        TeamSetup tenMen = away.withoutPlayer(field.get(0));

        TeamSetup result = assertDoesNotThrow(() -> AwayTeamAI.adaptAtHalftime(tenMen, 2, 0, 3));

        assertEquals(10, result.getStarters().size());
    }
}