package football.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private MainMenuPanel mainMenuPanel;
    private SquadPanel squadPanel;
    private MatchPanel matchPanel;
    private MatchController matchController;

    public MainFrame() {
        setTitle("Tactix Football");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        matchPanel = new MatchPanel(
                () -> matchController.runMatchSimulation(),
                () -> {
                    matchController.stop();
                    cardLayout.show(cardPanel, "MENU");
                });
        matchController = new MatchController(this, matchPanel);

        squadPanel = new SquadPanel((home, away) -> {
            matchController.setTeams(home, away);
            cardLayout.show(cardPanel, "MATCH");
        });

        mainMenuPanel = new MainMenuPanel(() -> {
            squadPanel.startNewDraftSession();
            cardLayout.show(cardPanel, "SQUAD");
        });

        cardPanel.add(mainMenuPanel, "MENU");
        cardPanel.add(squadPanel, "SQUAD");
        cardPanel.add(matchPanel, "MATCH");

        add(cardPanel);
        cardLayout.show(cardPanel, "MENU");
    }
}