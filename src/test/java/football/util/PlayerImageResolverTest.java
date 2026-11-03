package football.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerImageResolverTest {

    @Test
    void nullOrBlankNameGivesTheDefaultPicture() {
        assertEquals("/image/Player/Haalond.jpg", PlayerImageResolver.getPlayerImagePath(null));
        assertEquals("/image/Player/Haalond.jpg", PlayerImageResolver.getPlayerImagePath("   "));
    }

    @Test
    void knownPlayerGetsTheirOwnPicture() {
        assertEquals("/image/Player/Albar.jpg", PlayerImageResolver.getPlayerImagePath("Albar"));
    }

    @Test
    void nameIsTrimmedBeforeLookingUpTheFile() {
        assertEquals("/image/Player/Albar.jpg", PlayerImageResolver.getPlayerImagePath("  Albar "));
    }

    @Test
    void unknownPlayerFallsBackToTheDefaultPicture() {
        assertEquals("/image/Player/Haalond.jpg", PlayerImageResolver.getPlayerImagePath("NoSuchPlayer"));
    }
}
