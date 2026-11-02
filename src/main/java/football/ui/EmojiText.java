package football.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ข้อความที่มีอีโมจิปนกับตัวอักษร
 * แยกเป็นหลาย JLabel (ตัวอักษรใช้ฟอนต์ที่กำหนด / อีโมจิใช้ฟอนต์ Dialog)
 * เพราะ JLabel เดียวที่ผสมอีโมจิกับตัวอักษรอาจวาดตัวอักษรหายบางเครื่อง (เช่น macOS)
 */
public class EmojiText extends JPanel {

    public EmojiText(String text, Font textFont, Color color) {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        setOpaque(false);
        setAlignmentX(Component.CENTER_ALIGNMENT);

        Font emojiFont = new Font(Font.DIALOG, Font.PLAIN, textFont.getSize());

        for (Segment seg : split(text == null ? "" : text)) {
            JLabel label = new JLabel(seg.text);
            label.setFont(seg.emoji ? emojiFont : textFont);
            label.setForeground(color);
            add(label);
        }

        // กันไม่ให้ถูกยืดสูงใน BoxLayout แนวตั้ง
        setMaximumSize(new Dimension(Short.MAX_VALUE, getPreferredSize().height));
    }

    private static class Segment {
        final String text;
        final boolean emoji;

        Segment(String text, boolean emoji) {
            this.text = text;
            this.emoji = emoji;
        }
    }

    private static List<Segment> split(String text) {
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
        return cp >= 0x1F000                      // 🧤 🟥 🟨 🎯 🛑 🔄 🔴 🟢 🚪
                || (cp >= 0x2600 && cp <= 0x27BF) // ⚽ ➡
                || (cp >= 0x2B00 && cp <= 0x2BFF)
                || cp == 0xFE0F                   // variation selector (ติดกับอีโมจิตัวก่อนหน้า)
                || cp == 0x200D;                  // zero width joiner
    }
}