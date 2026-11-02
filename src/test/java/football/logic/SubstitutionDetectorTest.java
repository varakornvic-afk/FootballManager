package football.logic;

import football.model.Player;
import football.model.Position;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubstitutionDetectorTest {

    private static Player player(String name) {
        Position outfield = Arrays.stream(Position.values())
                .filter(p -> p != Position.GK)
                .findFirst().orElseThrow();
        // 4+4+4+5+4+3 = 24
        return new Player(name, outfield, 4, 4, 4, 5, 4, 3);
    }

    @Test
    void noSubstitutionReturnsEmptyList() {
        Player a = player("A"), b = player("B");
        List<SubInfo> subs = SubstitutionDetector.detect(List.of(a, b), List.of(a, b), true);
        assertTrue(subs.isEmpty());
    }

    @Test
    void oneSwapPairsOutAndIn() {
        Player a = player("A"), b = player("B"), c = player("C");
        List<SubInfo> subs = SubstitutionDetector.detect(List.of(a, b), List.of(a, c), true);

        assertEquals(1, subs.size());
        assertEquals("B", subs.get(0).playerOut);
        assertEquals("C", subs.get(0).playerIn);
        assertTrue(subs.get(0).isHome);
    }

    @Test
    void awayFlagIsKept() {
        Player a = player("A"), b = player("B");
        List<SubInfo> subs = SubstitutionDetector.detect(List.of(a), List.of(b), false);

        assertEquals(1, subs.size());
        assertFalse(subs.get(0).isHome);
    }

    @Test
    void unevenOutsAndInsCountOnlyThePairs() {
        // ออก 2 คน (B, C) แต่เข้า 1 คน (D) -> จับคู่ได้ 1 คู่ (เช่น กรณีโดนใบแดง)
        Player a = player("A"), b = player("B"), c = player("C"), d = player("D");
        List<SubInfo> subs = SubstitutionDetector.detect(List.of(a, b, c), List.of(a, d), true);

        assertEquals(1, subs.size());
        assertEquals("D", subs.get(0).playerIn);
    }
}