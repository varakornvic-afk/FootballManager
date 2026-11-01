package football.ui;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {
    public MainMenuPanel(Runnable onStart, Runnable onExit) {
        setLayout(new GridBagLayout());
        setBackground(new Color(30, 40, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;

        JLabel titleLabel = new JLabel("FOOTBALL MANAGER");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, gbc);

        JButton startButton = new JButton("Start Game");
        JButton exitButton = new JButton("Exit");
        gbc.gridy = 1; add(startButton, gbc);
        gbc.gridy = 2; add(exitButton, gbc);

        startButton.addActionListener(e -> onStart.run());
        exitButton.addActionListener(e -> onExit.run());
    }
}