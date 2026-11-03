package football.ui;

import javafx.scene.control.Button;

/** ปุ่มเปิด/ปิดเสียงทั้งเกม (ทุกปุ่มในทุกหน้าแสดงสถานะตรงกัน) */
public class SoundToggleButton extends Button {

    public SoundToggleButton() {
        refresh();
        setFocusTraversable(false);
        setOnAction(e -> SoundManager.setMuted(!SoundManager.isMuted()));
        SoundManager.addMuteListener(this::refresh);
    }

    private void refresh() {
        setText(SoundManager.isMuted() ? "Sound: Off" : "Sound: On");
    }
}