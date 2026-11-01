package football.util;

import football.model.GoalKeeper;
import football.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRepositoryTest {

    @Test
    void loadsPlayersFromClasspath() {
        List<Player> players = PlayerRepository.loadPlayersFromCSV("/players.csv");
        assertTrue(players.size() >= 20); // MainFrame ดึงนักเตะมา 20 คนตอน draft
        assertTrue(players.stream().anyMatch(p -> p instanceof GoalKeeper));
        assertTrue(players.stream().noneMatch(p -> p.getName().isBlank()));
    }

    @Test
    void missingResourceFailsLoudly() {
        assertThrows(IllegalStateException.class,
                () -> PlayerRepository.loadPlayersFromCSV("/no-such-file.csv"));
    }
}