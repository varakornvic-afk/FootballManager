package football.ui;

import football.logic.Match;
import football.model.*;
import football.util.PlayerRepository;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private JPanel mainMenuPanel;
    private JPanel modeSelectPanel;
    private JPanel squadPanel;
    private JPanel matchPanel;

    private JTextArea matchLogArea;

    private JComboBox<String> tacticComboBox;
    private JComboBox<String> formationComboBox;

    // โหมดเกม (AI หรือ Hotseat)
    private boolean isHotseatMode = false;
    private boolean isAwayDrafting = false;

    // ระบบเลือกนักเตะ
    private List<Player> poolPlayersHome;
    private List<Player> poolPlayersAway;

    private DefaultListModel<Player> poolListModel;
    private DefaultListModel<Player> starterListModel;
    private DefaultListModel<Player> benchListModel;

    private JList<Player> poolJList;
    private JList<Player> starterJList;
    private JList<Player> benchJList;
    private JLabel statusCountLabel;
    private JLabel draftTitleLabel;

    private List<Player> homeStartersDraft, homeBenchDraft;
    private List<Player> awayStartersDraft, awayBenchDraft;
    private Tactic homeTactic;
    private Formation homeFormation;

    private TeamSetup homeTeamSetup;
    private TeamSetup awayTeamSetup;

    // ตัวแปรสำหรับระบบจำลองแมตช์และพักครึ่ง
    private Match currentMatch;
    private Timer liveTimer;
    private int currentEventIndex = 0;
    private boolean isSecondHalfRunning = false;
    private List<MatchEvent> h1Events;
    private List<MatchEvent> h2Events;

    public MainFrame() {
        setTitle("Football Manager - Ultimate Edition 2026");
        setSize(1100, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initMainMenu();
        initModeSelectMenu();
        initSquadScreen();
        initMatchScreen();

        cardPanel.add(mainMenuPanel, "MENU");
        cardPanel.add(modeSelectPanel, "MODE");
        cardPanel.add(squadPanel, "SQUAD");
        cardPanel.add(matchPanel, "MATCH");

        add(cardPanel);
        cardLayout.show(cardPanel, "MENU");
    }

    private void initMainMenu() {
        mainMenuPanel = new JPanel(new GridBagLayout());
        mainMenuPanel.setBackground(new Color(30, 40, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.gridx = 0;

        JLabel titleLabel = new JLabel("FOOTBALL MANAGER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridy = 0;
        mainMenuPanel.add(titleLabel, gbc);

        JButton startButton = new JButton("Play Game");
        startButton.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 1;
        mainMenuPanel.add(startButton, gbc);

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 2;
        mainMenuPanel.add(exitButton, gbc);

        startButton.addActionListener(e -> cardLayout.show(cardPanel, "MODE"));
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void initModeSelectMenu() {
        modeSelectPanel = new JPanel(new GridBagLayout());
        modeSelectPanel.setBackground(new Color(40, 50, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.gridx = 0;

        JLabel titleLabel = new JLabel("SELECT GAME MODE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridy = 0;
        modeSelectPanel.add(titleLabel, gbc);

        JButton aiModeBtn = new JButton("Player vs AI");
        aiModeBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 1;
        modeSelectPanel.add(aiModeBtn, gbc);

        JButton hotseatModeBtn = new JButton("Hotseat (2 Players)");
        hotseatModeBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 2;
        modeSelectPanel.add(hotseatModeBtn, gbc);

        JButton backBtn = new JButton("Back to Menu");
        backBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridy = 3;
        modeSelectPanel.add(backBtn, gbc);

        aiModeBtn.addActionListener(e -> {
            isHotseatMode = false;
            startNewDraftSession();
            cardLayout.show(cardPanel, "SQUAD");
        });

        hotseatModeBtn.addActionListener(e -> {
            isHotseatMode = true;
            startNewDraftSession();
            cardLayout.show(cardPanel, "SQUAD");
        });

        backBtn.addActionListener(e -> cardLayout.show(cardPanel, "MENU"));
    }

    private void startNewDraftSession() {
        List<Player> all = PlayerRepository.loadPlayersFromCSV("players.csv");
        Collections.shuffle(all);

        poolPlayersHome = new ArrayList<>(all.subList(0, 20));
        if (isHotseatMode) {
            poolPlayersAway = new ArrayList<>(all.subList(20, Math.min(40, all.size())));
            if (poolPlayersAway.size() < 20) {
                Collections.shuffle(all);
                poolPlayersAway = new ArrayList<>(all.subList(0, 20));
            }
        }

        isAwayDrafting = false;
        loadPoolToList(poolPlayersHome);
        draftTitleLabel.setText("Player 1 (HOME): Select Tactics & Formation, then pick or Auto Select");
    }

    private void loadPoolToList(List<Player> pool) {
        poolListModel.clear();
        starterListModel.clear();
        benchListModel.clear();
        for (Player p : pool) {
            poolListModel.addElement(p);
        }
        updateCounts();
    }

    private void initSquadScreen() {
        squadPanel = new JPanel(new BorderLayout(10, 10));
        squadPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        draftTitleLabel = new JLabel("Select Squad", SwingConstants.CENTER);
        draftTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        squadPanel.add(draftTitleLabel, BorderLayout.NORTH);

        JPanel selectionPanel = new JPanel(new GridLayout(1, 3, 10, 10));

        poolListModel = new DefaultListModel<>();
        starterListModel = new DefaultListModel<>();
        benchListModel = new DefaultListModel<>();

        poolJList = new JList<>(poolListModel);
        starterJList = new JList<>(starterListModel);
        benchJList = new JList<>(benchListModel);

        Font listFont = new Font("Monospaced", Font.PLAIN, 12);
        poolJList.setFont(listFont);
        starterJList.setFont(listFont);
        benchJList.setFont(listFont);

        selectionPanel.add(createWrappedListPanel("Available Pool (20)", poolJList));
        selectionPanel.add(createWrappedListPanel("Starters (Need 11)", starterJList));
        selectionPanel.add(createWrappedListPanel("Bench (Need 5)", benchJList));

        squadPanel.add(selectionPanel, BorderLayout.CENTER);

        JPanel transferBtnPanel = new JPanel(new FlowLayout());
        JButton toStarterBtn = new JButton(">> Add to Starter");
        JButton toBenchBtn = new JButton(">> Add to Bench");
        JButton removeBtn = new JButton("<< Remove back to Pool");
        JButton autoSelectBtn = new JButton("⚡ Auto Select Squad");
        autoSelectBtn.setBackground(new Color(70, 130, 180));
        autoSelectBtn.setForeground(Color.WHITE);

        transferBtnPanel.add(toStarterBtn);
        transferBtnPanel.add(toBenchBtn);
        transferBtnPanel.add(removeBtn);
        transferBtnPanel.add(autoSelectBtn);

        toStarterBtn.addActionListener(e -> {
            Player p = poolJList.getSelectedValue();
            if (p != null) {
                if (starterListModel.size() >= 11) {
                    JOptionPane.showMessageDialog(this, "ตัวจริงครบ 11 คนแล้ว!");
                    return;
                }
                poolListModel.removeElement(p);
                starterListModel.addElement(p);
                updateCounts();
            }
        });

        toBenchBtn.addActionListener(e -> {
            Player p = poolJList.getSelectedValue();
            if (p != null) {
                if (benchListModel.size() >= 5) {
                    JOptionPane.showMessageDialog(this, "ตัวสำรองครบ 5 คนแล้ว!");
                    return;
                }
                poolListModel.removeElement(p);
                benchListModel.addElement(p);
                updateCounts();
            }
        });

        removeBtn.addActionListener(e -> {
            Player p1 = starterJList.getSelectedValue();
            if (p1 != null) {
                starterListModel.removeElement(p1);
                poolListModel.addElement(p1);
                updateCounts();
                return;
            }
            Player p2 = benchJList.getSelectedValue();
            if (p2 != null) {
                benchListModel.removeElement(p2);
                poolListModel.addElement(p2);
                updateCounts();
            }
        });

        autoSelectBtn.addActionListener(e -> {
            List<Player> tempPool = new ArrayList<>();
            for (int i = 0; i < poolListModel.size(); i++) tempPool.add(poolListModel.getElementAt(i));
            for (int i = 0; i < starterListModel.size(); i++) tempPool.add(starterListModel.getElementAt(i));
            for (int i = 0; i < benchListModel.size(); i++) tempPool.add(benchListModel.getElementAt(i));

            Collections.shuffle(tempPool);

            starterListModel.clear();
            benchListModel.clear();
            poolListModel.clear();

            for (int i = 0; i < 11; i++) {
                starterListModel.addElement(tempPool.get(i));
            }
            for (int i = 11; i < 16; i++) {
                benchListModel.addElement(tempPool.get(i));
            }
            for (int i = 16; i < tempPool.size(); i++) {
                poolListModel.addElement(tempPool.get(i));
            }

            updateCounts();
            JOptionPane.showMessageDialog(this, "จัดทีมแบบ Auto สำเร็จเรียบร้อย!");
        });

        JPanel midContainer = new JPanel(new BorderLayout());
        midContainer.add(transferBtnPanel, BorderLayout.NORTH);

        JPanel configPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("1. Select Tactics & Formation First"));
        configPanel.add(new JLabel("Select Tactic:"));
        tacticComboBox = new JComboBox<>(new String[]{"Counter Attack", "Possession", "Wide Play"});
        configPanel.add(tacticComboBox);

        configPanel.add(new JLabel("Select Formation:"));
        formationComboBox = new JComboBox<>(new String[]{"4-4-2", "4-3-3", "3-4-3", "5-3-2"});
        configPanel.add(formationComboBox);

        midContainer.add(configPanel, BorderLayout.CENTER);
        statusCountLabel = new JLabel("Starters: 0/11 | Bench: 0/5", SwingConstants.CENTER);
        statusCountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        midContainer.add(statusCountLabel, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel();
        JButton confirmButton = new JButton("Confirm Squad");
        JButton backButton = new JButton("Back to Mode");
        bottomPanel.add(confirmButton);
        bottomPanel.add(backButton);

        JPanel outerSouth = new JPanel(new BorderLayout());
        outerSouth.add(midContainer, BorderLayout.CENTER);
        outerSouth.add(bottomPanel, BorderLayout.SOUTH);
        squadPanel.add(outerSouth, BorderLayout.SOUTH);

        confirmButton.addActionListener(e -> {
            if (starterListModel.size() != 11 || benchListModel.size() != 5) {
                JOptionPane.showMessageDialog(this, "กรุณาเลือกตัวจริงให้ครบ 11 คน และตัวสำรองให้ครบ 5 คน!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                List<Player> starters = new ArrayList<>();
                for (int i = 0; i < starterListModel.size(); i++) starters.add(starterListModel.getElementAt(i));
                List<Player> bench = new ArrayList<>();
                for (int i = 0; i < benchListModel.size(); i++) bench.add(benchListModel.getElementAt(i));

                Tactic tactic;
                String tacticStr = (String) tacticComboBox.getSelectedItem();
                if ("Possession".equals(tacticStr)) tactic = new Possession();
                else if ("Wide Play".equals(tacticStr)) tactic = new WidePlay();
                else tactic = new CounterAttack();

                Formation form;
                String formStr = (String) formationComboBox.getSelectedItem();
                if ("4-3-3".equals(formStr)) form = new Formation433();
                else if ("3-4-3".equals(formStr)) form = new Formation343();
                else if ("5-3-2".equals(formStr)) form = new Formation532();
                else form = new Formation442();

                if (isHotseatMode && !isAwayDrafting) {
                    homeStartersDraft = starters;
                    homeBenchDraft = bench;
                    homeTactic = tactic;
                    homeFormation = form;

                    isAwayDrafting = true;
                    loadPoolToList(poolPlayersAway);
                    draftTitleLabel.setText("Player 2 (AWAY): Select Tactics & Formation, then pick or Auto Select");
                    JOptionPane.showMessageDialog(this, "บันทึกทีม Home เรียบร้อย! ส่งต่อให้ Player 2 จัดทีม Away ได้เลยครับ");
                } else {
                    if (isHotseatMode) {
                        awayStartersDraft = starters;
                        awayBenchDraft = bench;
                        homeTeamSetup = TeamSetup.create(homeTactic, homeFormation, Stance.NORMAL, homeStartersDraft, homeBenchDraft);
                        awayTeamSetup = TeamSetup.create(tactic, form, Stance.NORMAL, awayStartersDraft, awayBenchDraft);
                    } else {
                        homeTeamSetup = TeamSetup.create(tactic, form, Stance.NORMAL, starters, bench);

                        List<Player> all = PlayerRepository.loadPlayersFromCSV("players.csv");
                        Collections.shuffle(all);
                        awayTeamSetup = TeamSetup.create(new Possession(), new Formation442(), Stance.NORMAL, all.subList(0, 11), all.subList(11, 16));
                    }
                    cardLayout.show(cardPanel, "MATCH");
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "⚠️ " + ex.getMessage(), "Team Setup Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MODE"));
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
        matchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel headerLabel = new JLabel("Match Simulation Live", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        matchPanel.add(headerLabel, BorderLayout.NORTH);

        matchLogArea = new JTextArea();
        matchLogArea.setEditable(false);
        matchLogArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        matchPanel.add(new JScrollPane(matchLogArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton simButton = new JButton("Play Full Match");
        JButton backButton = new JButton("Back to Menu");
        controlPanel.add(simButton);
        controlPanel.add(backButton);
        matchPanel.add(controlPanel, BorderLayout.SOUTH);

        simButton.addActionListener(e -> runMatchSimulation());
        backButton.addActionListener(e -> {
            if (liveTimer != null) liveTimer.stop();
            cardLayout.show(cardPanel, "MENU");
        });
    }

    private int liveHomeGoals = 0;
    private int liveAwayGoals = 0;

    private void runMatchSimulation() {
        if (homeTeamSetup == null || awayTeamSetup == null) {
            JOptionPane.showMessageDialog(this, "กรุณาจัดทีมก่อนเริ่มการแข่งขัน!");
            return;
        }

        if (liveTimer != null && liveTimer.isRunning()) {
            liveTimer.stop();
        }

        matchLogArea.setText("");
        matchLogArea.append("=== KICK OFF (FIRST HALF) ===\n\n");

        currentMatch = new Match(homeTeamSetup, awayTeamSetup, System.currentTimeMillis());

        PeriodResult firstHalf = currentMatch.playFirstHalf();
        PeriodResult secondHalf = currentMatch.playSecondHalf();

        h1Events = firstHalf.getEvents() != null ? firstHalf.getEvents() : new ArrayList<>();
        h2Events = secondHalf.getEvents() != null ? secondHalf.getEvents() : new ArrayList<>();

        currentEventIndex = 0;
        isSecondHalfRunning = false;
        liveHomeGoals = 0;
        liveAwayGoals = 0;

        liveTimer = new Timer(1200, null);
        liveTimer.addActionListener(e -> {
            if (!isSecondHalfRunning) {
                if (currentEventIndex < h1Events.size()) {
                    MatchEvent ev = h1Events.get(currentEventIndex);
                    processAndCheckPenaltyEvent(ev);
                    currentEventIndex++;
                } else {
                    liveTimer.stop();
                    matchLogArea.append("\n--- HALF TIME ---\n");
                    matchLogArea.append(String.format("Score at HT -> Home %d : %d Away\n\n", liveHomeGoals, liveAwayGoals));

                    openSubstitutionDialog();

                    isSecondHalfRunning = true;
                    currentEventIndex = 0;
                    matchLogArea.append("=== SECOND HALF KICK OFF ===\n\n");
                    liveTimer.start();
                }
            } else {
                if (currentEventIndex < h2Events.size()) {
                    MatchEvent ev = h2Events.get(currentEventIndex);
                    processAndCheckPenaltyEvent(ev);
                    currentEventIndex++;
                } else {
                    liveTimer.stop();
                    matchLogArea.append("\n=== FULL TIME ===\n");
                    matchLogArea.append(String.format("Final Score -> Home %d : %d Away\n", liveHomeGoals, liveAwayGoals));

                    if (liveHomeGoals == liveAwayGoals) {
                        matchLogArea.append("\n=== EXTRA TIME / PENALTY SHOOTOUT ===\n");
                        runPenaltyShootout();
                    }
                }
            }
        });
        liveTimer.start();
    }

    private void processAndCheckPenaltyEvent(MatchEvent ev) {
        String desc = ev.getDescription();
        String teamName = ev.getTeamName();
        boolean isHomeTeamEvent = teamName.toLowerCase().contains("home");

        if (desc.toLowerCase().contains("penalty")) {
            liveTimer.stop();

            String[] directions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center"};

            if (isHomeTeamEvent) {
                String shootDir = (String) JOptionPane.showInputDialog(this,
                        "🚨 [IN-MATCH PENALTY] ทีมของคุณได้ลูกจุดโทษ! เลือกทิศทางยิง:",
                        "Penalty Kick", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
                if (shootDir == null) shootDir = directions[0];

                String aiSaveDir = directions[(int)(Math.random() * directions.length)];
                boolean scored = !shootDir.equals(aiSaveDir);

                if (scored) {
                    liveHomeGoals++;
                    desc = "Penalty goal scored! [Shot: " + shootDir + "] ⚽";
                } else {
                    desc = "Penalty saved by Goalkeeper! [Shot: " + shootDir + ", Saved: " + aiSaveDir + "] ❌";
                }
            } else {
                String saveDir = (String) JOptionPane.showInputDialog(this,
                        "⚠️ [IN-MATCH PENALTY] ฝ่ายตรงข้ามได้ลูกจุดโทษ! เลือกทิศทางพุ่งเซฟของผู้รักษาประตู:",
                        "Defend Penalty", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
                if (saveDir == null) saveDir = directions[0];

                String aiShootDir = directions[(int)(Math.random() * directions.length)];
                boolean saved = saveDir.equals(aiShootDir);

                if (saved) {
                    desc = "Penalty saved brilliantly by our Goalkeeper! [Dive: " + saveDir + "] 🛡️";
                } else {
                    liveAwayGoals++;
                    desc = "Penalty goal scored by opponent! [Shot: " + aiShootDir + ", Dive: " + saveDir + "] ⚽";
                }
            }

            liveTimer.start();
        } else if ((desc.contains("GOAL") || desc.toLowerCase().contains("goal") || desc.contains("scores"))
                && !desc.toLowerCase().contains("save")) {
            // [แก้ไขแล้ว] เพิ่มเงื่อนไขกรองคำว่า save ออก เพื่อไม่ให้การเซฟประตูถูกนับเป็นลูกยิงเข้า
            if (isHomeTeamEvent) liveHomeGoals++;
            else liveAwayGoals++;

            String scorer = getRandomScorer(teamName);
            desc += " [Scorer: " + scorer + " ⚽]";
        }

        matchLogArea.append(String.format("[%02d'] %s: %s\n", ev.getMinute(), teamName, desc));
    }

    private void openSubstitutionDialog() {
        homeTeamSetup = homeTeamSetup.endFirstHalf();
        awayTeamSetup = awayTeamSetup.endFirstHalf();

        // --- [เพิ่มใหม่] คำนวณหา 5 ผู้เล่นตัวจริงฝั่ง Home ที่ล้าและพลังตกลงมากที่สุด ---
        List<Player> homeStarters = homeTeamSetup.getStarters();
        List<Player> sortedByFatigue = new ArrayList<>(homeStarters);
        // เรียงตาม Stamina จากน้อยไปมาก (คนสตามิน่าน้อยสุดจะโดนหักพลังเปอร์เซ็นต์เยอะสุด)
        sortedByFatigue.sort((p1, p2) -> Integer.compare(p1.getStamina(), p2.getStamina()));

        StringBuilder recMsg = new StringBuilder("💡 คำแนะนำช่วงพักครึ่ง (5 อันดับตัวจริงที่ล้าและควรพิจารณาเปลี่ยนออก):\n\n");
        int count = Math.min(5, sortedByFatigue.size());
        for (int i = 0; i < count; i++) {
            Player p = sortedByFatigue.get(i);
            // คำนวณเปอร์เซ็นต์ความเสียหายที่โดนหัก
            double reductionPercent = (0.3 / Math.max(1, p.getStamina())) * 100;
            recMsg.append(String.format("%d. %s [%s] - Stamina: %d (พลังตกลง %.0f%%)\n",
                    (i + 1), p.getName(), p.getPosition(), p.getStamina(), reductionPercent));
        }

        // แสดงผลใน Match Log และเด้ง Popup แนะนำตัวผู้เล่น
        matchLogArea.append("\n" + recMsg.toString() + "\n");
        JOptionPane.showMessageDialog(this, recMsg.toString(), "Half-Time Fatigue Recommendation", JOptionPane.INFORMATION_MESSAGE);
        // --------------------------------------------------------------------------

        // 1. จัดการฝั่ง Player 1 (Home Team)
        runTeamSubstitutionProcess("Player 1 (HOME) - Half-Time Adjustment", true);

        // 2. จัดการฝั่ง Player 2 (Away Team) เฉพาะโหมด Hotseat
        if (isHotseatMode) {
            runTeamSubstitutionProcess("Player 2 (AWAY) - Half-Time Adjustment", false);
        } else {
            matchLogArea.append("[SUB] AI (Away) kept their squad.\n");
        }

        JOptionPane.showMessageDialog(this, "สิ้นสุดการพักครึ่ง ลุยต่อครึ่งหลังกันเลย!");
    }

    private void runTeamSubstitutionProcess(String titlePrefix, boolean isHome) {
        TeamSetup currentSetup = isHome ? homeTeamSetup : awayTeamSetup;

        JPanel tacticPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        tacticPanel.add(new JLabel(titlePrefix + " - Formation:"));
        JComboBox<String> formBox = new JComboBox<>(new String[]{"Current", "4-4-2", "4-3-3", "3-4-3", "5-3-2"});
        tacticPanel.add(formBox);

        tacticPanel.add(new JLabel("Change Stance:"));
        JComboBox<String> stanceBox = new JComboBox<>(new String[]{"Current", "Normal", "Park the Bus", "All Out Attack"});
        tacticPanel.add(stanceBox);

        int tacticResult = JOptionPane.showConfirmDialog(this, tacticPanel,
                titlePrefix, JOptionPane.OK_CANCEL_OPTION);

        if (tacticResult == JOptionPane.OK_OPTION) {
            try {
                String selectedForm = (String) formBox.getSelectedItem();
                if (!"Current".equals(selectedForm)) {
                    Formation newF;
                    if ("4-3-3".equals(selectedForm)) newF = new Formation433();
                    else if ("3-4-3".equals(selectedForm)) newF = new Formation343();
                    else if ("5-3-2".equals(selectedForm)) newF = new Formation532();
                    else newF = new Formation442();

                    currentSetup = currentSetup.withFormation(newF);
                    matchLogArea.append("[" + (isHome ? "HOME" : "AWAY") + " TACTIC] Formation changed to " + selectedForm + "\n");
                }

                String selectedStance = (String) stanceBox.getSelectedItem();
                if (!"Current".equals(selectedStance)) {
                    Stance newS;
                    if ("Park the Bus".equals(selectedStance)) newS = Stance.PARK_THE_BUS;
                    else if ("All Out Attack".equals(selectedStance)) newS = Stance.ALL_OUT_ATTACK;
                    else newS = Stance.NORMAL;

                    currentSetup = currentSetup.withStance(newS);
                    matchLogArea.append("[" + (isHome ? "HOME" : "AWAY") + " TACTIC] Stance changed to " + selectedStance + "\n");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "ปรับแผนไม่สำเร็จ: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        Set<Player> usedInSubs = new HashSet<>();

        for (int round = 1; round <= 3; round++) {
            int remaining = currentSetup.getRemainingSubstitutions();
            if (remaining <= 0) break;

            List<Player> availableStarters = new ArrayList<>();
            for (Player p : currentSetup.getStarters()) {
                if (!usedInSubs.contains(p)) availableStarters.add(p);
            }

            List<Player> availableBench = new ArrayList<>();
            for (Player p : currentSetup.getBench()) {
                if (!usedInSubs.contains(p)) availableBench.add(p);
            }

            if (availableStarters.isEmpty() || availableBench.isEmpty()) break;

            JPanel subPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            subPanel.add(new JLabel("Starter to Out:"));
            JComboBox<Player> outBox = new JComboBox<>(availableStarters.toArray(new Player[0]));
            subPanel.add(outBox);

            subPanel.add(new JLabel("Bench to In:"));
            JComboBox<Player> inBox = new JComboBox<>(availableBench.toArray(new Player[0]));
            subPanel.add(inBox);

            Object[] options = {"Confirm Substitution", "Skip this round", "Finish Subs"};
            int choice = JOptionPane.showOptionDialog(this, subPanel,
                    titlePrefix + " (Sub Round " + round + "/3, Left: " + remaining + ")",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                Player outPlayer = (Player) outBox.getSelectedItem();
                Player inPlayer = (Player) inBox.getSelectedItem();
                try {
                    currentSetup = currentSetup.withSubstitution(outPlayer, inPlayer);
                    usedInSubs.add(outPlayer);
                    usedInSubs.add(inPlayer);
                    matchLogArea.append("[" + (isHome ? "HOME" : "AWAY") + " SUB] Out: " + outPlayer.getName() + " | In: " + inPlayer.getName() + "\n");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "เปลี่ยนตัวไม่สำเร็จ: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    round--;
                }
            } else if (choice == 1) {
                continue;
            } else {
                break;
            }
        }

        if (isHome) {
            homeTeamSetup = currentSetup;
        } else {
            awayTeamSetup = currentSetup;
        }
    }

    private void runPenaltyShootout() {
        int homePenGoals = 0;
        int awayPenGoals = 0;
        String[] directions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center"};

        for (int round = 1; round <= 5; round++) {
            String homeDir = (String) JOptionPane.showInputDialog(this,
                    "Penalty Round " + round + " - Select Shot Direction (Home Team):",
                    "Penalty Shootout", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
            if (homeDir == null) homeDir = directions[0];

            String aiAwaySave = directions[(int)(Math.random() * directions.length)];
            boolean homeScored = !homeDir.equals(aiAwaySave);
            if (homeScored) homePenGoals++;

            matchLogArea.append(String.format("[PEN] Home shot %s -> %s!\n", homeDir, homeScored ? "GOAL ⚽" : "SAVED ❌"));

            String homeSaveDir = (String) JOptionPane.showInputDialog(this,
                    "Penalty Round " + round + " - Select Goalkeeper Dive Direction (Home GK):",
                    "Penalty Shootout", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
            if (homeSaveDir == null) homeSaveDir = directions[0];

            String awayDir = directions[(int)(Math.random() * directions.length)];
            boolean awayScored = !awayDir.equals(homeSaveDir);
            if (awayScored) awayPenGoals++;

            matchLogArea.append(String.format("[PEN] Away shot %s -> %s!\n", awayDir, awayScored ? "GOAL ⚽" : "SAVED ❌"));

            matchLogArea.append(String.format("Pen Score -> Home %d : %d Away\n\n", homePenGoals, awayPenGoals));
        }

        String penResult = homePenGoals > awayPenGoals ? "HOME WIN ON PENALTIES!" : (awayPenGoals > homePenGoals ? "AWAY WIN ON PENALTIES!" : "MATCH DRAWN!");
        matchLogArea.append("=== PENALTY SHOOTOUT ENDED ===\n" + penResult + "\n");
        JOptionPane.showMessageDialog(this, penResult, "Penalty Shootout Result", JOptionPane.INFORMATION_MESSAGE);
    }

    private String getRandomScorer(String teamName) {
        try {
            List<Player> lineup = teamName.toLowerCase().contains("away") ?
                    awayTeamSetup.getStarters() : homeTeamSetup.getStarters();

            if (lineup != null && !lineup.isEmpty()) {
                Player scorer = lineup.get((int) (Math.random() * lineup.size()));
                return scorer.getName();
            }
        } catch (Exception ignored) {}
        return "Super Sub";
    }

    /**
     * แสดง Popup ภาพรวม Lineup 11 ตัวจริงพร้อมรูปนักเตะก่อนเริ่มเกม / ก่อนเริ่มครึ่งหลัง
     */
    private void showLineupPopup(String stageTitle, TeamSetup setup) {
        JDialog popup = new JDialog(this, stageTitle, true);
        popup.setSize(650, 500);
        popup.setLocationRelativeTo(this);
        popup.setLayout(new BorderLayout(10, 10));

        // Header
        JLabel headerLabel = new JLabel(stageTitle + " - " + setup.getFormation().getName() + " (" + setup.getStance() + ")", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        popup.add(headerLabel, BorderLayout.NORTH);

        // Grid แสดงนักเตะ 11 คน (แยกเป็น Card เล็กๆ มีรูป + ชื่อ + ตำแหน่ง)
        JPanel pitchGridPanel = new JPanel(new GridLayout(3, 4, 8, 8)); // จัดเรียงแบบ Grid 3x4
        pitchGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        pitchGridPanel.setBackground(new Color(34, 139, 34)); // สีเขียวหญ้าสนามฟุตบอล

        for (Player p : setup.getStarters()) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(255, 255, 255, 220));
            card.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            // โหลดรูปนักเตะ (ถ้าไม่มีรูปเฉพาะคน จะใช้รูป Default / Avatar แทน)
            JLabel imgLabel = new JLabel();
            imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
            try {
                // สมมติไฟล์รูปชื่อเดียวกับนักเตะ เช่น images/Messy.png
                ImageIcon icon = new ImageIcon("images/" + p.getName() + ".png");
                Image img = icon.getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
                imgLabel.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                imgLabel.setText("👤"); // แสดง emoji อวตารแทนถ้ารูปหาไม่เจอ
                imgLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
            }

            JLabel infoLabel = new JLabel("<html><center><b>" + p.getName() + "</b><br><font size='2' color='blue'>" + p.getPosition() + "</font></center></html>", SwingConstants.CENTER);

            card.add(imgLabel, BorderLayout.CENTER);
            card.add(infoLabel, BorderLayout.SOUTH);

            pitchGridPanel.add(card);
        }

        popup.add(new JScrollPane(pitchGridPanel), BorderLayout.CENTER);

        // ปุ่มปิดเพื่อลุยต่อ
        JButton readyBtn = new JButton("Ready to Play! ⚽");
        readyBtn.setFont(new Font("Arial", Font.BOLD, 14));
        readyBtn.addActionListener(e -> popup.dispose());
        popup.add(readyBtn, BorderLayout.SOUTH);

        popup.setVisible(true);
    }
}