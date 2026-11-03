package football.event;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEventTest {

    private static final String IMG = "/image/Player/x.jpg";

    @Test
    void goalTitleNamesTheSideOnlyWhenKnown() {
        GameEvent.GoalEvent unknownSide = new GameEvent.GoalEvent("Messi", IMG);
        GameEvent.GoalEvent home = new GameEvent.GoalEvent("Messi", IMG, true);
        GameEvent.GoalEvent away = new GameEvent.GoalEvent("Messi", IMG, false);

        assertTrue(unknownSide.getEventTitle().contains("GOAL"));
        assertFalse(unknownSide.getEventTitle().contains("Your Team"));
        assertTrue(home.getEventTitle().contains("Your Team"));
        assertTrue(away.getEventTitle().contains("Opponent"));
    }

    @Test
    void titleCanBeRequestedForEitherSideExplicitly() {
        GameEvent.GoalEvent goal = new GameEvent.GoalEvent("Messi", IMG);

        assertTrue(goal.getEventTitle(true).contains("Your Team"));
        assertTrue(goal.getEventTitle(false).contains("Opponent"));
    }

    @Test
    void foulTitleAndColourDependOnCardType() {
        GameEvent.FoulEvent yellow = new GameEvent.FoulEvent("A", IMG, false);
        GameEvent.FoulEvent red = new GameEvent.FoulEvent("A", IMG, true);

        assertTrue(yellow.getEventTitle().contains("YELLOW"));
        assertTrue(red.getEventTitle().contains("RED"));
        assertNotEquals(yellow.getThemeColor(), red.getThemeColor());
        assertTrue(new GameEvent.FoulEvent("A", IMG, true, false).getEventTitle(false).contains("Opponent"));
    }

    @Test
    void saveTitleTalksAboutTheKeeper() {
        GameEvent.SaveEvent save = new GameEvent.SaveEvent("Keeper", IMG);

        assertTrue(save.getEventTitle(true).contains("Your Keeper"));
        assertTrue(save.getEventTitle(false).contains("Opponent Keeper"));
    }

    @Test
    void penaltyTitleNamesTheSide() {
        GameEvent.PenaltyEvent penalty = new GameEvent.PenaltyEvent("A", IMG, true);

        assertTrue(penalty.getEventTitle().contains("PENALTY"));
        assertTrue(penalty.getEventTitle().contains("Your Team"));
    }

    @Test
    void substitutionShowsBothPlayersAndUsesTheIncomingPlayersImage() {
        GameEvent.SubstitutionEvent sub = new GameEvent.SubstitutionEvent("Old", "New", IMG);

        assertTrue(sub.getPlayerName().contains("Old"));
        assertTrue(sub.getPlayerName().contains("New"));
        assertEquals(IMG, sub.getImagePath());
        assertTrue(sub.getEventTitle().contains("SUBSTITUTION"));
    }

    @Test
    void everyEventKindHasItsOwnThemeColour() {
        Color goal = new GameEvent.GoalEvent("A", IMG).getThemeColor();
        Color save = new GameEvent.SaveEvent("A", IMG).getThemeColor();
        Color penalty = new GameEvent.PenaltyEvent("A", IMG).getThemeColor();
        Color sub = new GameEvent.SubstitutionEvent("A", "B", IMG).getThemeColor();

        assertEquals(4, java.util.Set.of(goal, save, penalty, sub).size());
    }

    @Test
    void defaultSideAwareTitleFallsBackToThePlainTitle() {
        GameEvent plain = new GameEvent() {
            public String getEventTitle() { return "plain"; }
            public String getPlayerName() { return "p"; }
            public String getImagePath() { return IMG; }
            public Color getThemeColor() { return Color.BLACK; }
        };

        assertEquals("plain", plain.getEventTitle(true));
        assertEquals("plain", plain.getEventTitle(false));
    }

    @Test
    void playerNameAndImageComeFromTheConstructor() {
        GameEvent.GoalEvent goal = new GameEvent.GoalEvent("Messi", IMG);

        assertEquals("Messi", goal.getPlayerName());
        assertEquals(IMG, goal.getImagePath());
    }
}
