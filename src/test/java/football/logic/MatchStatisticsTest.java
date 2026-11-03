package football.logic;

import football.event.MatchEvent;
import football.event.MatchEvent.EventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchStatisticsTest {

    private static MatchEvent event(String team, EventType type, String description) {
        return new MatchEvent(10, team, null, type, description);
    }

    @Test
    void noEventsGivesZerosAndEvenPossession() {
        MatchStatistics stats = MatchStatistics.calculate(List.of());

        assertEquals(0, stats.getHomeShots());
        assertEquals(0, stats.getAwayShots());
        assertEquals(0, stats.getHomeShotsOnTarget());
        assertEquals(0, stats.getAwayShotsOnTarget());
        assertEquals(50, stats.getHomePossessionPercent());
        assertEquals(50, stats.getAwayPossessionPercent());
    }

    @Test
    void nullEventListIsTreatedAsEmpty() {
        MatchStatistics stats = MatchStatistics.calculate(null);

        assertEquals(50, stats.getHomePossessionPercent());
        assertEquals(0, stats.getHomeShots());
    }

    @Test
    void goalsSavesAndPenaltiesCountAsShotsOnTarget() {
        List<MatchEvent> events = List.of(
                event("Home", EventType.GOAL, ""),
                event("Home", EventType.SAVE, ""),
                event("Away", EventType.PENALTY, ""));

        MatchStatistics stats = MatchStatistics.calculate(events);

        assertEquals(2, stats.getHomeShots());
        assertEquals(2, stats.getHomeShotsOnTarget());
        assertEquals(1, stats.getAwayShots());
        assertEquals(1, stats.getAwayShotsOnTarget());
    }

    @Test
    void scoringChanceTextCountsAsShotButNotOnTarget() {
        MatchStatistics stats = MatchStatistics.calculate(List.of(
                event("Home", EventType.FOUL, "Messi creates a clear scoring chance"),
                event("Away", EventType.FOUL, "Haaland builds up an attack")));

        assertEquals(1, stats.getHomeShots());
        assertEquals(0, stats.getHomeShotsOnTarget());
        assertEquals(1, stats.getAwayShots());
        assertEquals(0, stats.getAwayShotsOnTarget());
    }

    @Test
    void foulsWithoutAttackTextAreNotShots() {
        MatchStatistics stats = MatchStatistics.calculate(List.of(
                event("Home", EventType.FOUL, "late tackle")));

        assertEquals(0, stats.getHomeShots());
        assertEquals(50, stats.getHomePossessionPercent());
    }

    @Test
    void cardsAreCountedPerTeam() {
        MatchStatistics stats = MatchStatistics.calculate(List.of(
                event("Home", EventType.YELLOW_CARD, ""),
                event("Home", EventType.YELLOW_CARD, ""),
                event("Away", EventType.YELLOW_CARD, ""),
                event("Home", EventType.RED_CARD, ""),
                event("Away", EventType.RED_CARD, "")));

        assertEquals(2, stats.getHomeYellowCards());
        assertEquals(1, stats.getAwayYellowCards());
        assertEquals(1, stats.getHomeRedCards());
        assertEquals(1, stats.getAwayRedCards());
    }

    @Test
    void possessionFollowsShareOfAttacksAndAddsUpToHundred() {
        MatchStatistics stats = MatchStatistics.calculate(List.of(
                event("Home", EventType.GOAL, ""),
                event("Home", EventType.SAVE, ""),
                event("Home", EventType.SAVE, ""),
                event("Away", EventType.GOAL, "")));

        assertEquals(75, stats.getHomePossessionPercent());
        assertEquals(25, stats.getAwayPossessionPercent());
    }

    @Test
    void teamNameIsCaseInsensitiveAndUnknownNamesCountAsAway() {
        MatchStatistics stats = MatchStatistics.calculate(List.of(
                event("home", EventType.GOAL, ""),
                event(null, EventType.GOAL, "")));

        assertEquals(1, stats.getHomeShots());
        assertEquals(1, stats.getAwayShots());
    }
}
