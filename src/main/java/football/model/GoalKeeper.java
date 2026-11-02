package football.model;

public class GoalKeeper extends Player {
    private int saveCount = 0; // ตัวแปรนับจำนวนเซฟในเกม

    public GoalKeeper(String name, int pace, int passing, int heading,
                      int stamina, int shoot, int defense, int saving) {
        super(name, Position.GK, pace, passing, heading, stamina, shoot, defense,
                checkSaving(saving));
    }

    private static int checkSaving(int saving) {
        checkRange("saving", saving);
        return saving;
    }

    /** ดึงค่า saving จากคลาสแม่ (Player) โดยตรง */
    @Override
    public int getSaving() {
        return super.getSaving();
    }

    /** 🧤 เมื่อทำการเซฟในเกม ครบ 3 ครั้งจะลด Stamina 1 หน่วย */
    public void onSaveInGame() {
        saveCount++;
        if (saveCount >= 3) {
            this.setStamina(Math.max(0, this.getStamina() - 1));
            saveCount = 0; // รีเซ็ตตัวนับ
        }
    }

    public int getSaveCount() {
        return saveCount;
    }
}