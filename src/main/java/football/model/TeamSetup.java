package football.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

/** ทีมหนึ่งฝั่ง: แทคติก (ล็อก) + ฟอร์เมชัน + โหมด + ตัวจริง 11 + สำรอง immutable ทุกการเปลี่ยนคืนอ็อบเจ็กต์ใหม่ */
public final class TeamSetup {
    public static final int LINEUP_SIZE = 11;
    public static final int MAX_SUBSTITUTIONS = 3;

    private static final double FATIGUE_RATE = 0.3;

    private final Tactic tactic;
    private final Formation formation;
    private final Stance stance;
    private final List<Player> starters;
    private final List<Player> bench;
    private final int substitutionsUsed;
    private final Set<Player> tired;
    private final Map<Player, Position> assignment;

    // ในไฟล์ football/model/TeamSetup.java

    public static TeamSetup create(Tactic tactic, Formation formation, Stance stance, List<Player> starters, List<Player> bench) {
        if (tactic == null || formation == null || stance == null || starters == null || bench == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        if (starters.size() != 11) {
            throw new IllegalArgumentException("Starters must contain exactly 11 players");
        }
        if (bench.size() != 5) {
            throw new IllegalArgumentException("Bench must contain exactly 5 players");
        }

        // เช็กว่าตัวจริงต้องมีผู้รักษาประตู (GK) เพียง 1 คนเท่านั้น
        long gkCount = starters.stream()
                .filter(p -> p.getPosition() == Position.GK || p instanceof GoalKeeper)
                .count();
        if (gkCount != 1) {
            throw new IllegalArgumentException("Starters must contain exactly 1 Goalkeeper");
        }

        return new TeamSetup(tactic, formation, stance, starters, bench, 0, new java.util.HashSet<>());
    }

    private TeamSetup(Tactic tactic, Formation formation, Stance stance,
                      List<Player> starters, List<Player> bench,
                      int substitutionsUsed, Set<Player> tired) {
        this.tactic = Objects.requireNonNull(tactic, "tactic");
        this.formation = Objects.requireNonNull(formation, "formation");
        this.stance = Objects.requireNonNull(stance, "stance");
        Objects.requireNonNull(starters, "starters");
        Objects.requireNonNull(bench, "bench");

        if (starters.size() > LINEUP_SIZE) {
            throw new IllegalArgumentException("lineup cannot exceed " + LINEUP_SIZE + " players");
        }
        long keepers = starters.stream().filter(p -> p.getPosition() == Position.GK).count();
        if (keepers != 1) {
            throw new IllegalArgumentException("lineup needs exactly one goalkeeper");
        }
        Set<Player> all = new HashSet<>(starters);
        all.addAll(bench);
        if (all.size() != starters.size() + bench.size()) {
            throw new IllegalArgumentException("a player appears more than once");
        }
        this.starters = List.copyOf(starters);
        this.bench = List.copyOf(bench);
        this.substitutionsUsed = substitutionsUsed;
        this.tired = Set.copyOf(tired);
        this.assignment = assign();
    }

    // การจัดคนลงช่อง

    private Map<Player, Position> assign() {
        SlotLayout layout = stance.apply(formation.getSlots());
        Map<Position, Integer> free = new EnumMap<>(Position.class);
        for (Position p : Position.values()) {
            free.put(p, layout.count(p));
        }
        Map<Player, Position> result = new LinkedHashMap<>();
        List<Player> pool = new ArrayList<>();
        for (Player p : starters) {
            if (p.getPosition() == Position.GK) {
                place(result, free, p, Position.GK);
            } else {
                pool.add(p);
            }
        }
        if (stance == Stance.PARK_THE_BUS) {
            Player lone = best(pool, Position.STRIKER, Player::getShoot);
            pool.remove(lone);
            place(result, free, lone, Position.STRIKER);
        } else if (stance == Stance.ALL_OUT_ATTACK) {
            Player lone = best(pool, Position.CENTER_BACK, Player::getDefense);
            pool.remove(lone);
            place(result, free, lone, Position.CENTER_BACK);
        }
        for (Player p : new ArrayList<>(pool)) {
            if (free.get(p.getPosition()) > 0) {
                pool.remove(p);
                place(result, free, p, p.getPosition());
            }
        }
        for (Player p : pool) {
            place(result, free, p, closestFreeSlot(p, free));
        }
        return result;
    }

    private static Player best(List<Player> pool, Position slot, ToIntFunction<Player> stat) {
        Player best = null;
        double bestScore = -1;
        for (Player p : pool) {
            double score = stat.applyAsInt(p) * positionFactor(p.getPosition().distanceTo(slot));
            if (score > bestScore) {
                best = p;
                bestScore = score;
            }
        }
        return best;
    }

    private static Position closestFreeSlot(Player p, Map<Position, Integer> free) {
        Position best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Position slot : Position.values()) {
            if (slot == Position.GK || free.get(slot) <= 0) {
                continue;
            }
            int d = p.getPosition().distanceTo(slot);
            if (d < bestDistance) {
                best = slot;
                bestDistance = d;
            }
        }
        return best;
    }

    private static void place(Map<Player, Position> result, Map<Position, Integer> free,
                              Player p, Position slot) {
        result.put(p, slot);
        free.merge(slot, -1, Integer::sum);
    }

    // ค่าที่ใช้คำนวณ

    /** ตัวคูณสถิติตามระยะห่างจากตำแหน่งจริง */
    public static double positionFactor(int distance) {
        switch (distance) {
            case 0: return 1.0;
            case 1: return 0.9;
            case 2: return 0.75;
            default: return 0.6;
        }
    }

    /** ตัวคูณความล้าหลังพักครึ่ง: ยิ่ง stamina ต่ำยิ่งล้ามาก */
    public static double fatigueFactor(int stamina) {
        return 1.0 - (FATIGUE_RATE / Math.max(1, stamina)); // ป้องกันหาร 0 ถ้า stamina เป็น 0
    }

    public Position assignedPosition(Player player) {
        Position slot = assignment.get(player);
        if (slot == null) {
            throw new IllegalArgumentException("player is not in the lineup");
        }
        return slot;
    }

    /** ค่าสถิติจริงในสนาม = ค่าเดิม x โทษผิดตำแหน่ง x ความล้า */
    public double effectiveValue(Player player, int baseStat) {
        double value = baseStat * positionFactor(player.getPosition().distanceTo(assignedPosition(player)));
        if (tired.contains(player)) {
            value *= fatigueFactor(player.getStamina());
        }
        return value;
    }

    // การเปลี่ยนแปลง (คืนอ็อบเจ็กต์ใหม่)

    public TeamSetup withFormation(Formation newFormation) {
        return new TeamSetup(tactic, newFormation, stance, starters, bench,
                substitutionsUsed, tired);
    }

    public TeamSetup withStance(Stance newStance) {
        return new TeamSetup(tactic, formation, newStance, starters, bench,
                substitutionsUsed, tired);
    }

    public TeamSetup withSubstitution(Player out, Player in) {
        if (substitutionsUsed >= MAX_SUBSTITUTIONS) {
            throw new IllegalStateException("no substitutions left");
        }
        int index = starters.indexOf(out);
        if (index < 0) {
            throw new IllegalArgumentException("player to replace is not on the pitch");
        }
        if (!bench.contains(in)) {
            throw new IllegalArgumentException("incoming player is not on the bench");
        }
        if ((out.getPosition() == Position.GK) != (in.getPosition() == Position.GK)) {
            throw new IllegalArgumentException("goalkeeper can only be swapped with a goalkeeper");
        }
        List<Player> newStarters = new ArrayList<>(starters);
        newStarters.set(index, in);
        List<Player> newBench = new ArrayList<>(bench);
        newBench.remove(in);
        Set<Player> newTired = new HashSet<>(tired);
        newTired.remove(out);
        return new TeamSetup(tactic, formation, stance, newStarters, newBench,
                substitutionsUsed + 1, newTired);
    }
    /** เอาผู้เล่นออกจากสนามโดยไม่มีตัวแทน (ใบแดง) */
    public TeamSetup withoutPlayer(Player sentOff) {
        if (!starters.contains(sentOff)) throw new IllegalArgumentException("player is not on the pitch");
        if (sentOff.getPosition() == Position.GK) throw new IllegalArgumentException("goalkeeper cannot be removed this way");
        List<Player> newStarters = new ArrayList<>(starters);
        newStarters.remove(sentOff);
        Set<Player> newTired = new HashSet<>(tired);
        newTired.remove(sentOff);
        return new TeamSetup(tactic, formation, stance, newStarters, bench, substitutionsUsed, newTired);
    }

    /** สร้างทีมใหม่จากตัวจริง/สำรองที่ปรับแล้ว (รองรับตัวจริงน้อยกว่า 11 หลังโดนใบแดง) */
    public TeamSetup withLineup(Formation newFormation, Stance newStance,
                                List<Player> newStarters, List<Player> newBench) {
        return new TeamSetup(tactic, newFormation, newStance, newStarters, newBench,
                substitutionsUsed, tired);
    }

    public Tactic getTactic() { return tactic; }
    public Formation getFormation() { return formation; }
    public Stance getStance() { return stance; }
    public List<Player> getStarters() { return starters; }
    public List<Player> getBench() { return bench; }
}