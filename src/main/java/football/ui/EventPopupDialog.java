package football.ui;

import football.event.GameEvent;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class EventPopupDialog extends JDialog {
    private static final int IMG_BOX_W = 220;
    private static final int IMG_BOX_H = 270;

    public EventPopupDialog(JFrame parent, GameEvent ev, boolean isHomeTeam) {
        super(parent, false);
        setUndecorated(true);

        // กำหนดสีพื้นหลังและข้อความระบุฝั่ง
        Color bgColor = isHomeTeam ? new Color(15, 35, 60) : new Color(60, 20, 20); // ฟ้าเข้ม vs แดงเข้ม

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bgColor);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ev.getThemeColor(), 4),
                BorderFactory.createEmptyBorder(20, 35, 20, 35)
        ));

        // 1. หัวข้อ Event (event สร้างข้อความที่มีฝั่งทีมมาให้แล้ว เช่น ⚽ GOAL! Your Team)
        EmojiText lblTitle = new EmojiText(ev.getEventTitle(isHomeTeam),
                new Font("Segoe UI", Font.BOLD, 24), ev.getThemeColor());

        // 2. รูปนักเตะ (กรอบ 220x270px แต่รักษาสัดส่วนภาพ ไม่ยืด)
        JLabel lblImg = new JLabel();
        lblImg.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblImg.setHorizontalAlignment(SwingConstants.CENTER);
        lblImg.setVerticalAlignment(SwingConstants.CENTER);
        Dimension imgBox = new Dimension(IMG_BOX_W, IMG_BOX_H);
        lblImg.setPreferredSize(imgBox);
        lblImg.setMinimumSize(imgBox);
        lblImg.setMaximumSize(imgBox);

        String imgPath = ev.getImagePath();
        if (imgPath != null) {
            // ตัดส่วนเกินกรณีติด path แบบเดิมมา
            if (imgPath.startsWith("src/main/resources")) {
                imgPath = imgPath.replace("src/main/resources", "");
            }
            if (!imgPath.startsWith("/")) {
                imgPath = "/" + imgPath;
            }

            // ดึงไฟล์รูปจากในไฟล์ JAR ผ่าน Classpath
            URL imgUrl = getClass().getResource(imgPath);
            if (imgUrl != null) {
                ImageIcon original = new ImageIcon(imgUrl);
                int w = original.getIconWidth();
                int h = original.getIconHeight();
                if (w > 0 && h > 0) {
                    // ย่อ/ขยายให้พอดีกรอบ โดยคูณทั้งกว้างและสูงด้วยอัตราเดียวกัน
                    double scale = Math.min((double) IMG_BOX_W / w, (double) IMG_BOX_H / h);
                    int drawW = Math.max(1, (int) Math.round(w * scale));
                    int drawH = Math.max(1, (int) Math.round(h * scale));
                    Image img = original.getImage().getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
                    lblImg.setIcon(new ImageIcon(img));
                }
            }
        }

        // 3. ชื่อนักเตะ
        EmojiText lblName = new EmojiText(ev.getPlayerName(),
                new Font("Segoe UI", Font.BOLD, 22), Color.WHITE);

        p.add(lblTitle); p.add(Box.createVerticalStrut(12));
        p.add(lblImg);   p.add(Box.createVerticalStrut(12));
        p.add(lblName);

        add(p);
        pack();
        setLocationRelativeTo(parent);

        // 🔥 เพิ่มเวลาค้างแสดงผลเป็น 3.5 วินาที (3500 ms) และปิดการทำซ้ำ (repeats = false)
        Timer timer = new Timer(3500, e -> dispose());
        timer.setRepeats(false); // 📌 ป้องกัน Timer ทำงานซ้ำซ้อน
        timer.start();
    }

    public static void showPopup(JFrame parent, GameEvent ev, boolean isHomeTeam) {
        new EventPopupDialog(parent, ev, isHomeTeam).setVisible(true);
    }
}