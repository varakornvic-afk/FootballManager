package football.ui;

import javafx.animation.PauseTransition;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * จัดการเสียงทั้งเกม: เพลงหน้าเมนู (วนซ้ำ) และเสียงเอฟเฟกต์สั้น ๆ
 * ปุ่มเปิด/ปิดเสียงปุ่มเดียวคุมทุกเสียง ({@link #setMuted(boolean)})
 * ถ้าหาไฟล์เสียงไม่เจอหรือเล่นไม่ได้ จะข้ามไปเงียบ ๆ เกมไม่พัง
 */
public final class SoundManager {
    public static final String MENU_MUSIC = "/sound/menu.mp3";
    public static final String GOAL_CHEER = "/sound/goal_cheer.mp3";
    public static final String GOAL_CONCEDED = "/sound/goal_conceded_boo.mp3";
    public static final String WHISTLE_FOUL = "/sound/whistle_foul.mp3";
    public static final String WHISTLE_HALFTIME = "/sound/whistle_halftime.mp3";
    public static final String WHISTLE_FULLTIME = "/sound/whistle_fulltime.mp3";

    private static final double MUSIC_VOLUME = 0.6;
    /** ความดังของเสียงเบา ๆ (เช่น ตอนได้จุดโทษ หรือเซฟได้) */
    public static final double SOFT_VOLUME = 0.4;

    private static final Map<String, Media> mediaCache = new HashMap<>();
    private static final Set<MediaPlayer> activeEffects = new HashSet<>(); // กัน GC เก็บทิ้งระหว่างเล่น
    private static final List<Runnable> muteListeners = new ArrayList<>();

    private static MediaPlayer menuPlayer;
    private static boolean menuWanted = false; // ตอนนี้อยู่หน้าเมนูหรือไม่ (ควรมีเพลงไหม)
    private static boolean muted = false;

    private SoundManager() {}

    // MUTE

    public static boolean isMuted() {
        return muted;
    }

    /** ปิด: เพลงหยุด (จำตำแหน่งไว้) และเอฟเฟกต์ไม่ดัง / เปิด: ถ้าอยู่หน้าเมนู เพลงเล่นต่อ */
    public static void setMuted(boolean value) {
        muted = value;
        if (muted) {
            for (MediaPlayer p : new ArrayList<>(activeEffects)) p.stop();
        }
        if (menuPlayer != null && menuWanted) {
            if (muted) menuPlayer.pause();
            else menuPlayer.play();
        }
        for (Runnable listener : muteListeners) listener.run();
    }

    /** ให้ปุ่มเปิด/ปิดเสียงหลายอันอัปเดตข้อความตรงกัน */
    public static void addMuteListener(Runnable listener) {
        muteListeners.add(listener);
    }

    // MENU MUSIC

    /** เริ่มเพลงเมนูใหม่ตั้งแต่ต้น (เรียกทุกครั้งที่กลับมาหน้าเมนู) */
    public static void playMenuMusic() {
        menuWanted = true;
        if (menuPlayer == null) {
            Media media = loadMedia(MENU_MUSIC);
            if (media == null) return;
            try {
                menuPlayer = new MediaPlayer(media);
                menuPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                menuPlayer.setVolume(MUSIC_VOLUME);
                final MediaPlayer created = menuPlayer;
                menuPlayer.setOnError(() -> System.err.println("[Sound] เพลงเมนูมีปัญหา: " + created.getError()));
            } catch (Exception e) {
                System.err.println("[Sound] สร้างเพลงเมนูไม่ได้: " + e);
                menuPlayer = null;
                return;
            }
        }
        menuPlayer.stop(); // กลับไปต้นเพลง
        if (!muted) menuPlayer.play();
    }

    /** หยุดเพลงเมนู (เรียกตอนออกจากหน้าเมนู) */
    public static void stopMenuMusic() {
        menuWanted = false;
        if (menuPlayer != null) menuPlayer.stop();
    }

    // EFFECTS

    /** เล่นเสียงเอฟเฟกต์หนึ่งครั้งด้วยความดังเต็ม (ไม่เล่นถ้าปิดเสียงอยู่) */
    public static void play(String path) {
        play(path, 1.0);
    }

    /** เล่นเสียงเอฟเฟกต์หนึ่งครั้งด้วยความดังที่กำหนด (0.0 - 1.0) */
    public static void play(String path, double volume) {
        if (muted) return;
        Media media = loadMedia(path);
        if (media == null) return;
        try {
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(volume);
            activeEffects.add(player);
            Runnable done = () -> {
                activeEffects.remove(player);
                player.dispose();
            };
            player.setOnEndOfMedia(done);
            player.setOnError(() -> {
                System.err.println("[Sound] เล่นเอฟเฟกต์ไม่ได้ (" + path + "): " + player.getError());
                done.run();
            });
            player.play();
        } catch (Exception e) {
            // เล่นไม่ได้ก็ข้าม
        }
    }

    /** เล่นเสียงหลังจากหน่วงเวลา (มิลลิวินาที) */
    public static void playAfter(String path, int delayMs) {
        PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
        delay.setOnFinished(e -> play(path));
        delay.play();
    }

    private static Media loadMedia(String path) {
        if (mediaCache.containsKey(path)) return mediaCache.get(path);
        Media media = null;
        try {
            URL url = SoundManager.class.getResource(path);
            if (url != null) media = new Media(url.toExternalForm());
            else System.err.println("[Sound] ไม่พบไฟล์เสียง: " + path);
        } catch (Exception e) {
            System.err.println("[Sound] โหลดไฟล์เสียงไม่ได้ (" + path + "): " + e);
            media = null;
        }
        mediaCache.put(path, media);
        return media;
    }
}