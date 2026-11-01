package football.ui;

import football.event.*;
import football.logic.Match;
import football.logic.MatchStatistics;
import football.model.*;
import football.util.PlayerRepository;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private JPanel mainMenuPanel, squadPanel, matchPanel;
    // เปลี่ยนจาก JTextArea เป็น JTextPane เพื่อให้จัดกึ่งกลางได้
    private JTextPane matchLogArea;
    private JComboBox<String> tacticComboBox, formationComboBox;

    private DefaultListModel<Player> poolListModel = new DefaultListModel<>();
    private DefaultListModel<Player> starterListModel = new DefaultListModel<>();
    private DefaultListModel<Player> benchListModel = new DefaultListModel<>();

    private JList<Player> poolJList, starterJList, benchJList;
    private JLabel statusCountLabel;

    private TeamSetup homeTeamSetup, awayTeamSetup;
    private Match currentMatch;
    private javax.swing.Timer liveTimer;
    private int currentEventIndex = 0;
    private boolean isSecondHalfRunning = false;
    private List<MatchEvent> h1Events, h2Events;
    private final List<MatchEvent> allMatchEvents = new ArrayList<>();
    private int liveHomeGoals = 0, liveAwayGoals = 0;

    public MainFrame() {
        setTitle("Football Manager 2026");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initMainMenu();
        initSquadScreen();
        initMatchScreen();

        cardPanel.add(mainMenuPanel, "MENU");
        cardPanel.add(squadPanel, "SQUAD");
        cardPanel.add(matchPanel, "MATCH");

        add(cardPanel);
        cardLayout.show(cardPanel, "MENU");
    }

    private void initMainMenu() {
        mainMenuPanel = new JPanel(new GridBagLayout());
        mainMenuPanel.setBackground(new Color(30, 40, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;

        JLabel titleLabel = new JLabel("FOOTBALL MANAGER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        mainMenuPanel.add(titleLabel, gbc);

        JButton startButton = new JButton("Start Game");
        JButton exitButton = new JButton("Exit");
        gbc.gridy = 1; mainMenuPanel.add(startButton, gbc);
        gbc.gridy = 2; mainMenuPanel.add(exitButton, gbc);

        startButton.addActionListener(e -> {
            startNewDraftSession();
            cardLayout.show(cardPanel, "SQUAD");
        });
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void startNewDraftSession() {
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

    private void initSquadScreen() {
        squadPanel = new JPanel(new BorderLayout(10, 10));
        squadPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel selectionPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        poolJList = new JList<>(poolListModel);
        starterJList = new JList<>(starterListModel);
        benchJList = new JList<>(benchListModel);

        selectionPanel.add(createWrappedListPanel("Available Pool (20)", poolJList));
        selectionPanel.add(createWrappedListPanel("Starters (Need 11)", starterJList));
        selectionPanel.add(createWrappedListPanel("Bench (Need 5)", benchJList));
        squadPanel.add(selectionPanel, BorderLayout.CENTER);

        JPanel transferBtnPanel = new JPanel(new FlowLayout());
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
        configPanel.setBorder(BorderFactory.createTitledBorder("Tactics & Formation"));
        tacticComboBox = new JComboBox<>(new String[]{"Counter Attack", "Possession", "Wide Play"});
        formationComboBox = new JComboBox<>(new String[]{"4-4-2", "4-3-3", "3-4-3", "5-3-2"});
        configPanel.add(new JLabel("Tactic:")); configPanel.add(tacticComboBox);
        configPanel.add(new JLabel("Formation:")); configPanel.add(formationComboBox);

        JPanel midContainer = new JPanel(new BorderLayout());
        midContainer.add(transferBtnPanel, BorderLayout.NORTH);
        midContainer.add(configPanel, BorderLayout.CENTER);
        statusCountLabel = new JLabel("Starters: 0/11 | Bench: 0/5", SwingConstants.CENTER);
        midContainer.add(statusCountLabel, BorderLayout.SOUTH);

        JButton confirmButton = new JButton("Confirm Squad & Start Match");
        confirmButton.addActionListener(e -> handleConfirmSquad());

        JPanel outerSouth = new JPanel(new BorderLayout());
        outerSouth.add(midContainer, BorderLayout.CENTER);
        outerSouth.add(confirmButton, BorderLayout.SOUTH);
        squadPanel.add(outerSouth, BorderLayout.SOUTH);
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
            JOptionPane.showMessageDialog(this, "เลือกตัวจริงให้ครบ 11 คน และตัวสำรอง 5 คน!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Player> starters = Collections.list(starterListModel.elements());
        List<Player> bench = Collections.list(benchListModel.elements());

        Tactic tactic = "Possession".equals(tacticComboBox.getSelectedItem()) ? new Possession() : new CounterAttack();
        Formation form = "4-3-3".equals(formationComboBox.getSelectedItem()) ? new Formation433() : new Formation442();

        homeTeamSetup = TeamSetup.create(tactic, form, Stance.NORMAL, starters, bench);

        List<Player> all = PlayerRepository.loadPlayersFromCSV("players.csv");
        Collections.shuffle(all);
        awayTeamSetup = TeamSetup.create(new Possession(), new Formation442(), Stance.NORMAL,
                all.subList(0, 11), all.subList(11, 16));

        cardLayout.show(cardPanel, "MATCH");
    }

    private JPanel createWrappedListPanel(String title, JList<Player> list) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private void updateCounts() {
        statusCountLabel.setText(String.format("Starters: %d/11 | Bench: %d/5", starterListModel.size(), benchListModel.size()));
    }

    private void initMatchScreen() {
        matchPanel = new JPanel(new BorderLayout(10, 10));
        matchLogArea = new JTextPane();
        matchLogArea.setEditable(false);
        matchPanel.add(new JScrollPane(matchLogArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton simButton = new JButton("Play Match");
        JButton backButton = new JButton("Back to Menu");
        controlPanel.add(simButton); controlPanel.add(backButton);
        matchPanel.add(controlPanel, BorderLayout.SOUTH);

        simButton.addActionListener(e -> runMatchSimulation());
        backButton.addActionListener(e -> {
            if (liveTimer != null) liveTimer.stop();
            cardLayout.show(cardPanel, "MENU");
        });
    }

    private void runMatchSimulation() {
        if (liveTimer != null && liveTimer.isRunning()) liveTimer.stop();
        matchLogArea.setText("");
        appendColoredLog("=== KICK OFF (FIRST HALF) ===");
        appendColoredLog("");

        currentMatch = new Match(homeTeamSetup, awayTeamSetup, System.currentTimeMillis());
        h1Events = currentMatch.playFirstHalf().getEvents();

        currentEventIndex = 0;
        isSecondHalfRunning = false;
        liveHomeGoals = 0;
        liveAwayGoals = 0;

        liveTimer = new javax.swing.Timer(1800, e -> {
            List<MatchEvent> currentList = isSecondHalfRunning ? h2Events : h1Events;

            if (currentList != null && currentEventIndex < currentList.size()) {
                processEvent(currentList.get(currentEventIndex++));
            } else if (!isSecondHalfRunning) {
                liveTimer.stop();
                appendColoredLog("");
                appendColoredLog("--- HALF TIME ---");
                appendColoredLog("");

                // 1. ดึงรายชื่อนักเตะที่ติดใบเหลืองในครึ่งแรก
                Set<String> yellowCardPlayers = new HashSet<>();
                if (h1Events != null) {
                    for (MatchEvent ev : h1Events) {
                        if (ev.getDescription().contains("Yellow card") && ev.getPlayer() != null) {
                            yellowCardPlayers.add(ev.getPlayer().getName());
                        }
                    }
                }

                // 2. เรียก HalfTimeDialog เพื่อปรับ Stance / Formation / Substitution
                HalfTimeDialog htDialog = new HalfTimeDialog(this, homeTeamSetup, yellowCardPlayers);
                htDialog.setVisible(true);

                // 3. ถ้าผู้เล่นกด Confirm หน้าพักครึ่ง ให้ประมวลผลครึ่งหลังต่อ
                if (htDialog.isConfirmed()) {
                    homeTeamSetup = htDialog.getUpdatedTeamSetup();

                    // คำนวณเหตุการณ์ครึ่งหลังตามแผนและไลน์อัปใหม่
                    h2Events = currentMatch.playSecondHalf().getEvents();

                    allMatchEvents.clear();
                    if (h1Events != null) allMatchEvents.addAll(h1Events);
                    if (h2Events != null) allMatchEvents.addAll(h2Events);

                    isSecondHalfRunning = true;
                    currentEventIndex = 0;
                    appendColoredLog("=== SECOND HALF ===");
                    appendColoredLog("");
                    liveTimer.start();
                }
            } else {
                liveTimer.stop();
                appendColoredLog("");
                appendColoredLog(String.format("=== FULL TIME: Home %d - %d Away ===", liveHomeGoals, liveAwayGoals));
                MatchStatistics stats = MatchStatistics.calculate(allMatchEvents);
                new MatchSummaryDialog(this, "Home", "Away", liveHomeGoals, liveAwayGoals, stats).setVisible(true);
            }
        });
        liveTimer.start();
    }


    private void processEvent(MatchEvent ev) {
        String desc = ev.getDescription();
        String teamName = ev.getTeamName();
        boolean isHome = teamName != null && teamName.toLowerCase().contains("home");
        String descLower = desc.toLowerCase();

        String playerName = "Player";
        String formattedLog = "";

        // 1. ยิงประตูได้ (Goal)
        if (descLower.contains("goal") && !descLower.contains("save")) {
            if (isHome) liveHomeGoals++; else liveAwayGoals++;
            playerName = ev.getPlayer() != null ? ev.getPlayer().getName() : "Player";

            formattedLog = String.format("[%d'] %s: GOAL! scored by %s ⚽", ev.getMinute(), teamName, playerName);
            EventPopupDialog.showPopup(this, new GameEvent.GoalEvent(playerName, getPlayerImagePath(playerName)), isHome);
        }
        // 2. ผู้รักษาประตูเซฟได้ (Save)
        else if (descLower.contains("save") || descLower.contains("saved")) {
            boolean isKeeperHome = !isHome; // ผู้รักษาประตูคือฝั่งรับ

            if (ev.getPlayer() != null) {
                playerName = ev.getPlayer().getName();
            } else {
                TeamSetup defendingTeam = isHome ? awayTeamSetup : homeTeamSetup;
                if (defendingTeam != null && defendingTeam.getStarters() != null) {
                    for (Player p : defendingTeam.getStarters()) {
                        if (p.getPosition() == Position.GK) {
                            playerName = p.getName();
                            break;
                        }
                    }
                }
            }

            formattedLog = String.format("[%d'] %s: Shot saved by GK %s 🧤", ev.getMinute(), teamName, playerName);
            EventPopupDialog.showPopup(this, new GameEvent.SaveEvent(playerName, getPlayerImagePath(playerName)), isKeeperHome);
        }
        // 3. ทำฟาล์ว / ใบเหลือง-ใบแดง (Foul)
        else if (desc.contains("Yellow card") || desc.contains("Red card") || descLower.contains("foul")) {
            playerName = ev.getPlayer() != null ? ev.getPlayer().getName() : "Player";
            boolean isRed = desc.contains("Red card");
            String cardText = isRed ? "RED CARD 🟥" : "YELLOW CARD 🟨";

            formattedLog = String.format("[%d'] %s: FOUL by %s (%s)", ev.getMinute(), teamName, playerName, cardText);
            EventPopupDialog.showPopup(this, new GameEvent.FoulEvent(playerName, getPlayerImagePath(playerName), isRed), isHome);
        }
        // 4. เหตุการณ์ทั่วไป
        else {
            formattedLog = String.format("[%d'] %s: %s", ev.getMinute(), teamName, desc);
        }

        // แสดงข้อความกึ่งกลางบน JTextPane สีดำมาตรฐาน
        appendColoredLog(formattedLog);
    }

    // Helper Method สำหรับพิมพ์ข้อความจัดกึ่งกลางลงใน JTextPane
    private void appendColoredLog(String text) {
        StyledDocument doc = matchLogArea.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();

        StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
        StyleConstants.setForeground(style, Color.BLACK);
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

    private String getPlayerImagePath(String name) {
        if (name == null || name.isEmpty()) return "src/main/resources/image/Player/allstars.jpeg";

        String dynamicPath = "src/main/resources/image/Player/" + name.trim() + ".jpg";
        java.io.File file = new java.io.File(dynamicPath);

        return file.exists() ? dynamicPath : "src/main/resources/image/Player/Haalond.jpg";
    }
}