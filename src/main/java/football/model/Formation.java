package football.model;

/**
 * ฟอร์เมชันของทีม (เช่น 4-4-2) เก็บค่าโจมตี คุมเกม ป้องกัน และ exposure
 * (ยิ่งสูงยิ่งเสียหนักเมื่อแพ้ทางแผน) คลาสลูกกำหนดช่องตำแหน่ง (getSlots)
 * และความเหมาะกับแต่ละแผน (fitFor)
 */
public abstract class Formation {
    protected static final double SPECIALIST_FIT = 1.15;  // ตรงแผนที่ฟอร์เมชันนี้ถนัด
    protected static final double OFF_PLAN_FIT = 0.95;    // ไม่ตรงแผน
    protected static final double POOR_FIT = 0.90;        // ขัดกับแผนมาก (เช่น 5-3-2 ไม่มีปีกเลย แต่เล่น Wide Play)
    protected static final double VERSATILE_FIT = 1.05;   // ฟอร์เมชันยืดหยุ่น เท่ากันทุกแผน

    private final String name;
    private final int attack;
    private final int control;
    private final int defense;
    private final double exposure;  // ยิ่งสูง ยิ่งเสียหนักเมื่อเจอแทคติกที่ชนะเรา

    protected Formation(String name, int attack, int control, int defense, double exposure) {
        this.name = name;
        this.attack = attack;
        this.control = control;
        this.defense = defense;
        this.exposure = exposure;
    }

    /** ตัวคูณความเหมาะสมของฟอร์เมชันนี้กับแทคติกที่เลือก */
    public abstract double fitFor(Tactic tactic);

    public String getName() { return name; }
    public int getAttack() { return attack; }
    public int getControl() { return control; }
    public int getDefense() { return defense; }
    public double getExposure() { return exposure; }

    public abstract SlotLayout getSlots();
}