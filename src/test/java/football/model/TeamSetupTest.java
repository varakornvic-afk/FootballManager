package football.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static football.model.Position.*;
import static org.junit.jupiter.api.Assertions.*;

class TeamSetupTest {

    private static Formation formation442() {
        return new Formation442();
    }

    private static Player gk(String name) {
        return new GoalKeeper(name, 4, 4, 4, 3, 3, 3, 3);
    }

    private static Player outfield(String name, Position position) {
        return new Player(name, position, 4, 4, 4, 4, 4, 4);
    }

    private static List<Player> lineup() {
        return new ArrayList<>(List.of(
                gk("GK1"),
                outfield("CB1", CENTER_BACK), outfield("CB2", CENTER_BACK),
                outfield("FB1", FULLBACK), outfield("FB2", FULLBACK),
                outfield("MF1", MIDFIELD), outfield("MF2", MIDFIELD),
                outfield("MW1", MIDWING), outfield("MW2", MIDWING),
                outfield("ST1", STRIKER), outfield("ST2", STRIKER)));
    }

    private static List<Player> bench() {
        return new ArrayList<>(List.of(
                gk("GK2"), outfield("BCB", CENTER_BACK), outfield("BMF", MIDFIELD),
                outfield("BFW", FRONTWING), outfield("BST", STRIKER)));
    }

    private static TeamSetup setup(List<Player> starters, List<Player> bench) {
        return TeamSetup.create(new CounterAttack(), formation442(), Stance.NORMAL, starters, bench);
    }

    private static Player find(List<Player> players, String name) {
        return players.stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void createRejectsInvalidSquads() {
        // 1. จำนวนตัวจริงไม่ครบ 11 คน (ต้องโยน Exception)
        List<Player> invalidStarters = new ArrayList<>(lineup().subList(0, 10));
        assertThrows(IllegalArgumentException.class, () -> setup(invalidStarters, bench()));

        // 2. จำนวนสำรองไม่ครบ 5 คน (ต้องโยน Exception)
        List<Player> invalidBench = new ArrayList<>(bench().subList(0, 4));
        assertThrows(IllegalArgumentException.class, () -> setup(lineup(), invalidBench));

        // 3. ค่าแผนการเล่น/ตัวผู้เล่นเป็น null (ต้องโยน Exception)
        assertThrows(IllegalArgumentException.class,
                () -> TeamSetup.create(null, formation442(), Stance.NORMAL, lineup(), bench()));
    }

    @Test
    void playersInTheirOwnPositionGetNoPenalty() {
        TeamSetup s = setup(lineup(), bench());
        for (Player p : s.getStarters()) {
            assertEquals(p.getPosition(), s.assignedPosition(p));
            assertEquals(4.0, s.effectiveValue(p, 4), 1e-9);
        }
    }

    @Test
    void outOfPositionPlayerIsPenalisedByDistance() {
        List<Player> l = lineup();
        l.set(10, outfield("FW1", FRONTWING)); // ไม่มีช่อง Frontwing ใน 4-4-2 ต้องไปลง Striker
        TeamSetup s = setup(l, bench());
        Player fw = find(l, "FW1");
        assertEquals(STRIKER, s.assignedPosition(fw));
        assertEquals(3.6, s.effectiveValue(fw, 4), 1e-9); // ระยะ 1 -> x0.9
    }

    @Test
    void parkTheBusPutsBestShooterUpFront() {
        List<Player> l = lineup();
        l.set(10, new Player("ST2", STRIKER, 4, 4, 4, 3, 6, 3));
        TeamSetup s = setup(l, bench()).withStance(Stance.PARK_THE_BUS);
        assertEquals(STRIKER, s.assignedPosition(find(l, "ST2")));
        assertEquals(MIDFIELD, s.assignedPosition(find(l, "ST1")));
    }

    @Test
    void allOutAttackKeepsBestDefenderAsLoneCenterBack() {
        List<Player> l = lineup();
        l.set(2, new Player("CB2", CENTER_BACK, 4, 3, 3, 3, 4, 7));
        TeamSetup s = setup(l, bench()).withStance(Stance.ALL_OUT_ATTACK);
        assertEquals(CENTER_BACK, s.assignedPosition(find(l, "CB2")));
        assertNotEquals(CENTER_BACK, s.assignedPosition(find(l, "CB1")));
    }

    @Test
    void substitutionRulesAreEnforced() {
        List<Player> l = lineup();
        List<Player> b = bench();
        TeamSetup s = setup(l, b);
        Player cb1 = find(l, "CB1");
        Player cb2 = find(l, "CB2");
        Player gk1 = find(l, "GK1");
        Player gk2 = find(b, "GK2");
        Player bcb = find(b, "BCB");
        Player bmf = find(b, "BMF");

        assertThrows(IllegalArgumentException.class, () -> s.withSubstitution(bcb, bmf));
        assertThrows(IllegalArgumentException.class, () -> s.withSubstitution(cb1, cb2));
        assertThrows(IllegalArgumentException.class, () -> s.withSubstitution(gk1, bcb));
        assertThrows(IllegalArgumentException.class, () -> s.withSubstitution(cb1, gk2));
        assertDoesNotThrow(() -> s.withSubstitution(gk1, gk2));
    }

    @Test
    void atMostThreeSubstitutions() {
        List<Player> l = lineup();
        List<Player> b = bench();
        TeamSetup start = setup(l, b);
        TeamSetup s = start
                .withSubstitution(find(l, "CB1"), find(b, "BCB"))
                .withSubstitution(find(l, "MF1"), find(b, "BMF"))
                .withSubstitution(find(l, "ST1"), find(b, "BST"));

        assertEquals(0, start.getSubstitutionsUsed());
        assertEquals(3, s.getSubstitutionsUsed());
        assertEquals(0, s.getRemainingSubstitutions());
        assertTrue(s.getStarters().contains(find(b, "BCB")));
        assertFalse(s.getStarters().contains(find(l, "CB1")));
        assertFalse(s.getBench().contains(find(b, "BCB")));
        assertEquals(2, s.getBench().size());
        assertThrows(IllegalStateException.class,
                () -> s.withSubstitution(find(l, "FB1"), find(b, "BFW")));
    }

    @Test
    void fatigueAppliesAfterFirstHalfAndSubstitutesAreFresh() {
        List<Player> l = lineup();
        List<Player> b = bench();
        TeamSetup half = setup(l, b).endFirstHalf();
        Player cb1 = find(l, "CB1");

        assertEquals(4.0, half.effectiveValue(cb1, 4), 1e-9);

        TeamSetup after = half.withSubstitution(cb1, find(b, "BCB"));
        assertEquals(4.0, after.effectiveValue(find(b, "BCB"), 4), 1e-9);
    }

    @Test
    void lowerStaminaTiresMoreAndFactorsAreOrdered() {
        assertEquals(0.7, TeamSetup.fatigueFactor(1), 1e-9);
        assertTrue(TeamSetup.fatigueFactor(2) < TeamSetup.fatigueFactor(8));
        assertEquals(1.0, TeamSetup.positionFactor(0), 1e-9);
        assertTrue(TeamSetup.positionFactor(1) > TeamSetup.positionFactor(2));
        assertTrue(TeamSetup.positionFactor(2) > TeamSetup.positionFactor(3));
    }

    private static List<Player> paceTeam() {
        List<Player> result = new ArrayList<>();
        for (Player p : lineup()) {
            result.add(p.getPosition() == GK
                    ? new GoalKeeper(p.getName(), 10, 3, 3, 2, 2, 2, 2)
                    : new Player(p.getName(), p.getPosition(), 10, 3, 3, 3, 3, 2));
        }
        return result;
    }

    @Test
    void fitMultiplierIsClampedAndTacticDependent() {
        // ค่าสถิติ 4 ทั้งหมด: Counter Attack ให้ ST (2 คน) ได้ Pace เพิ่มครึ่งหนึ่ง => fit เฉลี่ย (9*4 + 2*6) / 11
        double even = setup(lineup(), bench()).fitMultiplier();
        assertEquals(1.0 + 0.1 * ((48.0 / 11) - 4) / 4, even, 1e-9);
        assertTrue(even > 1.0 && even < 1.02); // ใกล้กลางๆ ยังไม่ถึงขีดจำกัด

        TeamSetup counter = setup(paceTeam(), bench());
        assertEquals(1.1, counter.fitMultiplier(), 1e-9); // fit เฉลี่ยสูงมาก ถูกจำกัดช่วงไว้ที่ 1.1

        // Possession: Passing 3 ทุกคน และ MF (2 คน) ได้เพิ่มครึ่งหนึ่ง (1) => fit เฉลี่ย (11*3 + 2*1) / 11
        TeamSetup possession = TeamSetup.create(new Possession(), formation442(),
                Stance.NORMAL, paceTeam(), bench());
        assertEquals(1.0 + 0.1 * ((35.0 / 11) - 4) / 4, possession.fitMultiplier(), 1e-9);
    }
}