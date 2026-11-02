package football.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class MatchPanel extends JPanel {
    private static final String PREMATCH_BACKGROUND_PATH = "/image/prematch_bg.png";
    private static final String MATCH_BACKGROUND_PATH = "/image/match_bg.png";
    private static final int PREMATCH_OVERLAY_ALPHA = 0;   // ก่อนเริ่ม: ไม่มีแผ่นดำ (ภาพสว่างเต็มสี)
    private static final int MATCH_OVERLAY_ALPHA = 150;    // ระหว่างแข่ง: เข้มพอให้ตัวหนังสืออ่านง่าย (0-255)

    private final JTextPane matchLogArea;
    private final JLabel scoreBoardLabel;
    private final JLabel matchTimeLabel;
    private final ImageBackgroundPanel logBackground;
    private final Color logTextColor;

    public MatchPanel(Runnable onPlay, Runnable onBack) {
        super(new BorderLayout(0, 0));

        JPanel scorePanel = new JPanel(new BorderLayout());
        scorePanel.setBackground(new Color(25, 35, 45));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        scoreBoardLabel = new JLabel("HOME  0 - 0  AWAY", SwingConstants.CENTER);
        scoreBoardLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        scoreBoardLabel.setForeground(Color.WHITE);

        matchTimeLabel = new JLabel("⏱️ 0'", SwingConstants.RIGHT);
        matchTimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        matchTimeLabel.setForeground(new Color(255, 215, 0));

        scorePanel.add(scoreBoardLabel, BorderLayout.CENTER);
        scorePanel.add(matchTimeLabel, BorderLayout.EAST);
        add(scorePanel, BorderLayout.NORTH);

        // ช่อง log เหตุการณ์ พร้อมภาพพื้นหลัง (ถ้าหาไฟล์ภาพไม่เจอ ใช้พื้นขาว + ตัวหนังสือดำแบบเดิม)
        // เริ่มด้วยภาพระหว่างแข่งเพื่อเช็คว่ามีไฟล์ภาพ แล้วค่อยสลับเป็นภาพก่อนเริ่ม (ถ้าไม่มีไฟล์ ก็ใช้ภาพแข่งไปก่อน)
        logBackground = new ImageBackgroundPanel(MATCH_BACKGROUND_PATH, MATCH_OVERLAY_ALPHA, new BorderLayout());
        boolean hasBackground = logBackground.hasImage();
        logTextColor = hasBackground ? Color.WHITE : Color.BLACK;
        showPreMatchBackground();

        matchLogArea = new JTextPane();
        matchLogArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(matchLogArea);
        if (hasBackground) {
            matchLogArea.setOpaque(false);
            logScroll.setOpaque(false);
            logScroll.getViewport().setOpaque(false);
            // กันภาพพื้นหลังเป็นเงาซ้อนตอนเลื่อน log
            logScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
            logScroll.setBorder(BorderFactory.createEmptyBorder());
        }
        logBackground.add(logScroll, BorderLayout.CENTER);
        add(logBackground, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton simButton = new JButton("Play Match");
        JButton backButton = new JButton("Back to Menu");
        controlPanel.add(simButton); controlPanel.add(backButton);
        add(controlPanel, BorderLayout.SOUTH);

        simButton.addActionListener(e -> onPlay.run());
        backButton.addActionListener(e -> onBack.run());
    }

    public void clearLog() {
        matchLogArea.setText("");
    }

    /** ภาพก่อนกด Play Match (log ว่าง จึงแสดงภาพชัดขึ้น) */
    public void showPreMatchBackground() {
        logBackground.setImage(PREMATCH_BACKGROUND_PATH, PREMATCH_OVERLAY_ALPHA);
    }

    /** ภาพระหว่างแข่ง (แผ่นดำเข้มขึ้นให้อ่าน log ได้) */
    public void showMatchBackground() {
        logBackground.setImage(MATCH_BACKGROUND_PATH, MATCH_OVERLAY_ALPHA);
    }

    /** กลับสู่สภาพก่อนเริ่มแข่ง: ล้าง log, รีเซ็ตสกอร์ และใช้ภาพก่อนเริ่ม */
    public void resetToPreMatch() {
        clearLog();
        updateScoreBoard(0, 0, 0);
        showPreMatchBackground();
    }

    public void updateScoreBoard(int homeGoals, int awayGoals, int minute) {
        scoreBoardLabel.setText(String.format("HOME  %d - %d  AWAY", homeGoals, awayGoals));
        matchTimeLabel.setText(String.format("⏱️ %d'", minute));
    }

    public void appendColoredLog(String text) {
        StyledDocument doc = matchLogArea.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();

        StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
        StyleConstants.setForeground(style, logTextColor);
        StyleConstants.setFontFamily(style, "Segoe UI");
        StyleConstants.setFontSize(style, 14);

        try {
            int lengthBefore = doc.getLength();
            doc.insertString(lengthBefore, text + "\n", style);
            doc.setParagraphAttributes(lengthBefore, text.length() + 1, style, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}