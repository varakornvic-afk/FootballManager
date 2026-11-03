package football.logic;

import football.model.Formation;
import football.model.Tactic;

public final class MatchRules {
    public static final double TACTIC_ADVANTAGE = 1.25;
    public static final double BASE_PENALTY = 0.2;

    private MatchRules() { }

    /** ผลของวงกลมแทคติก: ชนะได้โบนัสคงที่ แพ้เสียตาม exposure ของฟอร์เมชัน */
    public static double tacticModifier(Tactic mine, Tactic theirs, Formation formation) {
        if (mine.beats(theirs)) return TACTIC_ADVANTAGE;
        if (theirs.beats(mine)) return 1.0 - (BASE_PENALTY * formation.getExposure());
        return 1.0;
    }
}