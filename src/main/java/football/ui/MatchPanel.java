package football.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

/**
 * หน้าแมตช์ แสดงสกอร์ นาที และ log เหตุการณ์ พร้อมปุ่ม Play Match และ Back to Menu
 */
public class MatchPanel extends BorderPane {
    private static final String PREMATCH_BACKGROUND_PATH = "/image/prematch_bg.png";
    private static final String MATCH_BACKGROUND_PATH = "/image/match_bg.png";
    private static final int PREMATCH_OVERLAY_ALPHA = 0;   // ก่อนเริ่ม: ไม่มีแผ่นดำ (ภาพสว่างเต็มสี)
    private static final int MATCH_OVERLAY_ALPHA = 150;    // ระหว่างแข่ง: เข้มพอให้ตัวหนังสืออ่านง่าย (0-255)

    private final TextFlow matchLogArea = new TextFlow();
    private final Label scoreBoardLabel;
    private final Label matchTimeLabel;
    private final ImageBackgroundPanel logBackground;
    private final Color logTextColor;

    public MatchPanel(Runnable onPlay, Runnable onBack) {
        BorderPane scorePanel = new BorderPane();
        scorePanel.setBackground(new Background(new BackgroundFill(Color.rgb(25, 35, 45), CornerRadii.EMPTY, Insets.EMPTY)));
        scorePanel.setPadding(new Insets(12, 20, 12, 20));

        scoreBoardLabel = new Label("HOME  0 - 0  AWAY");
        scoreBoardLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        scoreBoardLabel.setTextFill(Color.WHITE);
        scoreBoardLabel.setMaxWidth(Double.MAX_VALUE);
        scoreBoardLabel.setAlignment(Pos.CENTER);

        matchTimeLabel = new Label("⏱️ 0'");
        matchTimeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        matchTimeLabel.setTextFill(Color.rgb(255, 215, 0));

        scorePanel.setCenter(scoreBoardLabel);
        scorePanel.setRight(matchTimeLabel);
        setTop(scorePanel);

        // ช่อง log เหตุการณ์ พร้อมภาพพื้นหลัง (ถ้าหาไฟล์ภาพไม่เจอ ใช้พื้นขาว + ตัวหนังสือดำแบบเดิม)
        // เริ่มด้วยภาพระหว่างแข่งเพื่อเช็คว่ามีไฟล์ภาพ แล้วค่อยสลับเป็นภาพก่อนเริ่ม (ถ้าไม่มีไฟล์ ก็ใช้ภาพแข่งไปก่อน)
        logBackground = new ImageBackgroundPanel(MATCH_BACKGROUND_PATH, MATCH_OVERLAY_ALPHA);
        boolean hasBackground = logBackground.hasImage();
        logTextColor = hasBackground ? Color.WHITE : Color.BLACK;
        showPreMatchBackground();

        matchLogArea.setTextAlignment(TextAlignment.CENTER);
        matchLogArea.setPadding(new Insets(8));

        ScrollPane logScroll = new ScrollPane(matchLogArea);
        logScroll.setFitToWidth(true);
        if (hasBackground) {
            logScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        } else {
            logScroll.setStyle("-fx-background: white; -fx-background-color: white;");
        }
        // เลื่อนลงล่างสุดอัตโนมัติเมื่อมีข้อความใหม่
        matchLogArea.heightProperty().addListener((obs, oldH, newH) -> logScroll.setVvalue(1.0));

        logBackground.getContent().setCenter(logScroll);
        setCenter(logBackground);

        Button simButton = new Button("Play Match");
        Button backButton = new Button("Back to Menu");
        HBox controlPanel = new HBox(10, simButton, backButton, new SoundToggleButton());
        controlPanel.setAlignment(Pos.CENTER);
        controlPanel.setPadding(new Insets(10));
        setBottom(controlPanel);

        simButton.setOnAction(e -> onPlay.run());
        backButton.setOnAction(e -> onBack.run());
    }

    /** ล้างข้อความใน log ของแมตช์ */
    public void clearLog() {
        matchLogArea.getChildren().clear();
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

    /** อัปเดตสกอร์และนาทีบนแถบด้านบน */
    public void updateScoreBoard(int homeGoals, int awayGoals, int minute) {
        scoreBoardLabel.setText(String.format("HOME  %d - %d  AWAY", homeGoals, awayGoals));
        matchTimeLabel.setText(String.format("⏱️ %d'", minute));
    }

    /** เพิ่มข้อความหนึ่งบรรทัดลงใน log ของแมตช์ */
    public void appendColoredLog(String text) {
        // แยกอีโมจิกับตัวอักษรเป็นคนละ Text (Text เดียวที่ปนกันอาจทำให้ตัวอักษรหายบน macOS)
        for (EmojiText.Segment seg : EmojiText.split(text)) {
            Text part = new Text(seg.text);
            part.setFill(logTextColor);
            part.setFont(seg.emoji ? Font.font("Apple Color Emoji", 14) : Font.font("Segoe UI", 14));
            matchLogArea.getChildren().add(part);
        }
        matchLogArea.getChildren().add(new Text("\n"));
    }
}