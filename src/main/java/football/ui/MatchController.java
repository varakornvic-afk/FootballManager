package football.ui;

import football.event.MatchEvent;
import football.event.*;
import football.logic.*;
import football.model.*;
import football.util.PlayerImageResolver;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.*;
import java.util.List;

/** ควบคุมการจำลองแมตช์: timer, ครึ่งแรก/ครึ่งหลัง, จัดการ event ต่าง ๆ */
public class MatchController {
    private final Stage parent;
    private final MatchPanel view;
    private final PopupManager popups;

    private TeamSetup homeTeamSetup, awayTeamSetup;
    // ทีมตอนเริ่มแมตช์ (ไม่โดนแก้ระหว่างเกม) ใช้รีเซ็ตเมื่อกด Play Match ซ้ำ
    private TeamSetup homeOriginalSetup, awayOriginalSetup;
    private Match currentMatch;
    private UiTimer liveTimer;
    private int currentEventIndex = 0;
    private boolean isSecondHalfRunning = false;
    private List<MatchEvent> h1Events, h2Events;
    private final List<MatchEvent> allMatchEvents = new ArrayList<>();
    private int liveHomeGoals = 0, liveAwayGoals = 0;

    private int homeSubsUsed = 0;
    private int awaySubsUsed = 0;

    private final Set<String> yellowCardPlayers = new HashSet<>();
    private final Set<Player> redCardedPlayers = new HashSet<>();
    // ทุกคนที่เคยถูกเปลี่ยนออก (ไม่ว่าครึ่งแรก/พักครึ่ง/ใบเหลือง) ห้ามเปลี่ยนกลับเข้ามาอีก
    private final Set<Player> subbedOutPlayers = new HashSet<>();

    public MatchController(Stage parent, MatchPanel view) {
        this.parent = parent;
        this.view = view;
        this.popups = new PopupManager(parent, () -> liveTimer);
    }

    public void setTeams(TeamSetup home, TeamSetup away) {
        this.homeOriginalSetup = home;
        this.awayOriginalSetup = away;
        this.homeTeamSetup = home;
        this.awayTeamSetup = away;
        stop();                  // กันแมตช์เก่ายังรันอยู่
        view.resetToPreMatch();  // กลับไปใช้ภาพก่อนเริ่ม ล้าง log และสกอร์
    }

    /** เริ่มแมตช์ใหม่ด้วยทีมตั้งต้น: ยกเลิกใบแดง/การเปลี่ยนตัวจากแมตช์ก่อน และคืน stamina เต็ม */
    private void resetTeamsForNewMatch() {
        homeTeamSetup = homeOriginalSetup;
        awayTeamSetup = awayOriginalSetup;
        restoreStamina(homeTeamSetup);
        restoreStamina(awayTeamSetup);
    }

    /** คนที่ห้ามส่งลงสนามของทีมเหย้า = โดนใบแดง + เคยถูกเปลี่ยนออกไปแล้ว */
    private Set<Player> unavailableForHome() {
        Set<Player> unavailable = new HashSet<>(redCardedPlayers);
        unavailable.addAll(subbedOutPlayers);
        return unavailable;
    }

    /** จดผู้เล่นที่หายไปจากตัวจริงว่าถูกเปลี่ยนออกแล้ว */
    private void recordSubbedOut(List<Player> before, List<Player> after) {
        for (Player p : before) {
            if (!after.contains(p)) subbedOutPlayers.add(p);
        }
    }

    private void restoreStamina(TeamSetup team) {
        if (team == null) return;
        for (Player p : team.getStarters()) p.setStamina(p.getMaxStamina());
        for (Player p : team.getBench()) p.setStamina(p.getMaxStamina());
    }

    public void stop() {
        if (liveTimer != null) liveTimer.stop();
    }

    // MATCH FLOW

    public void runMatchSimulation() {
        SoundManager.stopMenuMusic(); // เริ่มแข่งแล้ว ตัดเพลงให้เสียงเหตุการณ์ในเกมชัด
        if (liveTimer != null && liveTimer.isRunning()) liveTimer.stop();
        resetTeamsForNewMatch();
        view.clearLog();
        view.showMatchBackground();
        appendColoredLog("=== KICK OFF (FIRST HALF) ===");
        appendColoredLog("");

        yellowCardPlayers.clear();
        redCardedPlayers.clear();
        subbedOutPlayers.clear();
        allMatchEvents.clear();
        liveHomeGoals = 0;
        liveAwayGoals = 0;
        homeSubsUsed = 0;
        awaySubsUsed = 0;
        updateScoreBoard(0);

        currentMatch = new Match(homeTeamSetup, awayTeamSetup, System.currentTimeMillis());
        h1Events = currentMatch.playFirstHalf().getEvents();
        if (h1Events != null) allMatchEvents.addAll(h1Events);

        currentEventIndex = 0;
        isSecondHalfRunning = false;

        liveTimer = new UiTimer(1800, () -> {
            List<MatchEvent> currentList = isSecondHalfRunning ? h2Events : h1Events;

            if (currentList != null && currentEventIndex < currentList.size()) {
                processEvent(currentList.get(currentEventIndex++));
            } else if (!isSecondHalfRunning) {
                handleHalfTime();
            } else {
                handleFullTime();
            }
        });
        liveTimer.start();
    }

    private void handleHalfTime() {
        liveTimer.stop();
        appendColoredLog("");
        appendColoredLog("--- HALF TIME ---");
        appendColoredLog("");
        SoundManager.play(SoundManager.WHISTLE_HALFTIME);

        if (homeTeamSetup != null && homeTeamSetup.getBench() != null) {
            for (Player p : homeTeamSetup.getBench()) p.setStamina(p.getMaxStamina());
        }
        if (awayTeamSetup != null && awayTeamSetup.getBench() != null) {
            for (Player p : awayTeamSetup.getBench()) p.setStamina(p.getMaxStamina());
        }

        // บันทึก 11 ตัวจริงก่อนพักครึ่ง เพื่อเปรียบเทียบการเปลี่ยนตัวช่วงพักครึ่ง
        List<Player> oldHomeStarters = new ArrayList<>(homeTeamSetup.getStarters());
        List<Player> oldAwayStarters = new ArrayList<>(awayTeamSetup.getStarters());

        awayTeamSetup = AwayTeamAI.adaptAtHalftime(awayTeamSetup, liveHomeGoals, liveAwayGoals, Math.max(0, 3 - awaySubsUsed));
        currentMatch.setAwayTeam(awayTeamSetup);
        // นับการเปลี่ยนตัวช่วงพักครึ่งของทีมเยือนเข้าโควตา 3 ตัวด้วย
        awaySubsUsed += SubstitutionDetector.detect(oldAwayStarters, awayTeamSetup.getStarters(), false).size();
        recordSubbedOut(oldAwayStarters, awayTeamSetup.getStarters());

        HalfTimeDialog htDialog = new HalfTimeDialog(parent, homeTeamSetup, awayTeamSetup, yellowCardPlayers, unavailableForHome(), homeSubsUsed);
        htDialog.setOnCloseRequest(e -> e.consume()); // ปิดด้วยปุ่ม X ไม่ได้ ต้องกดปุ่มในหน้าต่าง
        htDialog.showAndWait();

        if (htDialog.isConfirmed()) {
            homeSubsUsed = htDialog.getUpdatedSubsUsed();
            homeTeamSetup = htDialog.getUpdatedTeamSetup();
            currentMatch.setHomeTeam(homeTeamSetup);
            recordSubbedOut(oldHomeStarters, homeTeamSetup.getStarters());

            h2Events = currentMatch.playSecondHalf().getEvents();
            if (h2Events != null) allMatchEvents.addAll(h2Events);

            isSecondHalfRunning = true;
            currentEventIndex = 0;

            // คำนวณรายชื่อเฉพาะที่ถูกเปลี่ยนตัว "ช่วงพักครึ่ง" เท่านั้น
            List<SubInfo> htSubs = new ArrayList<>();
            htSubs.addAll(SubstitutionDetector.detect(oldHomeStarters, homeTeamSetup.getStarters(), true));
            htSubs.addAll(SubstitutionDetector.detect(oldAwayStarters, awayTeamSetup.getStarters(), false));

            // เล่นคิว Pop-up 2-in-1 ทีละ 2 วิ (Home ก่อนแล้วตามด้วย Away) จากนั้นค่อย Kick-off
            playHalftimeSubPopupsThenStartSecondHalf(htSubs);
        }
    }

    private void handleFullTime() {
        liveTimer.stop();
        appendColoredLog("");
        appendColoredLog(String.format("=== FULL TIME: Home %d - %d Away ===", liveHomeGoals, liveAwayGoals));
        SoundManager.play(SoundManager.WHISTLE_FULLTIME);
        MatchStatistics stats = MatchStatistics.calculate(allMatchEvents);
        new MatchSummaryDialog(parent, "Home", "Away", liveHomeGoals, liveAwayGoals, stats).showAndWait();
    }

    // เล่นคิว Pop-up การเปลี่ยนตัวช่วงพักครึ่งแบบซีเควนซ์ (2 วินาทีต่อ 1 การเปลี่ยนตัว)
    private void playHalftimeSubPopupsThenStartSecondHalf(List<SubInfo> subs) {
        playSubQueue(subs, 0, "Halftime Sub", () -> {
            appendColoredLog("=== SECOND HALF ===");
            appendColoredLog("");
            if (liveTimer != null) liveTimer.start();
        });
    }

    private void playSubQueue(List<SubInfo> subs, int index, String tag, Runnable onComplete) {
        if (index >= subs.size()) {
            onComplete.run();
            return;
        }

        SubInfo sub = subs.get(index);
        String teamTag = sub.isHome ? "Home" : "Away";
        appendColoredLog(String.format("🔄 [%s] %s substituted %s OUT ⚽ %s IN", tag, teamTag, sub.playerOut, sub.playerIn));

        // แสดง Pop-up 2-in-1
        popups.showEventPopupAndWait(new GameEvent.SubstitutionEvent(sub.playerOut, sub.playerIn, PlayerImageResolver.getPlayerImagePath(sub.playerIn), sub.isHome), sub.isHome, false);

        UiTimer timer = new UiTimer(2000, () -> {
            popups.closeCurrentPopup();
            playSubQueue(subs, index + 1, tag, onComplete);
        });
        timer.setRepeats(false);
        timer.start();
    }

    // EVENT HANDLING

    private void processEvent(MatchEvent ev) {
        StaminaManager.updatePlayersStamina(ev, homeTeamSetup, awayTeamSetup);

        String desc = ev.getDescription();
        String teamName = ev.getTeamName();
        boolean isHome = teamName != null && teamName.toLowerCase().contains("home");
        String descLower = desc.toLowerCase();

        String playerName = ev.getPlayer() != null ? ev.getPlayer().getName() : "Player";
        String formattedLog = null;

        updateScoreBoard(ev.getMinute());

        // 1. จุดโทษ (Penalty Kick)
        if (ev.getEventType() == MatchEvent.EventType.PENALTY || desc.contains("Penalty") || descLower.contains("penalty")) {
            handlePenalty(ev, teamName, playerName, isHome);
            return;
        }
        // 2. ทำประตู (Goal)
        else if (ev.getEventType() == MatchEvent.EventType.GOAL || (descLower.contains("goal") && !descLower.contains("save"))) {
            formattedLog = handleGoal(ev, desc, teamName, playerName, isHome);
        }
        // 3. เซฟลูกยิง (Save)
        else if (ev.getEventType() == MatchEvent.EventType.SAVE || descLower.contains("save") || descLower.contains("saved")) {
            formattedLog = handleSave(ev, desc, teamName, isHome);
        }
        // 4. ใบแดง (Red Card)
        else if (ev.getEventType() == MatchEvent.EventType.RED_CARD || desc.contains("Red card") || desc.contains("RED CARD")) {
            formattedLog = handleRedCard(ev, desc, teamName, playerName, isHome);
        }
        // 5. ใบเหลือง (Yellow Card)
        else if (ev.getEventType() == MatchEvent.EventType.YELLOW_CARD || desc.contains("Yellow card") || desc.contains("YELLOW CARD")) {
            handleYellowCard(ev, teamName, playerName, isHome);
            return;
        }
        // 6. ฟาวล์ทั่วไป (Foul)
        else if (ev.getEventType() == MatchEvent.EventType.FOUL || descLower.contains("foul")) {
            formattedLog = String.format("[%d'] %s: Foul by %s 🛑", ev.getMinute(), teamName, playerName);
        }
        else {
            formattedLog = String.format("[%d'] %s: %s", ev.getMinute(), teamName, desc);
        }

        if (formattedLog != null && !formattedLog.isEmpty()) {
            appendColoredLog(formattedLog);
        }
    }

    private void handlePenalty(MatchEvent ev, String teamName, String playerName, boolean isHome) {
        if (liveTimer != null) liveTimer.stop();

        String penaltyAwardLog = String.format("[%d'] %s: PENALTY KICK awarded! 🎯 (Taker: %s)", ev.getMinute(), teamName, playerName);
        appendColoredLog(penaltyAwardLog);
        SoundManager.play(isHome ? SoundManager.GOAL_CHEER : SoundManager.GOAL_CONCEDED, SoundManager.SOFT_VOLUME);
        popups.showPopup(new GameEvent.PenaltyEvent(playerName, PlayerImageResolver.getPlayerImagePath(playerName), isHome), isHome);

        UiTimer penaltyResultTimer = new UiTimer(2000, () -> {
            popups.closeCurrentPopup();

            boolean isGoal = Math.random() < 0.75;

            if (isGoal) {
                if (isHome) liveHomeGoals++; else liveAwayGoals++;
                updateScoreBoard(ev.getMinute());

                String goalLog = String.format("[%d'] %s: GOAL! Scored from the penalty spot by %s ⚽", ev.getMinute(), teamName, playerName);
                appendColoredLog(goalLog);
                SoundManager.play(isHome ? SoundManager.GOAL_CHEER : SoundManager.GOAL_CONCEDED);
                popups.showPopup(new GameEvent.GoalEvent(playerName, PlayerImageResolver.getPlayerImagePath(playerName), isHome), isHome);
            } else {
                TeamSetup defendingTeam = isHome ? awayTeamSetup : homeTeamSetup;
                String keeperName = findKeeperName(defendingTeam);
                String saveLog = String.format("[%d'] %s: Penalty shot by %s WAS SAVED by GK %s! 🧤", ev.getMinute(), teamName, playerName, keeperName);
                appendColoredLog(saveLog);
                // เราเซฟจุดโทษได้ = เชียร์เต็ม / จุดโทษของเราโดนเซฟ = โห่เบา
                if (isHome) SoundManager.play(SoundManager.GOAL_CONCEDED, SoundManager.SOFT_VOLUME);
                else SoundManager.play(SoundManager.GOAL_CHEER);
                popups.showPopup(new GameEvent.SaveEvent(keeperName, PlayerImageResolver.getPlayerImagePath(keeperName), !isHome), !isHome);
            }

            UiTimer autoCloseOutcomeTimer = new UiTimer(2000, () -> {
                popups.closeCurrentPopup();
                if (liveTimer != null) liveTimer.start();
            });
            autoCloseOutcomeTimer.setRepeats(false);
            autoCloseOutcomeTimer.start();
        });

        penaltyResultTimer.setRepeats(false);
        penaltyResultTimer.start();
    }

    private String handleGoal(MatchEvent ev, String desc, String teamName, String playerName, boolean isHome) {
        if (isHome) liveHomeGoals++; else liveAwayGoals++;
        updateScoreBoard(ev.getMinute());

        String log = String.format("[%d'] %s: %s", ev.getMinute(), teamName, desc.contains("Scored") ? desc : "GOAL! scored by " + playerName + " ⚽");
        SoundManager.play(isHome ? SoundManager.GOAL_CHEER : SoundManager.GOAL_CONCEDED);
        popups.showEventPopupAndWait(new GameEvent.GoalEvent(playerName, PlayerImageResolver.getPlayerImagePath(playerName), isHome), isHome, true);
        return log;
    }

    private String handleSave(MatchEvent ev, String desc, String teamName, boolean isHome) {
        boolean isAttackingHome = isHome;
        boolean isKeeperHome = !isAttackingHome;

        TeamSetup defendingTeam = isAttackingHome ? awayTeamSetup : homeTeamSetup;
        String keeperName = findKeeperName(defendingTeam);

        String log = String.format("[%d'] %s: %s", ev.getMinute(), teamName, desc.contains("saved") ? desc : "Shot saved by GK " + keeperName + "! 🧤");
        if (isKeeperHome) SoundManager.play(SoundManager.GOAL_CHEER, SoundManager.SOFT_VOLUME); // ผู้รักษาประตูเราเซฟได้ (ลูกยิงของเราโดนเซฟไม่มีเสียง)
        popups.showEventPopupAndWait(new GameEvent.SaveEvent(keeperName, PlayerImageResolver.getPlayerImagePath(keeperName), isKeeperHome), isKeeperHome, true);
        return log;
    }

    /** @return log ที่ต้องแสดง หรือ null ถ้าแมตช์ถูกปรับแพ้ (ไม่ต้องแสดง log ใบแดงซ้ำ) */
    private String handleRedCard(MatchEvent ev, String desc, String teamName, String playerName, boolean isHome) {
        String log = String.format("[%d'] %s: RED CARD for %s 🟥 (Sent off!)", ev.getMinute(), teamName, playerName);
        SoundManager.play(SoundManager.WHISTLE_FOUL);
        if (isHome) SoundManager.playAfter(SoundManager.GOAL_CONCEDED, 600); // โห่เฉพาะใบแดงของฝั่งเรา
        popups.showEventPopupAndWait(new GameEvent.FoulEvent(playerName, PlayerImageResolver.getPlayerImagePath(playerName), true, isHome), isHome, true);

        Player p = ev.getPlayer();
        if (p != null) {
            redCardedPlayers.add(p);

            // ใช้ withoutPlayer แทน create() เพราะตัวจริงเหลือ 10 คน (create บังคับ 11)
            if (isHome && homeTeamSetup != null) {
                if (homeTeamSetup.getStarters().contains(p) && p.getPosition() != Position.GK) {
                    homeTeamSetup = homeTeamSetup.withoutPlayer(p);
                    currentMatch.setHomeTeam(homeTeamSetup);
                }
            } else if (!isHome && awayTeamSetup != null) {
                if (awayTeamSetup.getStarters().contains(p) && p.getPosition() != Position.GK) {
                    awayTeamSetup = awayTeamSetup.withoutPlayer(p);
                    currentMatch.setAwayTeam(awayTeamSetup);
                }
            }
        }

        if (desc.contains("FORFEITED")) {
            if (liveTimer != null) liveTimer.stop();
            appendColoredLog("");
            appendColoredLog("🚨 MATCH FORFEITED! " + teamName + " RECEIVED 5 RED CARDS!");
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "🚨 MATCH FORFEITED!\nทีม " + teamName + " ได้รับใบแดงครบ 5 คน! ถูกปรับแพ้อัตโนมัติ");
            alert.setTitle("Match Forfeited");
            alert.setHeaderText(null);
            alert.initOwner(parent);
            alert.showAndWait();
            return null;
        }
        return log;
    }

    private void handleYellowCard(MatchEvent ev, String teamName, String playerName, boolean isHome) {
        Player p = ev.getPlayer();
        if (p != null) yellowCardPlayers.add(p.getName());

        String cardLog = String.format("[%d'] %s: FOUL by %s (YELLOW CARD 🟨)", ev.getMinute(), teamName, playerName);
        appendColoredLog(cardLog);
        SoundManager.play(SoundManager.WHISTLE_FOUL);

        if (liveTimer != null) liveTimer.stop();

        popups.showPopup(new GameEvent.FoulEvent(playerName, PlayerImageResolver.getPlayerImagePath(playerName), false, isHome), isHome);

        UiTimer ycTimer = new UiTimer(2000, () -> {
            popups.closeCurrentPopup();

            if (isHome && p != null) {
                Alert confirm = new Alert(Alert.AlertType.WARNING,
                        "🟨 นักเตะ " + playerName + " ได้รับใบเหลือง!\nต้องการหยุดเกมเพื่อเปลี่ยนตัวออกทันทีหรือไม่?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Yellow Card Pause");
                confirm.setHeaderText(null);
                confirm.initOwner(parent);
                boolean wantsSub = confirm.showAndWait().filter(b -> b == ButtonType.YES).isPresent();

                if (wantsSub) {
                    openMidMatchSubDialog();
                } else {
                    if (liveTimer != null) liveTimer.start();
                }
            } else if (!isHome && p != null) {
                if (awaySubsUsed < 3 && awayTeamSetup.getBench() != null && !awayTeamSetup.getBench().isEmpty()) {
                    Player sub = AwayTeamAI.findCardReplacement(awayTeamSetup, p);

                    if (sub != null) {
                        awayTeamSetup = AwayTeamAI.applySubstitution(awayTeamSetup, p, sub);
                        subbedOutPlayers.add(p);
                        currentMatch.setAwayTeam(awayTeamSetup);

                        awaySubsUsed++;
                        appendColoredLog(String.format("🔄 [AI Tactical Sub] Away substituted %s OUT ⚽ %s IN (%d/3)", p.getName(), sub.getName(), awaySubsUsed));

                        // แสดง Pop-up เปลี่ยนตัวแบบ 2-in-1 สำหรับ AI
                        popups.showEventPopupAndWait(new GameEvent.SubstitutionEvent(p.getName(), sub.getName(), PlayerImageResolver.getPlayerImagePath(sub.getName()), false), false, true);
                        return;
                    }
                }
                if (liveTimer != null) liveTimer.start();
            } else {
                if (liveTimer != null) liveTimer.start();
            }
        });
        ycTimer.setRepeats(false);
        ycTimer.start();
    }

    private String findKeeperName(TeamSetup defendingTeam) {
        String keeperName = "Goalkeeper";
        if (defendingTeam != null && defendingTeam.getStarters() != null) {
            for (Player p : defendingTeam.getStarters()) {
                if (p.getPosition() == Position.GK || p instanceof GoalKeeper) {
                    keeperName = p.getName();
                    break;
                }
            }
        }
        return keeperName;
    }

    // MID-MATCH SUB DIALOG

    private void openMidMatchSubDialog() {
        List<Player> oldStarters = new ArrayList<>(homeTeamSetup.getStarters());

        HalfTimeDialog subDialog = new HalfTimeDialog(parent, homeTeamSetup, awayTeamSetup, yellowCardPlayers, unavailableForHome(), homeSubsUsed);
        subDialog.setTitle("Tactical Change / Substitution");
        subDialog.setOnCloseRequest(e -> e.consume()); // ปิดด้วยปุ่ม X ไม่ได้ ต้องกดปุ่มในหน้าต่าง

        changeButtonTextInDialog(subDialog.getScene().getRoot(), "Resume");

        subDialog.showAndWait();

        if (subDialog.isConfirmed()) {
            homeSubsUsed = subDialog.getUpdatedSubsUsed();
            homeTeamSetup = subDialog.getUpdatedTeamSetup();
            currentMatch.setHomeTeam(homeTeamSetup);
            recordSubbedOut(oldStarters, homeTeamSetup.getStarters());
            appendColoredLog("System: Substitution applied successfully.");

            // แสดง Pop-up 2-in-1 สำหรับการเปลี่ยนตัวกลางเกมของฝั่ง Home
            List<SubInfo> midSubs = SubstitutionDetector.detect(oldStarters, homeTeamSetup.getStarters(), true);
            if (!midSubs.isEmpty()) {
                playSubQueue(midSubs, 0, "Tactical Sub", () -> {
                    if (liveTimer != null) liveTimer.start();
                });
                return;
            }
        }

        if (liveTimer != null) {
            liveTimer.start();
        }
    }

    private void changeButtonTextInDialog(Parent container, String newText) {
        for (Node c : container.getChildrenUnmodifiable()) {
            if (c instanceof Button) {
                Button btn = (Button) c;
                String text = btn.getText();
                if (text != null && (text.toLowerCase().contains("start") || text.toLowerCase().contains("half") || text.toLowerCase().contains("confirm"))) {
                    btn.setText(newText);
                }
            } else if (c instanceof Parent) {
                changeButtonTextInDialog((Parent) c, newText);
            }
        }
    }

    // VIEW HELPERS

    private void updateScoreBoard(int minute) {
        view.updateScoreBoard(liveHomeGoals, liveAwayGoals, minute);
    }

    private void appendColoredLog(String text) {
        view.appendColoredLog(text);
    }
}