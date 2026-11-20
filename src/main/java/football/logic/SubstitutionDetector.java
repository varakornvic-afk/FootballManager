package football.logic;

import football.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * เปรียบเทียบตัวจริงก่อนและหลังแก้ไขช่วงพักครึ่ง เพื่อหาว่าเปลี่ยนใครออกและใครเข้า
 */
public final class SubstitutionDetector {

    private SubstitutionDetector() {}

    /** เปรียบเทียบ 11 ตัวจริงเก่า vs ใหม่ เพื่อจับคู่คนออก (OUT) และคนเข้า (IN) */
    public static List<SubInfo> detect(List<Player> oldStarters, List<Player> newStarters, boolean isHome) {
        List<SubInfo> subs = new ArrayList<>();
        List<Player> outs = new ArrayList<>();
        List<Player> ins = new ArrayList<>();

        for (Player p : oldStarters) {
            if (!newStarters.contains(p)) outs.add(p);
        }
        for (Player p : newStarters) {
            if (!oldStarters.contains(p)) ins.add(p);
        }

        int count = Math.min(outs.size(), ins.size());
        for (int i = 0; i < count; i++) {
            subs.add(new SubInfo(outs.get(i).getName(), ins.get(i).getName(), isHome));
        }
        return subs;
    }
}