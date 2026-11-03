package football.ui;

import football.logic.MatchStatistics;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MatchSummaryDialog extends Stage {

    public MatchSummaryDialog(Stage owner, String homeTeamName, String awayTeamName,
                              int homeScore, int awayScore, MatchStatistics stats) {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Post-Match Analysis & Statistics");

        BorderPane root = new BorderPane();

        // 1. Title Header & Final Score
        Label titleLabel = new Label("MATCH SUMMARY");
        titleLabel.setTextFill(Color.LIGHTGRAY);
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));

        Label scoreLabel = new Label(String.format("%s  %d - %d  %s", homeTeamName, homeScore, awayScore, awayTeamName));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));

        VBox headerPanel = new VBox(6, titleLabel, scoreLabel);
        headerPanel.setAlignment(Pos.CENTER);
        headerPanel.setPadding(new Insets(10));
        headerPanel.setBackground(new Background(new BackgroundFill(Color.rgb(40, 45, 52), CornerRadii.EMPTY, Insets.EMPTY)));
        root.setTop(headerPanel);

        // 2. Stats Comparison Table Panel (5 แถว x 3 คอลัมน์)
        GridPane statsPanel = new GridPane();
        statsPanel.setHgap(10);
        statsPanel.setVgap(8);
        statsPanel.setPadding(new Insets(15, 20, 15, 20));
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            statsPanel.getColumnConstraints().add(cc);
        }

        // Null-Safety Check
        if (stats != null) {
            addStatRow(statsPanel, 0, stats.getHomePossessionPercent() + "%", "Possession", stats.getAwayPossessionPercent() + "%");
            addStatRow(statsPanel, 1, String.valueOf(stats.getHomeShots()), "Total Shots", String.valueOf(stats.getAwayShots()));
            addStatRow(statsPanel, 2, String.valueOf(stats.getHomeShotsOnTarget()), "Shots on Target", String.valueOf(stats.getAwayShotsOnTarget()));
            addStatRow(statsPanel, 3, String.valueOf(stats.getHomeYellowCards()), "Yellow Cards", String.valueOf(stats.getAwayYellowCards()));
            addStatRow(statsPanel, 4, String.valueOf(stats.getHomeRedCards()), "Red Cards", String.valueOf(stats.getAwayRedCards()));
        } else {
            addStatRow(statsPanel, 0, "50%", "Possession", "50%");
            addStatRow(statsPanel, 1, "0", "Total Shots", "0");
            addStatRow(statsPanel, 2, "0", "Shots on Target", "0");
            addStatRow(statsPanel, 3, "0", "Yellow Cards", "0");
            addStatRow(statsPanel, 4, "0", "Red Cards", "0");
        }
        root.setCenter(statsPanel);

        // 3. Close Button
        Button closeBtn = new Button("Close & Return to Menu");
        closeBtn.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        closeBtn.setOnAction(e -> close());

        HBox bottomPanel = new HBox(closeBtn);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(10));
        root.setBottom(bottomPanel);

        setScene(new Scene(root, 500, 420));

        setOnShown(e -> {
            if (owner != null) {
                setX(owner.getX() + (owner.getWidth() - getWidth()) / 2);
                setY(owner.getY() + (owner.getHeight() - getHeight()) / 2);
            }
        });
    }

    private void addStatRow(GridPane grid, int row, String homeVal, String statName, String awayVal) {
        Label homeLbl = new Label(homeVal);
        homeLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        GridPane.setHalignment(homeLbl, HPos.RIGHT);

        Label nameLbl = new Label(statName);
        nameLbl.setTextFill(Color.GRAY);
        nameLbl.setFont(Font.font("SansSerif", 12));
        GridPane.setHalignment(nameLbl, HPos.CENTER);

        Label awayLbl = new Label(awayVal);
        awayLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        GridPane.setHalignment(awayLbl, HPos.LEFT);

        grid.add(homeLbl, 0, row);
        grid.add(nameLbl, 1, row);
        grid.add(awayLbl, 2, row);
    }
}
