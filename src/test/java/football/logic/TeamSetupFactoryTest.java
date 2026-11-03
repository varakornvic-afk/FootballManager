package football.logic;

import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static football.model.Position.*;
import static org.junit.jupiter.api.Assertions.*;

class TeamSetupFactoryTest {

    private static Player gk(String name) {
        return new GoalKeeper(name, 4, 4, 4, 3, 3, 3, 3);
    }

    private static Player outfield(String name, Position position) {
        return new Player(name, position, 4, 4, 4, 4, 4, 4);
    }

    private static List<Player> starters() {
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

    @Test
    void tacticNameSelectsTheMatchingTactic() {
        assertInstanceOf(Possession.class,
                TeamSetupFactory.createHome("Possession", "4-4-2", starters(), bench()).getTactic());
        assertInstanceOf(WidePlay.class,
                TeamSetupFactory.createHome("Wide Play", "4-4-2", starters(), bench()).getTactic());
        assertInstanceOf(CounterAttack.class,
                TeamSetupFactory.createHome("Counter Attack", "4-4-2", starters(), bench()).getTactic());
    }

    @Test
    void unknownTacticFallsBackToCounterAttack() {
        assertInstanceOf(CounterAttack.class,
                TeamSetupFactory.createHome(null, "4-4-2", starters(), bench()).getTactic());
        assertInstanceOf(CounterAttack.class,
                TeamSetupFactory.createHome("???", "4-4-2", starters(), bench()).getTactic());
    }

    @Test
    void formationNameSelectsTheMatchingFormation() {
        assertInstanceOf(Formation433.class,
                TeamSetupFactory.createHome("Possession", "4-3-3", starters(), bench()).getFormation());
        assertInstanceOf(Formation343.class,
                TeamSetupFactory.createHome("Possession", "3-4-3", starters(), bench()).getFormation());
        assertInstanceOf(Formation532.class,
                TeamSetupFactory.createHome("Possession", "5-3-2", starters(), bench()).getFormation());
        assertInstanceOf(Formation442.class,
                TeamSetupFactory.createHome("Possession", "4-4-2", starters(), bench()).getFormation());
    }

    @Test
    void unknownFormationFallsBackTo442() {
        assertInstanceOf(Formation442.class,
                TeamSetupFactory.createHome("Possession", null, starters(), bench()).getFormation());
    }

    @Test
    void homeTeamStartsWithNormalStanceAndGivenPlayers() {
        List<Player> starters = starters();
        List<Player> bench = bench();

        TeamSetup home = TeamSetupFactory.createHome("Possession", "4-4-2", starters, bench);

        assertEquals(Stance.NORMAL, home.getStance());
        assertEquals(starters, home.getStarters());
        assertEquals(bench, home.getBench());
    }

    @Test
    void randomAwayTeamAlwaysHasOneKeeperElevenStartersAndFiveDistinctSubs() {
        for (int i = 0; i < 200; i++) {
            TeamSetup away = TeamSetupFactory.createRandomAway();

            assertEquals(11, away.getStarters().size());
            assertEquals(5, away.getBench().size());
            assertEquals(1, away.getStarters().stream().filter(p -> p.getPosition() == GK).count());
            assertTrue(away.getStarters().stream().noneMatch(away.getBench()::contains));
        }
    }
}
