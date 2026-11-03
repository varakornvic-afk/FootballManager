package football.event;

import football.event.MatchEvent.EventType;
import football.model.Player;
import football.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchEventTest {

    @Test
    void exposesEverythingItWasCreatedWith() {
        Player player = new Player("Messi", Position.STRIKER, 4, 4, 4, 4, 4, 4);

        MatchEvent event = new MatchEvent(37, "Home", player, EventType.GOAL, "scores");

        assertEquals(37, event.getMinute());
        assertEquals("Home", event.getTeamName());
        assertSame(player, event.getPlayer());
        assertEquals(EventType.GOAL, event.getEventType());
        assertEquals("scores", event.getDescription());
    }

    @Test
    void hasAllSixEventTypes() {
        assertEquals(6, EventType.values().length);
        assertNotNull(EventType.valueOf("YELLOW_CARD"));
        assertNotNull(EventType.valueOf("RED_CARD"));
    }
}
