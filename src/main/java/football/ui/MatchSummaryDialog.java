package football.ui;

import football.logic.MatchStatistics;
import javax.swing.*;
import java.awt.*;

public class MatchSummaryDialog extends JDialog {

    public MatchSummaryDialog(Frame owner, String homeTeamName, String awayTeamName,
                              int homeScore, int awayScore, MatchStatistics stats) {
        super(owner, "Post-Match Analysis & Statistics", true);
        setLayout(new BorderLayout(10, 10));
        setSize(500, 420);
        setLocationRelativeTo(owner);

        // 1. Title Header & Final Score
        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBackground(new Color(40, 45, 52));

        JLabel titleLabel = new JLabel("MATCH SUMMARY", SwingConstants.CENTER);
        titleLabel.setForeground(Color.LIGHT_GRAY);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel scoreLabel = new JLabel(String.format("%s  %d - %d  %s", homeTeamName, homeScore, awayScore, awayTeamName), SwingConstants.CENTER);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        headerPanel.add(titleLabel);
        headerPanel.add(scoreLabel);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Stats Comparison Table Panel
        JPanel statsPanel = new JPanel(new GridLayout(5, 3, 10, 8)); // 📌 แก้ไขจาก (6, 3) เป็น (5, 3) ให้พอดีกับ 5 แถว
        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 📌 Null-Safety Check
        if (stats != null) {
            addStatRow(statsPanel, stats.getHomePossessionPercent() + "%", "Possession", stats.getAwayPossessionPercent() + "%");
            addStatRow(statsPanel, String.valueOf(stats.getHomeShots()), "Total Shots", String.valueOf(stats.getAwayShots()));
            addStatRow(statsPanel, String.valueOf(stats.getHomeShotsOnTarget()), "Shots on Target", String.valueOf(stats.getAwayShotsOnTarget()));
            addStatRow(statsPanel, String.valueOf(stats.getHomeYellowCards()), "Yellow Cards", String.valueOf(stats.getAwayYellowCards()));
            addStatRow(statsPanel, String.valueOf(stats.getHomeRedCards()), "Red Cards", String.valueOf(stats.getAwayRedCards()));
        } else {
            addStatRow(statsPanel, "50%", "Possession", "50%");
            addStatRow(statsPanel, "0", "Total Shots", "0");
            addStatRow(statsPanel, "0", "Shots on Target", "0");
            addStatRow(statsPanel, "0", "Yellow Cards", "0");
            addStatRow(statsPanel, "0", "Red Cards", "0");
        }

        add(statsPanel, BorderLayout.CENTER);

        // 3. Close Button
        JButton closeBtn = new JButton("Close & Return to Menu");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        closeBtn.addActionListener(e -> dispose());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addStatRow(JPanel panel, String homeVal, String statName, String awayVal) {
        JLabel homeLbl = new JLabel(homeVal, SwingConstants.RIGHT);
        homeLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel nameLbl = new JLabel(statName, SwingConstants.CENTER);
        nameLbl.setForeground(Color.GRAY);
        nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel awayLbl = new JLabel(awayVal, SwingConstants.LEFT);
        awayLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        panel.add(homeLbl);
        panel.add(nameLbl);
        panel.add(awayLbl);
    }
}