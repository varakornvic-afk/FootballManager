package football.logic;

import football.event.MatchEvent;
import football.model.Player;
import football.model.Position;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class StaminaManagerTest {

    private static Player player(String name) {
        Position outfield = Arrays.stream(Position.values())
                .filter(p -> p != Position.GK)
                .findFirst().orElseThrow();
        // 4+4+4+5+4+3 = 24 -> stamina เริ่มต้น 5
        return new Player(name, outfield, 4, 4, 4, 5, 4, 3);
    }

    @Test
    void playerInEventLosesOneStamina() {
        Player p = player("A");
        MatchEvent ev = new MatchEvent(10, "Home", p, MatchEvent.EventType.FOUL, "Foul");

        StaminaManager.updatePlayersStamina(ev, null, null);

        assertEquals(4, p.getStamina());
    }

    @Test
    void staminaNeverGoesBelowZero() {
        Player p = player("A");
        p.setStamina(0);
        MatchEvent ev = new MatchEvent(10, "Home", p, MatchEvent.EventType.FOUL, "Foul");

        StaminaManager.updatePlayersStamina(ev, null, null);

        assertEquals(0, p.getStamina());
    }

    @Test
    void eventWithoutPlayerDoesNotCrash() {
        MatchEvent ev = new MatchEvent(10, "Home", null, MatchEvent.EventType.FOUL, "Foul");
        assertDoesNotThrow(() -> StaminaManager.updatePlayersStamina(ev, null, null));
    }
}