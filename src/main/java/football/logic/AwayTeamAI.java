package football.logic;

import football.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** ตรรกะ AI ของทีมเยือน (ปรับทีมช่วงพักครึ่ง + เปลี่ยนตัวตอนโดนใบเหลือง) */
public final class AwayTeamAI {

    private AwayTeamAI() {}

    /**
     * @param subsRemaining โควตาเปลี่ยนตัวที่เหลือของทีมเยือน (3 - subsUsed) ห้ามเปลี่ยนเกินนี้
     */
    public static TeamSetup adaptAtHalftime(TeamSetup currentAway, int homeGoals, int awayGoals, int subsRemaining) {
        Stance newStance;
        Formation newForm;

        if (awayGoals < homeGoals) {
            newStance = Stance.ALL_OUT_ATTACK;
            newForm = new Formation433();
        } else if (awayGoals > homeGoals) {
            newStance = Stance.PARK_THE_BUS;
            newForm = new Formation442();
        } else {
            newStance = Stance.NORMAL;
            newForm = currentAway.getFormation();
        }

        // ใช้ withFormation/withStance แทน create() เพื่อให้ใช้ได้แม้สำรองเหลือ < 5 หรือตัวจริงเหลือ < 11 (หลังเปลี่ยนตัว/ใบแดง)
        TeamSetup result = currentAway.withFormation(newForm).withStance(newStance);

        // เรียงตัวจริงที่เหนื่อยสุดก่อน เพื่อให้โควตาไปตกที่คนที่เหนื่อยจริง ๆ
        List<Player> byTiredness = new ArrayList<>(result.getStarters());
        byTiredness.sort(Comparator.comparingInt(Player::getStamina));

        int subsMade = 0;
        for (Player starter : byTiredness) {
            if (subsMade >= subsRemaining) break;

            boolean isGK = starter instanceof GoalKeeper || starter.getPosition() == Position.GK;
            int fatigueThreshold = isGK ? 1 : 2;

            if (starter.getStamina() <= fatigueThreshold) {
                for (Player sub : result.getBench()) {
                    boolean subIsGK = sub instanceof GoalKeeper || sub.getPosition() == Position.GK;
                    if (isGK == subIsGK && sub.getStamina() > fatigueThreshold) {
                        result = result.withSubstitution(starter, sub);
                        subsMade++;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * หาตัวสำรองคนแรกที่ตำแหน่งประเภทเดียวกัน (GK ↔ GK) ไว้แทนผู้เล่นที่โดนใบเหลือง
     * คืน null ถ้าไม่มีตัวสำรองที่เหมาะ หรือผู้เล่นคนนั้นไม่ได้อยู่ในสนามแล้ว
     */
    public static Player findCardReplacement(TeamSetup away, Player carded) {
        if (!away.getStarters().contains(carded)) {
            return null;
        }
        boolean isGK = carded.getPosition() == Position.GK || carded instanceof GoalKeeper;

        for (int i = 0; i < away.getBench().size(); i++) {
            Player sub = away.getBench().get(i);
            boolean subIsGK = sub.getPosition() == Position.GK || sub instanceof GoalKeeper;

            if (isGK == subIsGK) {
                return sub;
            }
        }
        return null;
    }

    public static TeamSetup applySubstitution(TeamSetup away, Player out, Player in) {
        return away.withSubstitution(out, in);
    }
}