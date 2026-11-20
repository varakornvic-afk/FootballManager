package football.logic;

import football.model.*;
import football.util.PlayerRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * สร้าง TeamSetup ของทีมเหย้าจากตัวเลือกบนหน้าจัดทีม (ชื่อแผนและฟอร์เมชัน)
 * และสร้างทีมเยือนแบบสุ่ม
 */
public final class TeamSetupFactory {

    private TeamSetupFactory() {}

    /** สร้างทีมเหย้าจากชื่อ tactic / formation ที่เลือกใน ComboBox */
    public static TeamSetup createHome(String selectedTactic, String selectedForm,
                                       List<Player> starters, List<Player> bench) {
        Tactic tactic;
        if ("Possession".equals(selectedTactic)) tactic = new Possession();
        else if ("Wide Play".equals(selectedTactic)) tactic = new WidePlay();
        else tactic = new CounterAttack();

        Formation form;
        if ("4-3-3".equals(selectedForm)) form = new Formation433();
        else if ("3-4-3".equals(selectedForm)) form = new Formation343();
        else if ("5-3-2".equals(selectedForm)) form = new Formation532();
        else form = new Formation442();

        return TeamSetup.create(tactic, form, Stance.NORMAL, starters, bench);
    }

    /** สร้างทีมเยือนแบบสุ่ม (ผู้เล่น + tactic + formation + stance) */
    public static TeamSetup createRandomAway() {
        List<Player> all = PlayerRepository.loadPlayersFromCSV("players.csv");
        Collections.shuffle(all);

        // ตัวจริงต้องมี GK พอดี 1 คน จึงเลือก GK ก่อนแล้วค่อยสุ่มนักเตะสนามอีก 10 คน
        Player keeper = all.stream()
                .filter(p -> p.getPosition() == Position.GK)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("players.csv has no goalkeeper"));
        List<Player> others = new ArrayList<>(all);
        others.remove(keeper);

        List<Player> starters = new ArrayList<>();
        starters.add(keeper);
        for (Player p : others) {
            if (starters.size() == TeamSetup.LINEUP_SIZE) break;
            if (p.getPosition() != Position.GK) starters.add(p);
        }
        others.removeAll(starters);
        List<Player> bench = new ArrayList<>(others.subList(0, 5));

        Random rand = new Random();
        Tactic[] availableTactics = new Tactic[]{ new Possession(), new CounterAttack(), new WidePlay() };
        Formation[] availableFormations = new Formation[]{ new Formation442(), new Formation433(), new Formation343(), new Formation532() };
        Stance[] availableStances = Stance.values();

        Tactic randomAwayTactic = availableTactics[rand.nextInt(availableTactics.length)];
        Formation randomAwayForm = availableFormations[rand.nextInt(availableFormations.length)];
        Stance randomAwayStance = availableStances[rand.nextInt(availableStances.length)];

        return TeamSetup.create(randomAwayTactic, randomAwayForm, randomAwayStance,
                starters, bench);
    }
}