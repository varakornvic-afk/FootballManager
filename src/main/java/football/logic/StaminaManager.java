package football.logic;

import football.event.*;
import football.event.MatchEvent;
import football.model.*;

public final class StaminaManager {

    private StaminaManager() {}

    public static void updatePlayersStamina(MatchEvent ev, TeamSetup homeTeamSetup, TeamSetup awayTeamSetup) {
        int minute = ev.getMinute();

        if (minute > 0 && minute % 20 == 0) {
            reduceOutfieldStamina(homeTeamSetup);
            reduceOutfieldStamina(awayTeamSetup);
        }

        String descLower = ev.getDescription().toLowerCase();
        boolean isSaveEvent = ev.getEventType() == MatchEvent.EventType.SAVE || descLower.contains("save") || descLower.contains("saved");

        if (isSaveEvent) {
            boolean isAttackingHome = ev.getTeamName() != null && ev.getTeamName().toLowerCase().contains("home");
            TeamSetup defendingTeam = isAttackingHome ? awayTeamSetup : homeTeamSetup;

            if (defendingTeam != null && defendingTeam.getStarters() != null) {
                for (Player p : defendingTeam.getStarters()) {
                    if (p instanceof GoalKeeper) {
                        ((GoalKeeper) p).onSaveInGame();
                        break;
                    } else if (p.getPosition() == Position.GK) {
                        if (p.getStamina() > 0) p.setStamina(p.getStamina() - 1);
                        break;
                    }
                }
            }
        }

        Player activePlayer = ev.getPlayer();
        if (activePlayer != null && activePlayer.getStamina() > 0) {
            boolean isGK = activePlayer instanceof GoalKeeper || activePlayer.getPosition() == Position.GK;
            if (!isGK) {
                activePlayer.setStamina(activePlayer.getStamina() - 1);
            }
        }
    }

    private static void reduceOutfieldStamina(TeamSetup team) {
        if (team != null && team.getStarters() != null) {
            for (Player p : team.getStarters()) {
                boolean isGK = p.getPosition() == Position.GK || p instanceof GoalKeeper;
                if (!isGK && p.getStamina() > 0) {
                    p.setStamina(p.getStamina() - 1);
                }
            }
        }
    }
}