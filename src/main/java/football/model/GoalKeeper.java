package football.model;

public class GoalKeeper extends Player {
    private final int saving;

    public GoalKeeper(String name, int pace, int passing, int heading,
                      int stamina, int shoot, int defense, int saving) {
        super(name, Position.GK, pace, passing, heading, stamina, shoot, defense,
                checkSaving(saving));
        this.saving = saving;
    }

    private static int checkSaving(int saving) {
        checkRange("saving", saving);
        return saving;
    }

    public int getSaving() { return saving; }

    /** ลด Stamina ลง 1 ทุกครั้งที่เซฟลูกยิงในเกมปกติสำเร็จ (ขั้นต่ำ 1) */
    public void onSaveInGame() {
        reduceStamina(1);
    }
}