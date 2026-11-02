package football.logic;

import football.event.MatchEvent;
import football.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchEngineRedCardTest {

    private static final List<Position> OUTFIELD_POSITIONS = Arrays.stream(Position.values())
            .filter(p -> p != Position.GK)
            .toList();

    private static TeamSetup team(String prefix) {
        List<Player> starters = new ArrayList<>();
        // 4+4+4+4+3+3+2 = 24
        starters.add(new GoalKeeper(prefix + "GK", 4, 4, 4, 4, 3, 3, 2));
        for (int i = 0; i < 10; i++) {
            // 4+4+4+4+4+4 = 24
            starters.add(new Player(prefix + "F" + i,
                    OUTFIELD_POSITIONS.get(i % OUTFIELD_POSITIONS.size()), 4, 4, 4, 4, 4, 4));
        }
        List<Player> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bench.add(new Player(prefix + "B" + i,
                    OUTFIELD_POSITIONS.get(i % OUTFIELD_POSITIONS.size()), 4, 4, 4, 4, 4, 4));
        }
        return TeamSetup.create(new CounterAttack(), new Formation442(), Stance.NORMAL, starters, bench);
    }

    @Test
    void goalkeeperIsNeverSentOff() {
        TeamSetup home = team("H");
        TeamSetup away = team("A");
        int redCards = 0;

        // ลองหลาย seed เพื่อให้มีใบแดงเกิดขึ้นหลายครั้ง
        for (long seed = 0; seed < 3000; seed++) {
            MatchEngine engine = new MatchEngine(seed);
            for (MatchEvent ev : engine.generateHalfEvents(1, 45, home, away)) {
                if (ev.getEventType() == MatchEvent.EventType.RED_CARD) {
                    redCards++;
                    assertNotEquals(Position.GK, ev.getPlayer().getPosition(),
                            "GK got a red card with seed " + seed);
                }
            }
        }
        assertTrue(redCards > 0, "test never produced a red card, so it checks nothing");
    }
}