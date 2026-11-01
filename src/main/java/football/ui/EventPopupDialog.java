package football.ui;

import football.event.GameEvent;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class EventPopupDialog extends JDialog {

    public EventPopupDialog(JFrame parent, GameEvent ev, boolean isHomeTeam) {
        super(parent, false);
        setUndecorated(true);

        // กำหนดสีพื้นหลังและข้อความระบุฝั่ง
        Color bgColor = isHomeTeam ? new Color(15, 35, 60) : new Color(60, 20, 20); // ฟ้าเข้ม vs แดงเข้ม
        String teamPrefix = isHomeTeam ? "Your Player " : "Opponent Player ";

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bgColor);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ev.getThemeColor(), 4),
                BorderFactory.createEmptyBorder(20, 35, 20, 35)
        ));

        // 1. หัวข้อ Event (เช่น Your Player GOAL! ⚽ หรือ Opponent Player RED CARD! 🟥)
        JLabel lblTitle = new JLabel(teamPrefix + ev.getEventTitle(), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(ev.getThemeColor());
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. รูปนักเตะ (ขนาด 220x270px)
        JLabel lblImg = new JLabel();
        lblImg.setAlignmentX(Component.CENTER_ALIGNMENT);
        File file = new File(ev.getImagePath());
        if (!file.exists()) file = new File("src/main/resources/" + ev.getImagePath());
        if (file.exists()) {
            Image img = new ImageIcon(file.getAbsolutePath()).getImage().getScaledInstance(220, 270, Image.SCALE_SMOOTH);
            lblImg.setIcon(new ImageIcon(img));
        }

        // 3. ชื่อนักเตะ
        JLabel lblName = new JLabel(ev.getPlayerName(), SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(lblTitle); p.add(Box.createVerticalStrut(12));
        p.add(lblImg);   p.add(Box.createVerticalStrut(12));
        p.add(lblName);

        add(p);
        pack();
        setLocationRelativeTo(parent);

        // 🔥 เพิ่มเวลาค้างแสดงผลเป็น 3.5 วินาที (3500 ms)
        new Timer(3500, e -> dispose()).start();
    }

    public static void showPopup(JFrame parent, GameEvent ev, boolean isHomeTeam) {
        new EventPopupDialog(parent, ev, isHomeTeam).setVisible(true);
    }
}