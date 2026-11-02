package football.ui;

import football.model.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HalfTimeDialog extends JDialog {
    private TeamSetup updatedTeamSetup;
    private boolean confirmed = false;

    // 📌 บันทึกจำนวนโควตาเปลี่ยนตัวที่ใช้ไปแล้ว
    private int currentSubsUsed;
    private int newSubsMadeInSession = 0;

    // บันทึกรายชื่อนักเตะที่ถูกถอดออกจากสนามแล้ว + คนที่โดนใบแดง (ห้ามเลือก)
    private final Set<Player> subbedOutPlayers = new HashSet<>();
    private final TitledBorder subPanelBorder;

    public HalfTimeDialog(JFrame parent, TeamSetup currentSetup, TeamSetup awaySetup,
                          Set<String> yellowCardPlayers, Set<Player> redCardedPlayers, int currentSubsUsed) {
        super(parent, "Half-Time Tactics & Substitutions", true);
        this.currentSubsUsed = currentSubsUsed;
        setSize(620, 640);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        if (redCardedPlayers != null) {
            this.subbedOutPlayers.addAll(redCardedPlayers);
        }

        JPanel contentPanel = new JPanel(new GridLayout(6, 1, 8, 8));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 🔍 0. Opponent Scout Report
        JPanel scoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scoutPanel.setBorder(BorderFactory.createTitledBorder("🔍 Opponent Scout Report (ข้อมูลคู่แข่ง)"));
        scoutPanel.setBackground(new Color(245, 245, 250));

        String awayTacticName = awaySetup != null && awaySetup.getTactic() != null ?
                awaySetup.getTactic().getClass().getSimpleName() : "Unknown";
        String awayFormName = awaySetup != null && awaySetup.getFormation() != null ?
                awaySetup.getFormation().getClass().getSimpleName().replace("Formation", "") : "Unknown";
        Stance awayStance = awaySetup != null ? awaySetup.getStance() : Stance.NORMAL;

        JLabel scoutLabel = new JLabel(String.format("<html><b>Tactic (Fixed):</b> <font color='blue'>%s</font> | <b>Formation:</b> <font color='blue'>%s</font> | <b>Stance:</b> <font color='blue'>%s</font></html>",
                awayTacticName, awayFormName, awayStance));
        scoutLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        scoutPanel.add(scoutLabel);

        // 1. Tactic (Fixed) & Formation ฝั่งเรา
        JPanel tacticPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String homeTacticName = currentSetup != null && currentSetup.getTactic() != null ?
                currentSetup.getTactic().getClass().getSimpleName() : "Standard";

        JLabel tacticFixedLabel = new JLabel("Tactic (Fixed): 🔒 " + homeTacticName + "    ");
        tacticFixedLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tacticPanel.add(tacticFixedLabel);

        tacticPanel.add(new JLabel("Formation: "));
        JComboBox<String> formCombo = new JComboBox<>(new String[]{"4-4-2", "4-3-3", "3-4-3", "5-3-2"});
        if (currentSetup != null && currentSetup.getFormation() != null) {
            String currentFormStr = currentSetup.getFormation().getClass().getSimpleName().replace("Formation", "");
            if (currentFormStr.length() >= 3) {
                formCombo.setSelectedItem(currentFormStr.substring(0, 1) + "-" + currentFormStr.substring(1, 2) + "-" + currentFormStr.substring(2));
            }
        }
        tacticPanel.add(formCombo);

        // 2. Team Stance ฝั่งเรา
        JPanel stancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stancePanel.add(new JLabel("Team Stance: "));
        JComboBox<Stance> stanceCombo = new JComboBox<>(Stance.values());
        if (currentSetup != null && currentSetup.getStance() != null) {
            stanceCombo.setSelectedItem(currentSetup.getStance());
        }
        stancePanel.add(stanceCombo);

        // 3. Substitutions List
        List<Player> starters = new ArrayList<>(currentSetup.getStarters());
        List<Player> bench = new ArrayList<>(currentSetup.getBench());

        JPanel subPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        subPanelBorder = BorderFactory.createTitledBorder(getPanelSubTitle(starters.size()));
        subPanel.setBorder(subPanelBorder);

        JComboBox<Player> starterCombo = new JComboBox<>();
        JComboBox<Player> benchCombo = new JComboBox<>();

        updateComboBoxModels(starterCombo, benchCombo, starters, bench, yellowCardPlayers);

        subPanel.add(new JLabel("Out (Starter):")); subPanel.add(starterCombo);
        subPanel.add(new JLabel("In (Bench):")); subPanel.add(benchCombo);

        // 4. Action Buttons
        JPanel btnActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton swapBtn = new JButton("🔄 Swap Player");
        JButton recommendBtn = new JButton("💡 Smart Recommend");

        btnActionPanel.add(swapBtn);
        btnActionPanel.add(recommendBtn);

        // 🔄 ปุ่มสลับตัว (ดักจับโควตาไม่เกิน 3 คน)
        swapBtn.addActionListener(ev -> {
            // 🚨 เช็กว่าโควตา 3 คนเต็มหรือยัง
            if (this.currentSubsUsed + newSubsMadeInSession >= 3) {
                JOptionPane.showMessageDialog(this,
                        "คุณใช้โควตาเปลี่ยนตัวครบ 3 คนแล้ว! ไม่สามารถเปลี่ยนเพิ่มได้อีก",
                        "Substitutions Limit Reached", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Player pOut = (Player) starterCombo.getSelectedItem();
            Player pIn = (Player) benchCombo.getSelectedItem();

            if (pOut == null || pIn == null) return;

            if (subbedOutPlayers.contains(pIn)) {
                JOptionPane.showMessageDialog(this,
                        "นักเตะ " + pIn.getName() + " ไม่สามารถเปลี่ยนลงสนามได้ (ถูกถอดออกแล้ว หรือ โดนใบแดง)!",
                        "Substitution Restriction", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // ผู้รักษาประตูเปลี่ยนได้เฉพาะกับผู้รักษาประตูด้วยกัน (ทีมต้องมี GK 1 คนเสมอ)
            if ((pOut.getPosition() == Position.GK) != (pIn.getPosition() == Position.GK)) {
                JOptionPane.showMessageDialog(this,
                        "ผู้รักษาประตูเปลี่ยนได้เฉพาะกับผู้รักษาประตูด้วยกันเท่านั้น!",
                        "Substitution Restriction", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int idxOut = starters.indexOf(pOut);
            int idxIn = bench.indexOf(pIn);

            if (idxOut >= 0 && idxIn >= 0) {
                starters.set(idxOut, pIn);
                bench.set(idxIn, pOut);
                subbedOutPlayers.add(pOut);
                newSubsMadeInSession++;

                subPanelBorder.setTitle(getPanelSubTitle(starters.size()));
                subPanel.repaint();

                updateComboBoxModels(starterCombo, benchCombo, starters, bench, yellowCardPlayers);

                int remaining = 3 - (this.currentSubsUsed + newSubsMadeInSession); // 📌 แก้ไข Syntax Error
                JOptionPane.showMessageDialog(this, "สลับตัวเรียบร้อย: " + pOut.getName() + " (ออก) ↔ " + pIn.getName() + " (เข้า)\nโควตาคงเหลือ: " + remaining + "/3");
            }
        });

        recommendBtn.addActionListener(ev -> {
            if (this.currentSubsUsed + newSubsMadeInSession >= 3) {
                JOptionPane.showMessageDialog(this, "คุณใช้โควตาเปลี่ยนตัวครบ 3 คนแล้ว!", "Limit Reached", JOptionPane.WARNING_MESSAGE);
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
                starterCombo.setSelectedItem(worstStarter);
                benchCombo.setSelectedItem(bestBench);
                JOptionPane.showMessageDialog(this,
                        "💡 แนะนำเปลี่ยนตัว:\n" +
                                "ถอด: " + worstStarter.getName() + " [" + worstStarter.getPosition() + "] " + worstStarter.getStaminaStatusIcon() + "\n" +
                                "ส่ง: " + bestBench.getName() + " [" + bestBench.getPosition() + "] " + bestBench.getStaminaStatusIcon() + " เข้าแทน",
                        "Substitution Recommendation", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "ไม่มีตัวสำรองที่สามารถเปลี่ยนลงสนามได้แล้ว", "Notice", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        contentPanel.add(scoutPanel);
        contentPanel.add(tacticPanel);
        contentPanel.add(stancePanel);
        contentPanel.add(subPanel);
        contentPanel.add(btnActionPanel);

        JButton startH2Btn = new JButton("Start Second Half ⚽");
        startH2Btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startH2Btn.addActionListener(ev -> {
            String selectedForm = (String) formCombo.getSelectedItem();

            // 📌 ปรับปรุงให้รองรับทุก Formation ตาม Dropdown
            Formation newForm;
            switch (selectedForm != null ? selectedForm : "4-4-2") {
                case "4-3-3": newForm = new Formation433(); break;
                case "3-4-3": newForm = new Formation343(); break;
                case "5-3-2": newForm = new Formation532(); break;
                default: newForm = new Formation442(); break;
            }

            Stance newStance = (Stance) stanceCombo.getSelectedItem();

            // ใช้ withLineup แทน create เพราะตัวจริงอาจเหลือน้อยกว่า 11 คน (โดนใบแดง)
            updatedTeamSetup = currentSetup.withLineup(newForm, newStance, starters, bench);
            confirmed = true;
            dispose();
        });

        add(contentPanel, BorderLayout.CENTER);
        add(startH2Btn, BorderLayout.SOUTH);
    }

    private String getPanelSubTitle(int activeStarters) {
        int totalUsed = currentSubsUsed + newSubsMadeInSession;
        return String.format("Player Substitution (Used: %d/3 | Active Players: %d)", totalUsed, activeStarters);
    }

    private void updateComboBoxModels(JComboBox<Player> starterCombo, JComboBox<Player> benchCombo,
                                      List<Player> starters, List<Player> bench, Set<String> yellowCards) {

        starterCombo.setModel(new DefaultComboBoxModel<>(starters.toArray(new Player[0])));
        benchCombo.setModel(new DefaultComboBoxModel<>(bench.toArray(new Player[0])));

        ListCellRenderer<Object> renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Player p) {
                    String icon = p.getStaminaStatusIcon();
                    String posTag = "[" + p.getPosition() + "]"; // 📌 แทรกตำแหน่ง [ST], [CB], [GK]
                    String cardTag = yellowCards.contains(p.getName()) ? " 🟨" : "";
                    String subbedOutTag = subbedOutPlayers.contains(p) ? " 🚫 [Unavailable]" : "";

                    label.setText(String.format("%s %s %s (Stam: %d/%d)%s%s",
                            icon, posTag, p.getName(), p.getStamina(), p.getMaxStamina(), cardTag, subbedOutTag));

                    if (subbedOutPlayers.contains(p)) {
                        label.setForeground(Color.GRAY);
                    }
                }
                return label;
            }
        };

        starterCombo.setRenderer(renderer);
        benchCombo.setRenderer(renderer);
    }

    public TeamSetup getUpdatedTeamSetup() { return updatedTeamSetup; }
    public boolean isConfirmed() { return confirmed; }
    public int getUpdatedSubsUsed() { return currentSubsUsed + newSubsMadeInSession; }
}