package football.ui;

import football.event.*;

import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** จัดการ Pop-up เหตุการณ์ในเกม (แสดง / ปิดอัตโนมัติ) */
public class PopupManager {
    private final Stage parent;
    private final Supplier<UiTimer> liveTimerSupplier;
    private final List<EventPopupDialog> openPopups = new ArrayList<>();

    public PopupManager(Stage parent, Supplier<UiTimer> liveTimerSupplier) {
        this.parent = parent;
        this.liveTimerSupplier = liveTimerSupplier;
    }

    /** แสดง popup เฉย ๆ (ไม่หยุด/ไม่เริ่ม timer ให้) */
    public void showPopup(GameEvent event, boolean isHome) {
        EventPopupDialog dialog = EventPopupDialog.showPopup(parent, event, isHome);
        openPopups.add(dialog);
        dialog.setOnHidden(e -> openPopups.remove(dialog));
    }

    public void showEventPopupAndWait(GameEvent event, boolean isHome, boolean autoResume) {
        UiTimer liveTimer = liveTimerSupplier.get();
        if (liveTimer != null) liveTimer.stop();

        UiTimer autoCloseTimer = new UiTimer(2000, () -> {
            closeCurrentPopup();
            UiTimer t = liveTimerSupplier.get();
            if (autoResume && t != null) {
                t.start();
            }
        });
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();

        showPopup(event, isHome);
    }

    public void closeCurrentPopup() {
        for (EventPopupDialog dialog : new ArrayList<>(openPopups)) {
            dialog.close();
        }
        openPopups.clear();
    }
}
