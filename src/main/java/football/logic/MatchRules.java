package football.logic;

import football.model.Formation;
import football.model.Tactic;

public final class MatchRules {
    public static final double TACTIC_ADVANTAGE = 1.25;
    public static final double BASE_PENALTY = 0.2;
    public static final double MIN_MODIFIER = 0.6;
    public static final double MAX_MODIFIER = 1.5;

    private MatchRules() { }

    /** ผลของวงกลมแทคติก: ชนะได้โบนัสคงที่ แพ้เสียตาม exposure ของฟอร์เมชัน */
    public static double tacticModifier(Tactic mine, Tactic theirs, Formation formation) {
        if (mine.beats(theirs)) return TACTIC_ADVANTAGE;
        if (theirs.beats(mine)) return 1.0 - BASE_PENALTY * formation.getExposure();
        return 1.0;
    }

    /** ตัวคูณรวม = ผลแทคติก x ความเหมาะของฟอร์เมชัน แล้วจำกัดช่วง */
    public static double totalModifier(Tactic mine, Tactic theirs, Formation formation) {
        double total = tacticModifier(mine, theirs, formation) * formation.fitFor(mine);
        return Math.max(MIN_MODIFIER, Math.min(MAX_MODIFIER, total));
    }
}