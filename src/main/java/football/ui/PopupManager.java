package football.ui;

import football.event.*;
import football.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/** จัดการ Pop-up เหตุการณ์ในเกม (แสดง / ปิดอัตโนมัติ) */
public class PopupManager {
    private final JFrame parent;
    private final Supplier<javax.swing.Timer> liveTimerSupplier;

    public PopupManager(JFrame parent, Supplier<javax.swing.Timer> liveTimerSupplier) {
        this.parent = parent;
        this.liveTimerSupplier = liveTimerSupplier;
    }

    /** แสดง popup เฉย ๆ (ไม่หยุด/ไม่เริ่ม timer ให้) */
    public void showPopup(GameEvent event, boolean isHome) {
        EventPopupDialog.showPopup(parent, event, isHome);
    }

    public void showEventPopupAndWait(GameEvent event, boolean isHome, boolean autoResume) {
        javax.swing.Timer liveTimer = liveTimerSupplier.get();
        if (liveTimer != null) liveTimer.stop();

        javax.swing.Timer autoCloseTimer = new javax.swing.Timer(2000, e -> {
            closeCurrentPopup();
            javax.swing.Timer t = liveTimerSupplier.get();
            if (autoResume && t != null) {
                t.start();
            }
        });
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();

        EventPopupDialog.showPopup(parent, event, isHome);
    }

    public void closeCurrentPopup() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog && window.isVisible() && window != parent) {
                String title = ((JDialog) window).getTitle();
                if (title != null && (title.contains("Pause") || title.contains("Substitution") ||
                        title.contains("Forfeited") || title.contains("Summary") ||
                        title.contains("Statistics") || title.contains("Result") ||
                        title.contains("Tactical"))) {
                    continue;
                }
                window.dispose();
            }
        }
    }
}