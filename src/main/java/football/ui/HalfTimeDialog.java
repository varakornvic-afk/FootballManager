package football.ui;

import football.model.*;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HalfTimeDialog extends Stage {
    private TeamSetup updatedTeamSetup;
    private boolean confirmed = false;

    // บันทึกจำนวนโควตาเปลี่ยนตัวที่ใช้ไปแล้ว
    private int currentSubsUsed;
    private int newSubsMadeInSession = 0;

    // บันทึกรายชื่อนักเตะที่ถูกถอดออกจากสนามแล้ว + คนที่โดนใบแดง (ห้ามเลือก)
    private final Set<Player> subbedOutPlayers = new HashSet<>();
    private final TitledPane subPanel;

    public HalfTimeDialog(Stage parent, TeamSetup currentSetup, TeamSetup awaySetup,
                          Set<String> yellowCardPlayers, Set<Player> redCardedPlayers, int currentSubsUsed) {
        initOwner(parent);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Half-Time Tactics & Substitutions");
        this.currentSubsUsed = currentSubsUsed;

        if (redCardedPlayers != null) {
            this.subbedOutPlayers.addAll(redCardedPlayers);
        }

        VBox contentPanel = new VBox(8);
        contentPanel.setPadding(new Insets(15));

        // 0. Opponent Scout Report
        String awayTacticName = awaySetup != null && awaySetup.getTactic() != null ?
                awaySetup.getTactic().getClass().getSimpleName() : "Unknown";
        String awayFormName = awaySetup != null && awaySetup.getFormation() != null ?
                awaySetup.getFormation().getClass().getSimpleName().replace("Formation", "") : "Unknown";
        Stance awayStance = awaySetup != null ? awaySetup.getStance() : Stance.NORMAL;

        TextFlow scoutLabel = new TextFlow(
                boldText("Tactic (Fixed): "), blueText(awayTacticName), plainText(" | "),
                boldText("Formation: "), blueText(awayFormName), plainText(" | "),
                boldText("Stance: "), blueText(String.valueOf(awayStance)));
        TitledPane scoutPanel = new TitledPane("🔍 Opponent Scout Report (ข้อมูลคู่แข่ง)", scoutLabel);
        scoutPanel.setCollapsible(false);

        // 1. Tactic (Fixed) & Formation ฝั่งเรา
        String homeTacticName = currentSetup != null && currentSetup.getTactic() != null ?
                currentSetup.getTactic().getClass().getSimpleName() : "Standard";

        Label tacticFixedLabel = new Label("Tactic (Fixed): 🔒 " + homeTacticName + "    ");
        tacticFixedLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        ComboBox<String> formCombo = new ComboBox<>(FXCollections.observableArrayList("4-4-2", "4-3-3", "3-4-3", "5-3-2"));
        formCombo.getSelectionModel().selectFirst();
        if (currentSetup != null && currentSetup.getFormation() != null) {
            String currentFormStr = currentSetup.getFormation().getClass().getSimpleName().replace("Formation", "");
            if (currentFormStr.length() >= 3) {
                formCombo.setValue(currentFormStr.substring(0, 1) + "-" + currentFormStr.substring(1, 2) + "-" + currentFormStr.substring(2));
            }
        }
        HBox tacticPanel = new HBox(5, tacticFixedLabel, new Label("Formation: "), formCombo);
        tacticPanel.setAlignment(Pos.CENTER_LEFT);

        // 2. Team Stance ฝั่งเรา
        ComboBox<Stance> stanceCombo = new ComboBox<>(FXCollections.observableArrayList(Stance.values()));
        stanceCombo.getSelectionModel().selectFirst();
        if (currentSetup != null && currentSetup.getStance() != null) {
            stanceCombo.setValue(currentSetup.getStance());
        }
        HBox stancePanel = new HBox(5, new Label("Team Stance: "), stanceCombo);
        stancePanel.setAlignment(Pos.CENTER_LEFT);

        // 3. Substitutions List
        List<Player> starters = new ArrayList<>(currentSetup.getStarters());
        List<Player> bench = new ArrayList<>(currentSetup.getBench());

        ComboBox<Player> starterCombo = new ComboBox<>();
        ComboBox<Player> benchCombo = new ComboBox<>();
        starterCombo.setMaxWidth(Double.MAX_VALUE);
        benchCombo.setMaxWidth(Double.MAX_VALUE);

        updateComboBoxModels(starterCombo, benchCombo, starters, bench, yellowCardPlayers);

        GridPane subGrid = new GridPane();
        subGrid.setHgap(5);
        subGrid.setVgap(5);
        subGrid.add(new Label("Out (Starter):"), 0, 0);
        subGrid.add(starterCombo, 1, 0);
        subGrid.add(new Label("In (Bench):"), 0, 1);
        subGrid.add(benchCombo, 1, 1);
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        subGrid.getColumnConstraints().addAll(labelCol, fieldCol);

        subPanel = new TitledPane(getPanelSubTitle(starters.size()), subGrid);
        subPanel.setCollapsible(false);

        // 4. Action Buttons
        Button swapBtn = new Button("🔄 Swap Player");
        Button recommendBtn = new Button("💡 Smart Recommend");
        HBox btnActionPanel = new HBox(10, swapBtn, recommendBtn);
        btnActionPanel.setAlignment(Pos.CENTER);

        // ปุ่มสลับตัว (ดักจับโควตาไม่เกิน 3 คน)
        swapBtn.setOnAction(ev -> {
            // เช็กว่าโควตา 3 คนเต็มหรือยัง
            if (this.currentSubsUsed + newSubsMadeInSession >= 3) {
                showMessage(Alert.AlertType.WARNING, "Substitutions Limit Reached",
                        "คุณใช้โควตาเปลี่ยนตัวครบ 3 คนแล้ว! ไม่สามารถเปลี่ยนเพิ่มได้อีก");
                return;
            }

            Player pOut = starterCombo.getValue();
            Player pIn = benchCombo.getValue();

            if (pOut == null || pIn == null) return;

            if (subbedOutPlayers.contains(pIn)) {
                showMessage(Alert.AlertType.WARNING, "Substitution Restriction",
                        "นักเตะ " + pIn.getName() + " ไม่สามารถเปลี่ยนลงสนามได้ (ถูกถอดออกแล้ว หรือ โดนใบแดง)!");
                return;
            }

            // ผู้รักษาประตูเปลี่ยนได้เฉพาะกับผู้รักษาประตูด้วยกัน (ทีมต้องมี GK 1 คนเสมอ)
            if ((pOut.getPosition() == Position.GK) != (pIn.getPosition() == Position.GK)) {
                showMessage(Alert.AlertType.WARNING, "Substitution Restriction",
                        "ผู้รักษาประตูเปลี่ยนได้เฉพาะกับผู้รักษาประตูด้วยกันเท่านั้น!");
                return;
            }

            int idxOut = starters.indexOf(pOut);
            int idxIn = bench.indexOf(pIn);

            if (idxOut >= 0 && idxIn >= 0) {
                starters.set(idxOut, pIn);
                bench.set(idxIn, pOut);
                subbedOutPlayers.add(pOut);
                newSubsMadeInSession++;

                subPanel.setText(getPanelSubTitle(starters.size()));

                updateComboBoxModels(starterCombo, benchCombo, starters, bench, yellowCardPlayers);

                int remaining = 3 - (this.currentSubsUsed + newSubsMadeInSession);
                showMessage(Alert.AlertType.INFORMATION, "Message",
                        "สลับตัวเรียบร้อย: " + pOut.getName() + " (ออก) ↔ " + pIn.getName() + " (เข้า)\nโควตาคงเหลือ: " + remaining + "/3");
            }
        });

        recommendBtn.setOnAction(ev -> {
            if (this.currentSubsUsed + newSubsMadeInSession >= 3) {
                showMessage(Alert.AlertType.WARNING, "Limit Reached", "คุณใช้โควตาเปลี่ยนตัวครบ 3 คนแล้ว!");
                return;
            }

            Player worstStarter = null;
            int lowestScore = 999;

            for (Player p : starters) {
                int score = p.getStamina();
                if (yellowCardPlayers.contains(p.getName())) score -= 2;
                if (score < lowestScore) {
                    lowestScore = score;
                    worstStarter = p;
                }
            }

            Player bestBench = null;
            int highestStamina = -1;
            for (Player p : bench) {
                if (!subbedOutPlayers.contains(p) && p.getStamina() > highestStamina) {
                    highestStamina = p.getStamina();
                    bestBench = p;
                }
            }

            if (worstStarter != null && bestBench != null) {
                starterCombo.setValue(worstStarter);
                benchCombo.setValue(bestBench);
                showMessage(Alert.AlertType.INFORMATION, "Substitution Recommendation",
                        "💡 แนะนำเปลี่ยนตัว:\n" +
                                "ถอด: " + worstStarter.getName() + " [" + worstStarter.getPosition() + "] " + worstStarter.getStaminaStatusIcon() + "\n" +
                                "ส่ง: " + bestBench.getName() + " [" + bestBench.getPosition() + "] " + bestBench.getStaminaStatusIcon() + " เข้าแทน");
            } else {
                showMessage(Alert.AlertType.INFORMATION, "Notice", "ไม่มีตัวสำรองที่สามารถเปลี่ยนลงสนามได้แล้ว");
            }
        });

        contentPanel.getChildren().addAll(scoutPanel, tacticPanel, stancePanel, subPanel, btnActionPanel);

        Button startH2Btn = new Button("Start Second Half ⚽");
        startH2Btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        startH2Btn.setMaxWidth(Double.MAX_VALUE);
        startH2Btn.setOnAction(ev -> {
            String selectedForm = formCombo.getValue();

            // ปรับปรุงให้รองรับทุก Formation ตาม Dropdown
            Formation newForm;
            switch (selectedForm != null ? selectedForm : "4-4-2") {
                case "4-3-3": newForm = new Formation433(); break;
                case "3-4-3": newForm = new Formation343(); break;
                case "5-3-2": newForm = new Formation532(); break;
                default: newForm = new Formation442(); break;
            }

            Stance newStance = stanceCombo.getValue();

            // ใช้ withLineup แทน create เพราะตัวจริงอาจเหลือน้อยกว่า 11 คน (โดนใบแดง)
            updatedTeamSetup = currentSetup.withLineup(newForm, newStance, starters, bench);
            confirmed = true;
            close();
        });

        BorderPane root = new BorderPane();
        root.setCenter(contentPanel);
        BorderPane.setMargin(startH2Btn, new Insets(0, 15, 15, 15));
        root.setBottom(startH2Btn);

        setScene(new Scene(root, 620, 560));

        setOnShown(e -> {
            if (parent != null) {
                setX(parent.getX() + (parent.getWidth() - getWidth()) / 2);
                setY(parent.getY() + (parent.getHeight() - getHeight()) / 2);
            }
        });
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(this);
        alert.showAndWait();
    }

    private static Text boldText(String s) {
        Text t = new Text(s);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        return t;
    }

    private static Text blueText(String s) {
        Text t = new Text(s);
        t.setFont(Font.font("Segoe UI", 14));
        t.setFill(Color.BLUE);
        return t;
    }

    private static Text plainText(String s) {
        Text t = new Text(s);
        t.setFont(Font.font("Segoe UI", 14));
        return t;
    }

    private String getPanelSubTitle(int activeStarters) {
        int totalUsed = currentSubsUsed + newSubsMadeInSession;
        return String.format("Player Substitution (Used: %d/3 | Active Players: %d)", totalUsed, activeStarters);
    }

    private void updateComboBoxModels(ComboBox<Player> starterCombo, ComboBox<Player> benchCombo,
                                      List<Player> starters, List<Player> bench, Set<String> yellowCards) {

        starterCombo.setItems(FXCollections.observableArrayList(starters));
        benchCombo.setItems(FXCollections.observableArrayList(bench));
        starterCombo.getSelectionModel().selectFirst();
        benchCombo.getSelectionModel().selectFirst();

        starterCombo.setCellFactory(lv -> new PlayerCell(yellowCards));
        starterCombo.setButtonCell(new PlayerCell(yellowCards));
        benchCombo.setCellFactory(lv -> new PlayerCell(yellowCards));
        benchCombo.setButtonCell(new PlayerCell(yellowCards));
    }

    /** แสดงผู้เล่นในคอมโบ: ไอคอนแรง ตำแหน่ง ชื่อ สแตมินา ใบเหลือง และป้ายห้ามใช้ */
    private class PlayerCell extends ListCell<Player> {
        private final Set<String> yellowCards;

        PlayerCell(Set<String> yellowCards) {
            this.yellowCards = yellowCards;
        }

        @Override
        protected void updateItem(Player p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setText(null);
                setStyle("");
                return;
            }
            String icon = p.getStaminaStatusIcon();
            String posTag = "[" + p.getPosition() + "]"; // แทรกตำแหน่ง [ST], [CB], [GK]
            String cardTag = yellowCards.contains(p.getName()) ? " 🟨" : "";
            String subbedOutTag = subbedOutPlayers.contains(p) ? " 🚫 [Unavailable]" : "";

            setText(String.format("%s %s %s (Stam: %d/%d)%s%s",
                    icon, posTag, p.getName(), p.getStamina(), p.getMaxStamina(), cardTag, subbedOutTag));

            setStyle(subbedOutPlayers.contains(p) ? "-fx-text-fill: gray;" : "");
        }
    }

    public TeamSetup getUpdatedTeamSetup() { return updatedTeamSetup; }
    public boolean isConfirmed() { return confirmed; }
    public int getUpdatedSubsUsed() { return currentSubsUsed + newSubsMadeInSession; }
}
