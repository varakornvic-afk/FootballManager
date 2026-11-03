package football.logic;

import football.event.MatchEvent;
import football.event.MatchEvent.EventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchHalfResultTest {

    private static MatchEvent goal() {
        return new MatchEvent(5, "Home", null, EventType.GOAL, "goal");
    }

    @Test
    void storesGoalsAndEvents() {
        MatchEvent goal = goal();
        MatchHalfResult result = new MatchHalfResult(2, List.of(goal));

        assertEquals(2, result.getHomeGoals());
        assertEquals(List.of(goal), result.getEvents());
    }

    @Test
    void nullEventsBecomeAnEmptyList() {
        MatchHalfResult result = new MatchHalfResult(0, null);

        assertTrue(result.getEvents().isEmpty());
    }

    @Test
    void eventsAreCopiedSoLaterChangesDoNotLeakIn() {
        List<MatchEvent> source = new ArrayList<>();
        source.add(goal());
        MatchHalfResult result = new MatchHalfResult(1, source);

        source.add(goal());

        assertEquals(1, result.getEvents().size());
    }

    @Test
    void returnedEventListCannotBeModified() {
        MatchHalfResult result = new MatchHalfResult(1, List.of(goal()));

        assertThrows(UnsupportedOperationException.class, () -> result.getEvents().add(goal()));
    }
}
