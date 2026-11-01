package football.ui;

import football.logic.Match;
import football.logic.MatchStatistics;
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
    private final List<MatchEvent> allMatchEvents = new ArrayList<>();

    private int liveHomeGoals = 0;
    private int liveAwayGoals = 0;

    public MainFrame() {
        setTitle("Football Manager - Ultimate Edition 2026");
        setSize(1200, 820);
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

        List<Player> gks = new ArrayList<>();
        List<Player> outfields = new ArrayList<>();
        for (Player p : all) {
            if (p.getPosition() == Position.GK) {
                gks.add(p);
            } else {
                outfields.add(p);
            }
        }

        Collections.shuffle(gks);
        Collections.shuffle(outfields);

        poolPlayersHome = new ArrayList<>();
        poolPlayersAway = new ArrayList<>();

        if (!gks.isEmpty()) poolPlayersHome.add(gks.remove(0));
        if (isHotseatMode && !gks.isEmpty()) poolPlayersAway.add(gks.remove(0));

        outfields.addAll(gks);
        Collections.shuffle(outfields);

        while (poolPlayersHome.size() < 20 && !outfields.isEmpty()) {
            poolPlayersHome.add(outfields.remove(0));
        }

        if (isHotseatMode) {
            while (poolPlayersAway.size() < 20 && !outfields.isEmpty()) {
                poolPlayersAway.add(outfields.remove(0));
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
            List<Player> allAvailable = new ArrayList<>();
            for (int i = 0; i < poolListModel.size(); i++) allAvailable.add(poolListModel.getElementAt(i));
            for (int i = 0; i < starterListModel.size(); i++) allAvailable.add(starterListModel.getElementAt(i));
            for (int i = 0; i < benchListModel.size(); i++) allAvailable.add(benchListModel.getElementAt(i));

            List<Player> gks = new ArrayList<>();
            List<Player> outfields = new ArrayList<>();

            for (Player p : allAvailable) {
                if (p.getPosition() == Position.GK) gks.add(p);
                else outfields.add(p);
            }

            if (gks.isEmpty()) {
                JOptionPane.showMessageDialog(this, "⚠️ ไม่พบผู้รักษาประตู (GK) ใน Pool ชุดนี้!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Collections.shuffle(gks);
            Player starterGK = gks.remove(0);

            Collections.shuffle(outfields);
            starterListModel.clear();
            benchListModel.clear();
            poolListModel.clear();

            starterListModel.addElement(starterGK);

            for (int i = 0; i < 10 && !outfields.isEmpty(); i++) {
                starterListModel.addElement(outfields.remove(0));
            }

            outfields.addAll(gks);
            Collections.shuffle(outfields);

            for (int i = 0; i < 5 && !outfields.isEmpty(); i++) {
                benchListModel.addElement(outfields.remove(0));
            }

            for (Player p : outfields) {
                poolListModel.addElement(p);
            }

            updateCounts();
            JOptionPane.showMessageDialog(this, "จัดทีมแบบ Auto สำเร็จเรียบร้อย! (การันตีโกลตัวจริง 1 คน)");
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
                        // สร้างทีม Home ของผู้เล่น
                        homeTeamSetup = TeamSetup.create(tactic, form, Stance.NORMAL, starters, bench);

                        // บังคับการสุ่มทีมของ AI ให้มีโกล 1 คน
                        List<Player> all = PlayerRepository.loadPlayersFromCSV("players.csv");
                        List<Player> aiGks = new ArrayList<>();
                        List<Player> aiOutfields = new ArrayList<>();

                        for (Player p : all) {
                            if (p.getPosition() == Position.GK) aiGks.add(p);
                            else aiOutfields.add(p);
                        }

                        Collections.shuffle(aiGks);
                        Collections.shuffle(aiOutfields);

                        List<Player> aiStarters = new ArrayList<>();
                        List<Player> aiBench = new ArrayList<>();

                        if (!aiGks.isEmpty()) aiStarters.add(aiGks.remove(0));

                        for (int i = 0; i < 10 && !aiOutfields.isEmpty(); i++) {
                            aiStarters.add(aiOutfields.remove(0));
                        }

                        aiOutfields.addAll(aiGks);
                        Collections.shuffle(aiOutfields);

                        for (int i = 0; i < 5 && !aiOutfields.isEmpty(); i++) {
                            aiBench.add(aiOutfields.remove(0));
                        }

                        awayTeamSetup = TeamSetup.create(new Possession(), new Formation442(), Stance.NORMAL, aiStarters, aiBench);
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

        allMatchEvents.clear();
        allMatchEvents.addAll(h1Events);
        allMatchEvents.addAll(h2Events);

        currentEventIndex = 0;
        isSecondHalfRunning = false;
        liveHomeGoals = 0;
        liveAwayGoals = 0;

        liveTimer = new Timer(2000, null);
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
                        JOptionPane.showMessageDialog(this, "หมดเวลาการแข่งขันเสมอกัน! ต้องตัดสินผู้ชนะด้วยการดวลจุดโทษ", "Full Time - Draw", JOptionPane.INFORMATION_MESSAGE);
                        runPenaltyShootout();
                    } else {
                        showPostMatchSummary();
                    }
                }
            }
        });
        liveTimer.start();
    }

    private void showPostMatchSummary() {
        MatchStatistics stats = MatchStatistics.calculate(allMatchEvents);
        MatchSummaryDialog summaryDialog = new MatchSummaryDialog(
                this, "Home", "Away", liveHomeGoals, liveAwayGoals, stats
        );
        summaryDialog.setVisible(true);
    }

    private void runPenaltyShootout() {
        matchLogArea.append("\n=== PENALTY SHOOTOUT ===\n");
        int homePenGoals = 0;
        int awayPenGoals = 0;
        int rounds = 5;

        for (int i = 1; i <= rounds; i++) {
            boolean homeScored = takePenaltyForShootout(true, i);
            if (homeScored) homePenGoals++;

            boolean awayScored = takePenaltyForShootout(false, i);
            if (awayScored) awayPenGoals++;

            matchLogArea.append(String.format("Round %d: Home %d - %d Away\n\n", i, homePenGoals, awayPenGoals));
        }

        int round = 6;
        while (homePenGoals == awayPenGoals) {
            matchLogArea.append("--- SUDDEN DEATH ---\n");
            boolean homeScored = takePenaltyForShootout(true, round);
            if (homeScored) homePenGoals++;

            boolean awayScored = takePenaltyForShootout(false, round);
            if (awayScored) awayPenGoals++;

            matchLogArea.append(String.format("Round %d: Home %d - %d Away\n\n", round, homePenGoals, awayPenGoals));
            round++;
        }

        matchLogArea.append("=== MATCH FINISHED ===\n");
        matchLogArea.append(String.format("🏆 WINNER: %s (Penalty Score: %d - %d)\n",
                (homePenGoals > awayPenGoals ? "HOME" : "AWAY"), homePenGoals, awayPenGoals));

        JOptionPane.showMessageDialog(this, String.format("ทีม %s ชนะการดวลจุดโทษ! (%d - %d)",
                (homePenGoals > awayPenGoals ? "HOME" : "AWAY"), homePenGoals, awayPenGoals), "Match Result", JOptionPane.INFORMATION_MESSAGE);

        showPostMatchSummary();
    }

    private boolean takePenaltyForShootout(boolean isHomeShooting, int round) {
        String[] directions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center"};
        TeamSetup currentSetup = isHomeShooting ? homeTeamSetup : awayTeamSetup;
        Player shooter = getRandomScorer(currentSetup);
        String shooterName = (shooter != null) ? shooter.getName() : "Player";

        if (isHomeShooting) {
            String shootDir = (String) JOptionPane.showInputDialog(this,
                    "[SHOOTOUT Round " + round + "] " + shooterName + " (HOME) ยิงจุดโทษ! เลือกทิศทางยิง:",
                    "Penalty Shootout - Shoot", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
            if (shootDir == null) shootDir = directions[0];
            String aiSaveDir = directions[(int)(Math.random() * directions.length)];

            boolean scored = !shootDir.equals(aiSaveDir);
            matchLogArea.append(shooterName + " (Home) shoots " + shootDir + "... " + (scored ? "GOAL! ⚽" : "SAVED! ❌") + "\n");
            return scored;
        } else {
            if (isHotseatMode) {
                String shootDir = (String) JOptionPane.showInputDialog(this,
                        "[SHOOTOUT Round " + round + "] " + shooterName + " (AWAY) ยิงจุดโทษ! เลือกทิศทางยิง:",
                        "Penalty Shootout - Shoot", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
                if (shootDir == null) shootDir = directions[0];
                String aiSaveDir = directions[(int)(Math.random() * directions.length)];
                boolean scored = !shootDir.equals(aiSaveDir);
                matchLogArea.append(shooterName + " (Away) shoots " + shootDir + "... " + (scored ? "GOAL! ⚽" : "SAVED! ❌") + "\n");
                return scored;
            } else {
                String saveDir = (String) JOptionPane.showInputDialog(this,
                        "[SHOOTOUT Round " + round + "] " + shooterName + " (AWAY) ยิงจุดโทษ! เลือกทิศทางพุ่งเซฟ:",
                        "Penalty Shootout - Defend", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
                if (saveDir == null) saveDir = directions[0];
                String aiShootDir = directions[(int)(Math.random() * directions.length)];

                boolean saved = saveDir.equals(aiShootDir);
                matchLogArea.append(shooterName + " (Away) shoots " + aiShootDir + "... " + (!saved ? "GOAL! ⚽" : "SAVED! ❌") + "\n");
                return !saved;
            }
        }
    }

    private void processAndCheckPenaltyEvent(MatchEvent ev) {
        String desc = ev.getDescription();
        String teamName = ev.getTeamName();
        boolean isHomeTeamEvent = teamName.toLowerCase().contains("home");
        TeamSetup currentSetup = isHomeTeamEvent ? homeTeamSetup : awayTeamSetup;

        if (desc.contains("MATCH FORFEITED")) {
            liveTimer.stop();
            matchLogArea.append(String.format("[%d'] 🚨 %s\n", ev.getMinute(), desc));
            JOptionPane.showMessageDialog(this, desc, "Match Abandoned - Forfeit Penalty", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (desc.toLowerCase().contains("penalty")) {
            liveTimer.stop();

            String[] directions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right", "Center"};
            Player shooter = getRandomScorer(currentSetup);
            String shooterName = (shooter != null) ? shooter.getName() : "Unknown Player";

            if (isHomeTeamEvent) {
                String shootDir = (String) JOptionPane.showInputDialog(this,
                        "🚨 [IN-MATCH PENALTY] " + shooterName + " รับหน้าที่สังหารจุดโทษ! เลือกทิศทางยิง:",
                        "Penalty Kick", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
                if (shootDir == null) shootDir = directions[0];

                String aiSaveDir = directions[(int)(Math.random() * directions.length)];
                boolean scored = !shootDir.equals(aiSaveDir);

                if (scored) {
                    liveHomeGoals++;
                    desc = "Penalty goal scored by " + shooterName + "! [Shot: " + shootDir + "] ⚽";
                } else {
                    desc = "Penalty missed by " + shooterName + " (Saved)! [Shot: " + shootDir + "] ❌";
                }
            } else {
                String saveDir = (String) JOptionPane.showInputDialog(this,
                        "⚠️ [IN-MATCH PENALTY] " + shooterName + " (ทีมเยือน) รับหน้าที่สังหารจุดโทษ! เลือกทิศทางพุ่งเซฟ:",
                        "Defend Penalty", JOptionPane.QUESTION_MESSAGE, null, directions, directions[0]);
                if (saveDir == null) saveDir = directions[0];

                String aiShootDir = directions[(int)(Math.random() * directions.length)];
                boolean saved = saveDir.equals(aiShootDir);

                if (saved) {
                    desc = "Penalty saved brilliantly! Our GK denied " + shooterName + "! [Dive: " + saveDir + "] 🛡️";
                } else {
                    liveAwayGoals++;
                    desc = "Penalty goal scored by " + shooterName + " (Opponent)! [Shot: " + aiShootDir + "] ⚽";
                }
            }
            liveTimer.start();
        }
        else if ((desc.contains("GOAL") || desc.toLowerCase().contains("goal") || desc.contains("scores"))
                && !desc.toLowerCase().contains("save")) {
            if (isHomeTeamEvent) liveHomeGoals++;
            else liveAwayGoals++;

            String scorerName = "";
            if (ev.getPlayer() != null) {
                scorerName = ev.getPlayer().getName();
            } else {
                Player randomShooter = getRandomScorer(currentSetup);
                if (randomShooter != null) scorerName = randomShooter.getName();
            }

            desc = desc + " by " + scorerName + " ⚽";
        }
        else if (desc.contains("Yellow card")) {
            liveTimer.stop();
            matchLogArea.append(String.format("[%d'] %s: %s\n", ev.getMinute(), teamName, desc));
            boolean canSubstitute = isHomeTeamEvent || isHotseatMode;
            if (canSubstitute && currentSetup.getRemainingSubstitutions() > 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "🟨 มีผู้เล่นได้รับใบเหลือง (" + teamName + ")!\nคุณต้องการเปลี่ยนตัวแก้เกมทันทีเลยหรือไม่? (เหลือเปลี่ยนตัวอีก "
                                + currentSetup.getRemainingSubstitutions() + " คน)",
                        "Emergency Substitution Alert",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    runEmergencySubstitution(isHomeTeamEvent);
                }
            }
            liveTimer.start();
            return;
        }

        matchLogArea.append(String.format("[%d'] %s: %s\n", ev.getMinute(), teamName, desc));
    }

    private void runEmergencySubstitution(boolean isHome) {
        TeamSetup currentSetup = isHome ? homeTeamSetup : awayTeamSetup;
        List<Player> availableStarters = currentSetup.getStarters();
        List<Player> availableBench = currentSetup.getBench();

        if (availableStarters.isEmpty() || availableBench.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ไม่สามารถเปลี่ยนตัวได้ (ไม่มีผู้เล่นในตัวสำรอง)");
            return;
        }

        JPanel subPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        subPanel.add(new JLabel("เลือกตัวจริงที่จะเปลี่ยนออก:"));
        JComboBox<Player> outBox = new JComboBox<>(availableStarters.toArray(new Player[0]));
        subPanel.add(outBox);

        subPanel.add(new JLabel("เลือกตัวสำรองที่จะส่งลงแทน:"));
        JComboBox<Player> inBox = new JComboBox<>(availableBench.toArray(new Player[0]));
        subPanel.add(inBox);

        int result = JOptionPane.showConfirmDialog(this, subPanel,
                "Emergency Substitution (" + (isHome ? "HOME" : "AWAY") + ")",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            Player outPlayer = (Player) outBox.getSelectedItem();
            Player inPlayer = (Player) inBox.getSelectedItem();

            try {
                TeamSetup updatedSetup = currentSetup.withSubstitution(outPlayer, inPlayer);
                if (isHome) {
                    homeTeamSetup = updatedSetup;
                } else {
                    awayTeamSetup = updatedSetup;
                }
                matchLogArea.append(String.format("🚨 [EMERGENCY SUB - %s] Out: %s | In: %s\n",
                        isHome ? "HOME" : "AWAY", outPlayer.getName(), inPlayer.getName()));
                JOptionPane.showMessageDialog(this, "เปลี่ยนตัวเรียบร้อยแล้ว!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "เปลี่ยนตัวไม่สำเร็จ: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSubstitutionDialog() {
        homeTeamSetup = homeTeamSetup.endFirstHalf();
        awayTeamSetup = awayTeamSetup.endFirstHalf();

        runTeamSubstitutionProcess("Player 1 (HOME) - Half-Time Adjustment", true);

        if (isHotseatMode) {
            runTeamSubstitutionProcess("Player 2 (AWAY) - Half-Time Adjustment", false);
        } else {
            matchLogArea.append("[SUB] AI (Away) kept their squad.\n");
        }

        JOptionPane.showMessageDialog(this, "สิ้นสุดการพักครึ่ง ลุยต่อครึ่งหลังกันเลย!");
    }

    private void runTeamSubstitutionProcess(String titlePrefix, boolean isHome) {
        TeamSetup currentSetup = isHome ? homeTeamSetup : awayTeamSetup;

        List<Player> currentStarters = currentSetup.getStarters();
        List<Player> sortedByFatigue = new ArrayList<>(currentStarters);
        sortedByFatigue.sort((p1, p2) -> Integer.compare(p1.getStamina(), p2.getStamina()));

        StringBuilder recMsg = new StringBuilder("💡 คำแนะนำพักครึ่งสำหรับ " + (isHome ? "Player 1 (HOME)" : "Player 2 (AWAY)") + "\n");
        recMsg.append("(5 อันดับตัวจริงที่ล้าที่สุดและควรพิจารณาเปลี่ยนออก):\n\n");

        int count = Math.min(5, sortedByFatigue.size());
        for (int i = 0; i < count; i++) {
            Player p = sortedByFatigue.get(i);
            recMsg.append(String.format("%d. %s [%s] - Stamina: %d\n",
                    (i + 1), p.getName(), p.getPosition(), p.getStamina()));
        }

        JOptionPane.showMessageDialog(this, recMsg.toString(), titlePrefix + " - Recommendation", JOptionPane.INFORMATION_MESSAGE);
        matchLogArea.append("\n" + recMsg.toString() + "\n");

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

            int subChoice = JOptionPane.showConfirmDialog(this, subPanel,
                    titlePrefix + " - Substitution (Remaining: " + remaining + ")",
                    JOptionPane.OK_CANCEL_OPTION);

            if (subChoice == JOptionPane.OK_OPTION) {
                Player outP = (Player) outBox.getSelectedItem();
                Player inP = (Player) inBox.getSelectedItem();

                try {
                    currentSetup = currentSetup.withSubstitution(outP, inP);
                    usedInSubs.add(outP);
                    usedInSubs.add(inP);

                    matchLogArea.append(String.format("[%s SUB] Out: %s | In: %s\n",
                            isHome ? "HOME" : "AWAY", outP.getName(), inP.getName()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "เปลี่ยนตัวไม่สำเร็จ: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                break;
            }
        }

        if (isHome) homeTeamSetup = currentSetup;
        else awayTeamSetup = currentSetup;
    }

    private Player getRandomScorer(TeamSetup teamSetup) {
        List<Player> starters = teamSetup.getStarters();
        if (starters == null || starters.isEmpty()) return null;

        int totalWeight = 0;
        List<Integer> weights = new ArrayList<>();

        for (Player p : starters) {
            int weight = 1;
            if (p.getPosition() != null) {
                switch (p.getPosition()) {
                    case STRIKER: weight = 100; break;
                    case FRONTWING: weight = 70; break;
                    case MIDWING: case MIDFIELD: weight = 40; break;
                    case FULLBACK: weight = 15; break;
                    case CENTER_BACK: weight = 5; break;
                    case GK: weight = 1; break;
                }
            }
            weights.add(weight);
            totalWeight += weight;
        }

        int randomValue = (int) (Math.random() * totalWeight);
        int currentSum = 0;

        for (int i = 0; i < starters.size(); i++) {
            currentSum += weights.get(i);
            if (randomValue < currentSum) {
                return starters.get(i);
            }
        }
        return starters.get(0);
    }
}