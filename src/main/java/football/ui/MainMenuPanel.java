package football.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.io.InputStream;

/**
 * หน้าเมนูหลัก มีปุ่ม Start Game, Exit และปุ่มเปิดปิดเสียง
 * ใช้ภาพพื้นหลัง ถ้าโหลดภาพไม่ได้จะใช้สีพื้นแทน
 */
public class MainMenuPanel extends StackPane {
    private static final String BACKGROUND_PATH = "/image/menu_bg.png";

    public MainMenuPanel(Runnable onStart) {
        Color baseColor = Color.rgb(30, 40, 50);
        Image background = loadBackground();

        Button startButton = new Button("Start Game");
        Button exitButton = new Button("Exit");
        startButton.setOnAction(e -> onStart.run());
        exitButton.setOnAction(e -> Platform.exit());

        if (background == null) {
            // ไม่เจอไฟล์ภาพ: ใช้หน้าตาเดิม (พื้นสีเข้ม + ชื่อเกม + ปุ่ม)
            setBackground(new Background(new BackgroundFill(baseColor, CornerRadii.EMPTY, Insets.EMPTY)));

            Label titleLabel = new Label("FOOTBALLTACTIX");
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            titleLabel.setTextFill(Color.WHITE);

            VBox box = new VBox(10, titleLabel, startButton, exitButton);
            box.setAlignment(Pos.CENTER);
            getChildren().add(box);
            addSoundButton();
        } else {
            // ขยายภาพให้เต็มพื้นที่ โดยรักษาสัดส่วน (ส่วนที่เกินจะถูกตัดออก)
            BackgroundImage bgImage = new BackgroundImage(background,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(1.0, 1.0, true, true, false, true));
            setBackground(new Background(
                    new BackgroundFill[]{new BackgroundFill(baseColor, CornerRadii.EMPTY, Insets.EMPTY)},
                    new BackgroundImage[]{bgImage}));

            // มีภาพพื้นหลัง: ดันปุ่มไปไว้ช่วงล่างของหน้าต่าง ไม่บังโลโก้และนักเตะตรงกลาง
            startButton.setPrefSize(200, 40);
            exitButton.setPrefSize(200, 40);

            // แผ่นดำโปร่งใสมุมมน วางหลังปุ่มให้อ่านง่ายบนภาพ
            VBox buttonPanel = new VBox(12, startButton, exitButton);
            buttonPanel.setAlignment(Pos.CENTER);
            buttonPanel.setPadding(new Insets(18, 40, 18, 40));
            buttonPanel.setBackground(new Background(new BackgroundFill(
                    Color.rgb(0, 0, 0, 150 / 255.0), new CornerRadii(18), Insets.EMPTY)));
            buttonPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

            StackPane.setAlignment(buttonPanel, Pos.BOTTOM_CENTER);
            StackPane.setMargin(buttonPanel, new Insets(0, 0, 40, 0));
            getChildren().add(buttonPanel);
            addSoundButton();
        }
    }

    /** ปุ่มเปิด/ปิดเสียงมุมขวาบน */
    private void addSoundButton() {
        SoundToggleButton soundButton = new SoundToggleButton();
        StackPane.setAlignment(soundButton, Pos.TOP_RIGHT);
        StackPane.setMargin(soundButton, new Insets(15));
        getChildren().add(soundButton);
    }

    private static Image loadBackground() {
        try (InputStream in = MainMenuPanel.class.getResourceAsStream(BACKGROUND_PATH)) {
            if (in == null) return null;
            Image img = new Image(in);
            return img.isError() ? null : img;
        } catch (IOException e) {
            return null;
        }
    }
}