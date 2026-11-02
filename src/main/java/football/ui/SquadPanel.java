package football.ui;

import football.logic.TeamSetupFactory;
import football.model.*;
import football.util.PlayerRepository;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;

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

    private JComboBox<String> tacticComboBox, formationComboBox;

    /** tooltip บอกใบ้ของแต่ละตัวเลือก (ทั้งตอนเปิดรายการและตอนเลือกแล้ว) ไม่ใช้ popup */
    private static void installHints(JComboBox<String> box, java.util.function.Function<String, String> hint) {
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) setToolTipText(hint.apply(value.toString()));
                return c;
            }
        });
        box.addActionListener(e -> {
            Object selected = box.getSelectedItem();
            box.setToolTipText(selected == null ? null : hint.apply(selected.toString()));
        });
        Object selected = box.getSelectedItem();
        box.setToolTipText(selected == null ? null : hint.apply(selected.toString()));
    }

    private final DefaultListModel<Player> poolListModel = new DefaultListModel<>();
    private final DefaultListModel<Player> starterListModel = new DefaultListModel<>();
    private final DefaultListModel<Player> benchListModel = new DefaultListModel<>();

    private JList<Player> poolJList, starterJList, benchJList;
    private JLabel statusCountLabel;

    private final BiConsumer<TeamSetup, TeamSetup> onSquadConfirmed;

    /** @param onSquadConfirmed ถูกเรียกเมื่อกด Confirm สำเร็จ ส่ง (ทีมเหย้า, ทีมเยือน) กลับไป */
    public SquadPanel(BiConsumer<TeamSetup, TeamSetup> onSquadConfirmed) {
        super(BACKGROUND_PATH, OVERLAY_ALPHA, new BorderLayout(10, 10));
        this.onSquadConfirmed = onSquadConfirmed;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel selectionPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        selectionPanel.setOpaque(false);
        poolJList = new JList<>(poolListModel);
        starterJList = new JList<>(starterListModel);
        benchJList = new JList<>(benchListModel);
        for (JList<Player> list : List.of(poolJList, starterJList, benchJList)) {
            list.setCellRenderer(new CompactPlayerRenderer());
        }

        selectionPanel.add(createWrappedListPanel("Available Pool (20)", poolJList));
        selectionPanel.add(createWrappedListPanel("Starters (Need 11)", starterJList));
        selectionPanel.add(createWrappedListPanel("Bench (Need 5)", benchJList));
        add(selectionPanel, BorderLayout.CENTER);

        JPanel transferBtnPanel = new JPanel(new FlowLayout());
        transferBtnPanel.setOpaque(false);
        JButton toStarterBtn = new JButton(">> Starter");
        JButton toBenchBtn = new JButton(">> Bench");
        JButton removeBtn = new JButton("<< Remove");
        JButton autoSelectBtn = new JButton("⚡ Auto Select");

        transferBtnPanel.add(toStarterBtn); transferBtnPanel.add(toBenchBtn);
        transferBtnPanel.add(removeBtn); transferBtnPanel.add(autoSelectBtn);

        toStarterBtn.addActionListener(e -> movePlayer(poolJList, poolListModel, starterListModel, 11));
        toBenchBtn.addActionListener(e -> movePlayer(poolJList, poolListModel, benchListModel, 5));
        removeBtn.addActionListener(e -> {
            if (starterJList.getSelectedValue() != null) movePlayer(starterJList, starterListModel, poolListModel, 20);
            else if (benchJList.getSelectedValue() != null) movePlayer(benchJList, benchListModel, poolListModel, 20);
        });
        autoSelectBtn.addActionListener(e -> handleAutoSelect());

        JPanel configPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        configPanel.setOpaque(false);
        configPanel.setBorder(BorderFactory.createTitledBorder("Tactics & Formation"));
        tacticComboBox = new JComboBox<>(new String[]{"Counter Attack", "Possession", "Wide Play"});
        formationComboBox = new JComboBox<>(new String[]{"4-4-2", "4-3-3", "3-4-3", "5-3-2"});
        installHints(tacticComboBox, TacticHints::forTactic);
        installHints(formationComboBox, TacticHints::forFormation);
        configPanel.add(new JLabel("Tactic:")); configPanel.add(tacticComboBox);
        configPanel.add(new JLabel("Formation:")); configPanel.add(formationComboBox);

        JPanel midContainer = new JPanel(new BorderLayout());
        midContainer.setOpaque(false);
        midContainer.add(transferBtnPanel, BorderLayout.NORTH);
        midContainer.add(configPanel, BorderLayout.CENTER);
        statusCountLabel = new JLabel("Starters: 0/11 | Bench: 0/5", SwingConstants.CENTER);
        midContainer.add(statusCountLabel, BorderLayout.SOUTH);

        JButton confirmButton = new JButton("Confirm Squad & Start Match");
        confirmButton.addActionListener(e -> handleConfirmSquad());

        JPanel outerSouth = new JPanel(new BorderLayout());
        outerSouth.setOpaque(false);
        outerSouth.add(midContainer, BorderLayout.CENTER);
        outerSouth.add(confirmButton, BorderLayout.SOUTH);
        add(outerSouth, BorderLayout.SOUTH);
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
        if (!gks.isEmpty()) poolListModel.addElement(gks.remove(0));
        outfields.addAll(gks); Collections.shuffle(outfields);

        while (poolListModel.size() < 20 && !outfields.isEmpty()) {
            poolListModel.addElement(outfields.remove(0));
        }
        updateCounts();
    }

    private void movePlayer(JList<Player> sourceList, DefaultListModel<Player> source, DefaultListModel<Player> target, int maxLimit) {
        Player p = sourceList.getSelectedValue();
        if (p != null && target.size() < maxLimit) {
            source.removeElement(p);
            target.addElement(p);
            updateCounts();
        }
    }

    private void handleAutoSelect() {
        List<Player> all = new ArrayList<>();
        for (int i = 0; i < poolListModel.size(); i++) all.add(poolListModel.getElementAt(i));
        for (int i = 0; i < starterListModel.size(); i++) all.add(starterListModel.getElementAt(i));
        for (int i = 0; i < benchListModel.size(); i++) all.add(benchListModel.getElementAt(i));

        List<Player> gks = new ArrayList<>(), outfields = new ArrayList<>();
        for (Player p : all) {
            if (p.getPosition() == Position.GK) gks.add(p); else outfields.add(p);
        }
        if (gks.isEmpty()) return;

        Collections.shuffle(gks); Collections.shuffle(outfields);
        starterListModel.clear(); benchListModel.clear(); poolListModel.clear();

        starterListModel.addElement(gks.remove(0));
        for (int i = 0; i < 10 && !outfields.isEmpty(); i++) starterListModel.addElement(outfields.remove(0));
        outfields.addAll(gks); Collections.shuffle(outfields);
        for (int i = 0; i < 5 && !outfields.isEmpty(); i++) benchListModel.addElement(outfields.remove(0));
        for (Player p : outfields) poolListModel.addElement(p);
        updateCounts();
    }

    private void handleConfirmSquad() {
        if (starterListModel.size() != 11 || benchListModel.size() != 5) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "เลือกตัวจริงให้ครบ 11 คน และตัวสำรอง 5 คน!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Player> starters = Collections.list(starterListModel.elements());
        List<Player> bench = Collections.list(benchListModel.elements());

        TeamSetup home = TeamSetupFactory.createHome(
                (String) tacticComboBox.getSelectedItem(),
                (String) formationComboBox.getSelectedItem(),
                starters, bench);
        TeamSetup away = TeamSetupFactory.createRandomAway();

        onSquadConfirmed.accept(home, away);
    }

    private JPanel createWrappedListPanel(String title, JList<Player> list) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    /**
     * แสดงผู้เล่นในลิสต์แบบสั้น เพื่อไม่ให้ข้อความโดนตัด
     * ใช้ข้อความ toString() เดิมของ Player แล้วย่อตำแหน่งกับตัวคั่น (ไม่ต้องแก้ Player)
     * เอาเมาส์ชี้เพื่อดูข้อความเต็มใน tooltip
     */
    private static class CompactPlayerRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String full = String.valueOf(value);
            label.setText(compact(full));
            label.setToolTipText(full);
            return label;
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