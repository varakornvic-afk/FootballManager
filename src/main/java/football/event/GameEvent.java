package football.event;

import java.awt.Color;

public interface GameEvent {
    String getEventTitle();
    String getPlayerName();
    String getImagePath();
    Color getThemeColor();

    abstract class BaseGameEvent implements GameEvent {
        protected String playerName, imagePath;
        public BaseGameEvent(String name, String path) {
            this.playerName = name;
            this.imagePath = path;
        }
        @Override
        public String getPlayerName() { return playerName; }
        @Override
        public String getImagePath() { return imagePath; }
    }

    // Popup ตอนทำประตู
    public static class GoalEvent extends BaseGameEvent {
        public GoalEvent(String name, String path) { super(name, path); }
        @Override
        public String getEventTitle() { return "GOAL! ⚽"; }
        @Override
        public Color getThemeColor() { return new Color(40, 167, 69); } // สีเขียว
    }

    // Popup ตอนฟาล์ว (ใบเหลือง / ใบแดง)
    public static class FoulEvent extends BaseGameEvent {
        private boolean isRed;
        public FoulEvent(String name, String path, boolean isRed) {
            super(name, path);
            this.isRed = isRed;
        }
        @Override
        public String getEventTitle() { return isRed ? "RED CARD! 🟥" : "YELLOW CARD! 🟨"; }
        @Override
        public Color getThemeColor() { return isRed ? new Color(220, 53, 69) : new Color(255, 193, 7); }
    }

    // Popup ตอนโกลเซฟประตู
    public static class SaveEvent extends BaseGameEvent {
        public SaveEvent(String name, String path) { super(name, path); }
        @Override
        public String getEventTitle() { return "GREAT SAVE! 🧤"; }
        @Override
        public Color getThemeColor() { return new Color(23, 162, 184); } // สีฟ้า cyan
    }
}