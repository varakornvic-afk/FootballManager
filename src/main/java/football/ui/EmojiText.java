package football.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * ข้อความที่มีอีโมจิปนกับตัวอักษร
 * แยกเป็นหลาย Label (ตัวอักษรใช้ฟอนต์ที่กำหนด / อีโมจิใช้ฟอนต์ระบบ)
 * เพื่อไม่ให้ตัวอักษรหายเวลาปนกับอีโมจิบางเครื่อง (เช่น macOS)
 */
public class EmojiText extends HBox {

    public EmojiText(String text, Font textFont, Color color) {
        super(0);
        setAlignment(Pos.CENTER);

        Font emojiFont = Font.font(textFont.getSize());

        for (Segment seg : split(text == null ? "" : text)) {
            Label label = new Label(seg.text);
            label.setFont(seg.emoji ? emojiFont : textFont);
            label.setTextFill(color);
            getChildren().add(label);
        }
    }

    /** ส่วนของข้อความ: อีโมจิหรือตัวอักษร (ใช้ร่วมกับ MatchPanel) */
    static class Segment {
        final String text;
        final boolean emoji;

        Segment(String text, boolean emoji) {
            this.text = text;
            this.emoji = emoji;
        }
    }

    static List<Segment> split(String text) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean currentIsEmoji = false;

        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            boolean isEmoji = isEmojiCodePoint(cp);

            if (current.length() > 0 && isEmoji != currentIsEmoji) {
                segments.add(new Segment(current.toString(), currentIsEmoji));
                current.setLength(0);
            }
            currentIsEmoji = isEmoji;
            current.appendCodePoint(cp);
        }
        if (current.length() > 0) {
            segments.add(new Segment(current.toString(), currentIsEmoji));
        }
        return segments;
    }

    private static boolean isEmojiCodePoint(int cp) {
        return cp >= 0x1F000
                || (cp >= 0x2600 && cp <= 0x27BF)
                || (cp >= 0x2B00 && cp <= 0x2BFF)
                || cp == 0xFE0F                   // variation selector (ติดกับอีโมจิตัวก่อนหน้า)
                || cp == 0x200D;                  // zero width joiner
    }
}