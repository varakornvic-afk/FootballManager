package football.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * คลาสหลักของแอป JavaFX สร้างหน้าต่างเกมและสลับหน้าจอระหว่างเมนู จัดทีม และแมตช์
 */
public class MainFrame extends Application {
    private final StackPane cardPanel = new StackPane();

    private MainMenuPanel mainMenuPanel;
    private SquadPanel squadPanel;
    private MatchPanel matchPanel;
    private MatchController matchController;

    /** เริ่มแอป สร้างหน้าจอทั้งหมดและแสดงหน้าเมนู */
    @Override
    public void start(Stage stage) {
        stage.setTitle("FootballTactix");
        stage.setOnCloseRequest(e -> Platform.exit());

        matchPanel = new MatchPanel(
                () -> matchController.runMatchSimulation(),
                () -> {
                    matchController.stop();
                    showCard(mainMenuPanel);
                });
        matchController = new MatchController(stage, matchPanel);

        squadPanel = new SquadPanel((home, away) -> {
            matchController.setTeams(home, away);
            showCard(matchPanel);
        });

        mainMenuPanel = new MainMenuPanel(() -> {
            squadPanel.startNewDraftSession();
            showCard(squadPanel);
        });

        showCard(mainMenuPanel);

        stage.setScene(new Scene(cardPanel, 1280, 720));
        stage.centerOnScreen();
        stage.show();
    }

    /** แสดงหน้าที่ต้องการ (แทน CardLayout.show) */
    private void showCard(Node card) {
        cardPanel.getChildren().setAll(card);
        // กลับมาหน้าเมนูแล้วเพลงเริ่มใหม่ ส่วนหน้าเลือกทีมและหน้าแข่งก่อนเริ่มเพลงเล่นต่อเนื่อง
        // (เพลงหยุดตอนกด Play Match ใน MatchController)
        if (card == mainMenuPanel) SoundManager.playMenuMusic();
    }
}