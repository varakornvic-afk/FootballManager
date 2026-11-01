package football.ui;

import football.model.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HalfTimeDialog extends JDialog {
    private TeamSetup updatedTeamSetup;
    private boolean confirmed = false;

    public HalfTimeDialog(JFrame parent, TeamSetup currentSetup, Set<String> yellowCardPlayers) {
        super(parent, "Half-Time Tactics & Substitutions", true);
        setSize(550, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. ปรับเปลี่ยน Tactic & Formation
        JPanel tacticPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tacticPanel.add(new JLabel("Tactic: "));
        JComboBox<String> tacticCombo = new JComboBox<>(new String[]{"Counter Attack", "Possession", "Wide Play"});
        tacticPanel.add(tacticCombo);

        tacticPanel.add(new JLabel("  Formation: "));
        JComboBox<String> formCombo = new JComboBox<>(new String[]{"4-4-2", "4-3-3", "3-4-3", "5-3-2"});
        tacticPanel.add(formCombo);

        // 2. ปรับเปลี่ยน Team Stance (Park The Bus / All Out Attack)
        JPanel stancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stancePanel.add(new JLabel("Team Stance: "));
        JComboBox<Stance> stanceCombo = new JComboBox<>(Stance.values());
        if (currentSetup != null && currentSetup.getStance() != null) {
            stanceCombo.setSelectedItem(currentSetup.getStance());
        }
        stancePanel.add(stanceCombo);

        // 3. เปลี่ยนตัวผู้เล่น (พร้อมติดสัญลักษณ์ใบเหลือง 🟨)
        List<Player> starters = new ArrayList<>(currentSetup.getStarters());
        List<Player> bench = new ArrayList<>(currentSetup.getBench());

        JPanel subPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        subPanel.setBorder(BorderFactory.createTitledBorder("Player Substitution"));

        // แปลงชื่อนักเตะให้มี 🟨 ถ้าติดใบเหลือง
        DefaultComboBoxModel<String> starterModel = createPlayerListModel(starters, yellowCardPlayers);
        DefaultComboBoxModel<String> benchModel = createPlayerListModel(bench, yellowCardPlayers);

        JComboBox<String> starterCombo = new JComboBox<>(starterModel);
        JComboBox<String> benchCombo = new JComboBox<>(benchModel);
        JButton swapBtn = new JButton("🔄 Swap Player");

        subPanel.add(new JLabel("Out (Starter):")); subPanel.add(starterCombo);
        subPanel.add(new JLabel("In (Bench):")); subPanel.add(benchCombo);

        swapBtn.addActionListener(ev -> {
            int idxOut = starterCombo.getSelectedIndex();
            int idxIn = benchCombo.getSelectedIndex();

            if (idxOut >= 0 && idxIn >= 0) {
                Player pOut = starters.remove(idxOut);
                Player pIn = bench.remove(idxIn);
                starters.add(idxIn, pIn);
                bench.add(idxOut, pOut);

                starterCombo.setModel(createPlayerListModel(starters, yellowCardPlayers));
                benchCombo.setModel(createPlayerListModel(bench, yellowCardPlayers));
                JOptionPane.showMessageDialog(this, "สลับตัวเรียบร้อย: " + pOut.getName() + " ↔ " + pIn.getName());
            }
        });

        contentPanel.add(tacticPanel);
        contentPanel.add(stancePanel);
        contentPanel.add(subPanel);
        contentPanel.add(swapBtn);

        JButton startH2Btn = new JButton("Start Second Half ⚽");
        startH2Btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        startH2Btn.addActionListener(ev -> {
            Tactic newTac = "Possession".equals(tacticCombo.getSelectedItem()) ? new Possession() : new CounterAttack();
            Formation newForm = "4-3-3".equals(formCombo.getSelectedItem()) ? new Formation433() : new Formation442();
            Stance newStance = (Stance) stanceCombo.getSelectedItem();

            updatedTeamSetup = TeamSetup.create(newTac, newForm, newStance, starters, bench);
            confirmed = true;
            dispose();
        });

        add(contentPanel, BorderLayout.CENTER);
        add(startH2Btn, BorderLayout.SOUTH);
    }

    private DefaultComboBoxModel<String> createPlayerListModel(List<Player> list, Set<String> yellowCards) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (Player p : list) {
            String name = p.getName();
            if (yellowCards.contains(name)) {
                name += " 🟨";
            }
            model.addElement(name);
        }
        return model;
    }

    public TeamSetup getUpdatedTeamSetup() { return updatedTeamSetup; }
    public boolean isConfirmed() { return confirmed; }
}