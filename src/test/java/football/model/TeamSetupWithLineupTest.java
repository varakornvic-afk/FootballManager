package football.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamSetupWithLineupTest {

    private static final List<Position> OUTFIELD_POSITIONS = Arrays.stream(Position.values())
            .filter(p -> p != Position.GK)
            .toList();

    private static Player outfield(String name, int index) {
        // 4+4+4+4+4+4 = 24
        return new Player(name, OUTFIELD_POSITIONS.get(index % OUTFIELD_POSITIONS.size()), 4, 4, 4, 4, 4, 4);
    }

    private static GoalKeeper keeper(String name) {
        // 4+4+4+4+3+3+2 = 24
        return new GoalKeeper(name, 4, 4, 4, 4, 3, 3, 2);
    }

    private static List<Player> starters(int outfieldCount) {
        List<Player> list = new ArrayList<>();
        list.add(keeper("GK"));
        for (int i = 0; i < outfieldCount; i++) list.add(outfield("F" + i, i));
        return list;
    }

    private static List<Player> bench() {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) list.add(outfield("B" + i, i));
        return list;
    }

    private static TeamSetup fullTeam() {
        return TeamSetup.create(new CounterAttack(), new Formation442(), Stance.NORMAL, starters(10), bench());
    }

    @Test
    void acceptsTenStartersAfterARedCard() {
        TeamSetup team = fullTeam();

        TeamSetup result = team.withLineup(new Formation433(), Stance.PARK_THE_BUS, starters(9), bench());

        assertEquals(10, result.getStarters().size());
        assertInstanceOf(Formation433.class, result.getFormation());
        assertEquals(Stance.PARK_THE_BUS, result.getStance());
    }

    @Test
    void keepsTheSameTactic() {
        TeamSetup team = fullTeam();

        TeamSetup result = team.withLineup(new Formation442(), Stance.NORMAL, starters(10), bench());

        assertSame(team.getTactic(), result.getTactic());
    }

    @Test
    void rejectsMoreThanElevenStarters() {
        TeamSetup team = fullTeam();
        assertThrows(IllegalArgumentException.class,
                () -> team.withLineup(new Formation442(), Stance.NORMAL, starters(11), bench()));
    }

    @Test
    void rejectsLineupWithoutExactlyOneGoalkeeper() {
        TeamSetup team = fullTeam();
        List<Player> noKeeper = new ArrayList<>();
        for (int i = 0; i < 10; i++) noKeeper.add(outfield("X" + i, i));

        assertThrows(IllegalArgumentException.class,
                () -> team.withLineup(new Formation442(), Stance.NORMAL, noKeeper, bench()));
    }
}