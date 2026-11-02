package football.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class MainMenuPanel extends JPanel {
    private static final String BACKGROUND_PATH = "/image/menu_bg.png";

    private final Image background;

    public MainMenuPanel(Runnable onStart) {
        super(new GridBagLayout());
        setBackground(new Color(30, 40, 50));
        background = loadBackground();

        JButton startButton = new JButton("Start Game");
        JButton exitButton = new JButton("Exit");
        startButton.addActionListener(e -> onStart.run());
        exitButton.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;

        if (background == null) {
            // ไม่เจอไฟล์ภาพ: ใช้หน้าตาเดิม (พื้นสีเข้ม + ชื่อเกม + ปุ่ม)
            gbc.insets = new Insets(10, 10, 10, 10);

            JLabel titleLabel = new JLabel("TACTIX FOOTBALL");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
            titleLabel.setForeground(Color.WHITE);
            add(titleLabel, gbc);

            gbc.gridy = 1; add(startButton, gbc);
            gbc.gridy = 2; add(exitButton, gbc);
        } else {
            // มีภาพพื้นหลัง: ดันปุ่มไปไว้ช่วงล่างของหน้าต่าง ไม่บังโลโก้และนักเตะตรงกลาง
            startButton.setPreferredSize(new Dimension(200, 40));
            exitButton.setPreferredSize(new Dimension(200, 40));

            TranslucentPanel buttonPanel = new TranslucentPanel();
            buttonPanel.add(startButton);
            buttonPanel.add(exitButton);

            gbc.gridy = 0;
            gbc.weighty = 1.0;
            gbc.anchor = GridBagConstraints.SOUTH;
            gbc.insets = new Insets(0, 0, 40, 0);
            add(buttonPanel, gbc);
        }
    }

    private static Image loadBackground() {
        try (InputStream in = MainMenuPanel.class.getResourceAsStream(BACKGROUND_PATH)) {
            if (in == null) return null;
            BufferedImage img = ImageIO.read(in);
            return img;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background == null) return;

        int imgW = background.getWidth(null);
        int imgH = background.getHeight(null);
        if (imgW <= 0 || imgH <= 0) return;

        // ขยายให้เต็มพื้นที่ โดยรักษาสัดส่วนภาพ (ส่วนที่เกินจะถูกตัดออกด้านข้างหรือด้านบน-ล่าง)
        double scale = Math.max((double) getWidth() / imgW, (double) getHeight() / imgH);
        int drawW = (int) Math.ceil(imgW * scale);
        int drawH = (int) Math.ceil(imgH * scale);
        int x = (getWidth() - drawW) / 2;
        int y = (getHeight() - drawH) / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(background, x, y, drawW, drawH, null);
        g2.dispose();
    }

    /** แผ่นดำโปร่งใสมุมมน วางหลังปุ่มให้อ่านง่ายบนภาพ */
    private static class TranslucentPanel extends JPanel {
        TranslucentPanel() {
            super(new GridLayout(0, 1, 0, 12));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(18, 40, 18, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}