package football.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

/**
 * ตัวจับเวลาแบบเดียวกับ javax.swing.Timer (start / stop / setRepeats / isRunning)
 * ใช้แทนของ Swing เพื่อให้โค้ดใน MatchController แทบไม่ต้องเปลี่ยน
 *
 * งานที่ถึงเวลาจะถูกรันผ่าน Platform.runLater เสมอ
 * เพราะ JavaFX ห้ามเปิด dialog แบบรอผล (showAndWait) ขณะอยู่ใน animation โดยตรง
 */
public class UiTimer {
    private final int delayMs;
    private final Runnable action;
    private boolean repeats = true;
    private boolean running = false;
    private int generation = 0; // เพิ่มทุกครั้งที่ stop/start เพื่อยกเลิกรอบที่ค้างอยู่
    private PauseTransition pause;

    public UiTimer(int delayMs, Runnable action) {
        this.delayMs = delayMs;
        this.action = action;
    }

    public void setRepeats(boolean repeats) {
        this.repeats = repeats;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        stop();
        running = true;
        schedule(generation);
    }

    public void stop() {
        running = false;
        generation++;
        if (pause != null) {
            pause.stop();
            pause = null;
        }
    }

    private void schedule(int gen) {
        pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> Platform.runLater(() -> fire(gen)));
        pause.play();
    }

    private void fire(int gen) {
        if (!running || gen != generation) return; // ถูก stop/start ใหม่ไปแล้ว
        if (!repeats) running = false;
        action.run();
        if (repeats && running && gen == generation) schedule(gen);
    }
}
