package football.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamSetupWithoutPlayerTest {

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

    private static TeamSetup team(List<Player> field) {
        List<Player> starters = new ArrayList<>();
        starters.add(keeper("GK"));
        starters.addAll(field);
        List<Player> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) bench.add(outfield("B" + i, i));
        return TeamSetup.create(new CounterAttack(), new Formation442(), Stance.NORMAL, starters, bench);
    }

    private static List<Player> field() {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) list.add(outfield("F" + i, i));
        return list;
    }

    @Test
    void removingAPlayerLeavesTenStartersAndKeepsBench() {
        List<Player> field = field();
        TeamSetup team = team(field);

        TeamSetup tenMen = team.withoutPlayer(field.get(3));

        assertEquals(10, tenMen.getStarters().size());
        assertFalse(tenMen.getStarters().contains(field.get(3)));
        assertEquals(5, tenMen.getBench().size());
    }

    @Test
    void cannotRemovePlayerWhoIsNotOnThePitch() {
        TeamSetup team = team(field());
        assertThrows(IllegalArgumentException.class, () -> team.withoutPlayer(outfield("Ghost", 0)));
    }

    @Test
    void cannotRemoveTheGoalkeeper() {
        TeamSetup team = team(field());
        Player gk = team.getStarters().get(0);
        assertThrows(IllegalArgumentException.class, () -> team.withoutPlayer(gk));
    }
}