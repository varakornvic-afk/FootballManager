package football.ui;

import football.logic.TeamSetupFactory;
import football.model.*;
import football.util.PlayerRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SquadPanel extends ImageBackgroundPanel {
    private static final String BACKGROUND_PATH = "/image/squad_bg.png";
    private static final int OVERLAY_ALPHA = 0; // ภาพซีดอยู่แล้ว ไม่ต้องทับแผ่นดำ

    /** ชื่อย่อตำแหน่งสำหรับแสดงในลิสต์ (ไม่แตะ enum Position จึงไม่กระทบตรรกะเกม) */
    private static final Map<String, String> POSITION_ABBR = Map.of(
            "[GK]", "[GK]",
            "[CENTER_BACK]", "[CB]",
            "[FULLBACK]", "[FB]",
            "[MIDFIELD]", "[MF]",
            "[MIDWING]", "[MW]",
            "[FRONTWING]", "[FW]",
            "[STRIKER]", "[ST]");

    private ComboBox<String> tacticComboBox, formationComboBox;

    private final ObservableList<Player> poolListModel = FXCollections.observableArrayList();
    private final ObservableList<Player> starterListModel = FXCollections.observableArrayList();
    private final ObservableList<Player> benchListModel = FXCollections.observableArrayList();

    private ListView<Player> poolJList, starterJList, benchJList;
    private Label statusCountLabel;

    private final BiConsumer<TeamSetup, TeamSetup> onSquadConfirmed;

    /** @param onSquadConfirmed ถูกเรียกเมื่อกด Confirm สำเร็จ ส่ง (ทีมเหย้า, ทีมเยือน) กลับไป */
    public SquadPanel(BiConsumer<TeamSetup, TeamSetup> onSquadConfirmed) {
        super(BACKGROUND_PATH, OVERLAY_ALPHA);
        this.onSquadConfirmed = onSquadConfirmed;

        BorderPane root = getContent();
        root.setPadding(new Insets(10));

        poolJList = new ListView<>(poolListModel);
        starterJList = new ListView<>(starterListModel);
        benchJList = new ListView<>(benchListModel);
        for (ListView<Player> list : List.of(poolJList, starterJList, benchJList)) {
            list.setCellFactory(lv -> new CompactPlayerCell());
        }

        GridPane selectionPanel = new GridPane();
        selectionPanel.setHgap(10);
        selectionPanel.setVgap(10);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            selectionPanel.getColumnConstraints().add(cc);
        }
        RowConstraints rc = new RowConstraints();
        rc.setVgrow(Priority.ALWAYS);
        selectionPanel.getRowConstraints().add(rc);

        selectionPanel.add(createWrappedListPanel("Available Pool (20)", poolJList), 0, 0);
        selectionPanel.add(createWrappedListPanel("Starters (Need 11)", starterJList), 1, 0);
        selectionPanel.add(createWrappedListPanel("Bench (Need 5)", benchJList), 2, 0);
        root.setCenter(selectionPanel);

        Button toStarterBtn = new Button(">> Starter");
        Button toBenchBtn = new Button(">> Bench");
        Button removeBtn = new Button("<< Remove");
        Button autoSelectBtn = new Button("⚡ Auto Select");

        HBox transferBtnPanel = new HBox(5, toStarterBtn, toBenchBtn, removeBtn, autoSelectBtn, new SoundToggleButton());
        transferBtnPanel.setAlignment(Pos.CENTER);

        toStarterBtn.setOnAction(e -> movePlayer(poolJList, poolListModel, starterListModel, 11));
        toBenchBtn.setOnAction(e -> movePlayer(poolJList, poolListModel, benchListModel, 5));
        removeBtn.setOnAction(e -> {
            if (starterJList.getSelectionModel().getSelectedItem() != null) movePlayer(starterJList, starterListModel, poolListModel, 20);
            else if (benchJList.getSelectionModel().getSelectedItem() != null) movePlayer(benchJList, benchListModel, poolListModel, 20);
        });
        autoSelectBtn.setOnAction(e -> handleAutoSelect());

        tacticComboBox = new ComboBox<>(FXCollections.observableArrayList("Counter Attack", "Possession", "Wide Play"));
        formationComboBox = new ComboBox<>(FXCollections.observableArrayList("4-4-2", "4-3-3", "3-4-3", "5-3-2"));
        tacticComboBox.getSelectionModel().selectFirst();
        formationComboBox.getSelectionModel().selectFirst();
        installHints(tacticComboBox, TacticHints::forTactic);
        installHints(formationComboBox, TacticHints::forFormation);

        HBox configRow = new HBox(10, new Label("Tactic:"), tacticComboBox, new Label("Formation:"), formationComboBox);
        configRow.setAlignment(Pos.CENTER_LEFT);
        TitledPane configPanel = new TitledPane("Tactics & Formation", configRow);
        configPanel.setCollapsible(false);

        statusCountLabel = new Label("Starters: 0/11 | Bench: 0/5");
        statusCountLabel.setMaxWidth(Double.MAX_VALUE);
        statusCountLabel.setAlignment(Pos.CENTER);

        Button confirmButton = new Button("Confirm Squad & Start Match");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(e -> handleConfirmSquad());

        VBox outerSouth = new VBox(8, transferBtnPanel, configPanel, statusCountLabel, confirmButton);
        outerSouth.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(outerSouth);
    }

    /** สุ่ม pool 20 คนใหม่ (เรียกตอนกด Start Game) */
    public void startNewDraftSession() {
        List<Player> all = PlayerRepository.loadPlayersFromCSV("players.csv");
        List<Player> gks = new ArrayList<>(), outfields = new ArrayList<>();
        for (Player p : all) {
            if (p.getPosition() == Position.GK) gks.add(p);
            else outfields.add(p);
        }
        Collections.shuffle(gks); Collections.shuffle(outfields);

        poolListModel.clear(); starterListModel.clear(); benchListModel.clear();
        if (!gks.isEmpty()) poolListModel.add(gks.remove(0));
        outfields.addAll(gks); Collections.shuffle(outfields);

        while (poolListModel.size() < 20 && !outfields.isEmpty()) {
            poolListModel.add(outfields.remove(0));
        }
        updateCounts();
    }

    private void movePlayer(ListView<Player> sourceList, ObservableList<Player> source, ObservableList<Player> target, int maxLimit) {
        Player p = sourceList.getSelectionModel().getSelectedItem();
        if (p != null && target.size() < maxLimit) {
            source.remove(p);
            target.add(p);
            updateCounts();
        }
    }

    private void handleAutoSelect() {
        List<Player> all = new ArrayList<>();
        all.addAll(poolListModel);
        all.addAll(starterListModel);
        all.addAll(benchListModel);

        List<Player> gks = new ArrayList<>(), outfields = new ArrayList<>();
        for (Player p : all) {
            if (p.getPosition() == Position.GK) gks.add(p); else outfields.add(p);
        }
        if (gks.isEmpty()) return;

        Collections.shuffle(gks); Collections.shuffle(outfields);
        starterListModel.clear(); benchListModel.clear(); poolListModel.clear();

        starterListModel.add(gks.remove(0));
        for (int i = 0; i < 10 && !outfields.isEmpty(); i++) starterListModel.add(outfields.remove(0));
        outfields.addAll(gks); Collections.shuffle(outfields);
        for (int i = 0; i < 5 && !outfields.isEmpty(); i++) benchListModel.add(outfields.remove(0));
        for (Player p : outfields) poolListModel.add(p);
        updateCounts();
    }

    private void handleConfirmSquad() {
        if (starterListModel.size() != 11 || benchListModel.size() != 5) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "เลือกตัวจริงให้ครบ 11 คน และตัวสำรอง 5 คน!");
            alert.setTitle("Error");
            alert.setHeaderText(null);
            if (getScene() != null) alert.initOwner(getScene().getWindow());
            alert.showAndWait();
            return;
        }
        List<Player> starters = new ArrayList<>(starterListModel);
        List<Player> bench = new ArrayList<>(benchListModel);

        TeamSetup home = TeamSetupFactory.createHome(
                tacticComboBox.getValue(),
                formationComboBox.getValue(),
                starters, bench);
        TeamSetup away = TeamSetupFactory.createRandomAway();

        onSquadConfirmed.accept(home, away);
    }

    /** tooltip บอกใบ้: ที่ตัว combo (ตามตัวเลือกที่เลือกอยู่) และที่แต่ละรายการใน dropdown */
    private static void installHints(ComboBox<String> box, Function<String, String> hint) {
        Tooltip boxTip = new Tooltip();
        boxTip.setWrapText(true);
        boxTip.setMaxWidth(340);
        Runnable update = () -> {
            String t = box.getValue() == null ? null : hint.apply(box.getValue());
            boxTip.setText(t == null ? "" : t);
        };
        box.valueProperty().addListener((obs, oldV, newV) -> update.run());
        update.run();
        box.setTooltip(boxTip);

        box.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    String t = hint.apply(item);
                    if (t == null) {
                        setTooltip(null);
                    } else {
                        Tooltip tip = new Tooltip(t);
                        tip.setWrapText(true);
                        tip.setMaxWidth(340);
                        setTooltip(tip);
                    }
                }
            }
        });
        // ช่องที่แสดงค่าที่เลือกอยู่ ใช้ข้อความธรรมดา (tooltip ของช่องดูแลโดย boxTip)
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
    }

    private TitledPane createWrappedListPanel(String title, ListView<Player> list) {
        TitledPane p = new TitledPane(title, list);
        p.setCollapsible(false);
        p.setMaxWidth(Double.MAX_VALUE);
        p.setMaxHeight(Double.MAX_VALUE);
        return p;
    }

    /**
     * แสดงผู้เล่นในลิสต์แบบสั้น เพื่อไม่ให้ข้อความโดนตัด
     * ใช้ข้อความ toString() เดิมของ Player แล้วย่อตำแหน่งกับตัวคั่น (ไม่ต้องแก้ Player)
     */
    private static class CompactPlayerCell extends ListCell<Player> {
        @Override
        protected void updateItem(Player item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                String full = String.valueOf(item);
                setText(compact(full));
            }
        }

        private static String compact(String text) {
            String result = text;
            for (Map.Entry<String, String> e : POSITION_ABBR.entrySet()) {
                if (result.startsWith(e.getKey())) {
                    result = e.getValue() + result.substring(e.getKey().length());
                    break;
                }
            }
            return result.replace(" | ", "  ")
                    .replace("Head:", "Hd:");
        }
    }

    private void updateCounts() {
        statusCountLabel.setText(String.format("Starters: %d/11 | Bench: %d/5", starterListModel.size(), benchListModel.size()));
    }
}