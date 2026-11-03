package football;

import football.ui.MainFrame;
import javafx.application.Application;

/**
 * จุดเริ่มโปรแกรม
 * แยกออกจาก MainFrame (ที่ extends Application) เพื่อให้รันจากไฟล์ .jar ได้
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(MainFrame.class, args);
    }
}
