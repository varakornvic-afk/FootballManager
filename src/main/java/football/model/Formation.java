package football.model;

public abstract class Formation {
    protected static final double SPECIALIST_FIT = 1.15;  // ตรงแผนที่ฟอร์เมชันนี้ถนัด
    protected static final double OFF_PLAN_FIT = 0.95;    // ไม่ตรงแผน
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
}