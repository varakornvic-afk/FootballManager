package football.event;

import javafx.scene.paint.Color;

public interface GameEvent {
    String getEventTitle();
    String getPlayerName();
    String getImagePath();
    Color getThemeColor();

    // 📌 รองรับการส่งฝั่งทีม (Home/Away) เพื่อปรับข้อความ Pop-up
    default String getEventTitle(boolean isHome) {
        return getEventTitle();
    }

    abstract class BaseGameEvent implements GameEvent {
        protected String playerName, imagePath;
        protected Boolean isHome;

        public BaseGameEvent(String name, String path) {
            this.playerName = name;
            this.imagePath = path;
            this.isHome = null;
        }

        public BaseGameEvent(String name, String path, boolean isHome) {
            this.playerName = name;
            this.imagePath = path;
            this.isHome = isHome;
        }

        @Override
        public String getPlayerName() { return playerName; }
        @Override
        public String getImagePath() { return imagePath; }

        protected String getTeamPrefix(boolean isHomeSide) {
            return isHomeSide ? "Your Team" : "Opponent";
        }
    }

    // ⚽ 1. Popup ตอนทำประตู (ชื่อนักเตะแสดงใต้รูปอยู่แล้ว จึงไม่ใส่ในหัวข้อ)
    public static class GoalEvent extends BaseGameEvent {
        public GoalEvent(String name, String path) { super(name, path); }
        public GoalEvent(String name, String path, boolean isHome) { super(name, path, isHome); }

        @Override
        public String getEventTitle() {
            if (isHome != null) return getEventTitle(isHome);
            return "⚽ GOAL!";
        }

        @Override
        public String getEventTitle(boolean isHome) {
            return String.format("⚽ GOAL! %s", getTeamPrefix(isHome));
        }

        @Override
        public Color getThemeColor() { return Color.rgb(40, 167, 69); } // สีเขียว
    }

    // 🟨🟥 2. Popup ตอนฟาล์ว (ใบเหลือง / ใบแดง)
    public static class FoulEvent extends BaseGameEvent {
        private final boolean isRed;

        public FoulEvent(String name, String path, boolean isRed) {
            super(name, path);
            this.isRed = isRed;
        }

        public FoulEvent(String name, String path, boolean isRed, boolean isHome) {
            super(name, path, isHome);
            this.isRed = isRed;
        }

        @Override
        public String getEventTitle() {
            if (isHome != null) return getEventTitle(isHome);
            return isRed ? "🟥 RED CARD & Sent Off! 🚪" : "🟨 YELLOW CARD!";
        }

        @Override
        public String getEventTitle(boolean isHome) {
            String teamText = getTeamPrefix(isHome);
            if (isRed) {
                return String.format("🟥 RED CARD! %s sent off 🚪", teamText);
            } else {
                return String.format("🟨 YELLOW CARD! %s", teamText);
            }
        }

        @Override
        public Color getThemeColor() {
            return isRed ? Color.rgb(220, 53, 69) : Color.rgb(255, 193, 7); // ใบแดงสีแดง / ใบเหลืองสีเหลือง
        }
    }

    // 🧤 3. Popup ตอนโกลเซฟประตู
    public static class SaveEvent extends BaseGameEvent {
        public SaveEvent(String name, String path) { super(name, path); }
        public SaveEvent(String name, String path, boolean isHome) { super(name, path, isHome); }

        @Override
        public String getEventTitle() {
            if (isHome != null) return getEventTitle(isHome);
            return "🧤 GREAT SAVE!";
        }

        @Override
        public String getEventTitle(boolean isHome) {
            String teamText = isHome ? "Your Keeper" : "Opponent Keeper";
            return String.format("🧤 GREAT SAVE! %s 🛑", teamText);
        }

        @Override
        public Color getThemeColor() { return Color.rgb(23, 162, 184); } // สีฟ้า Cyan
    }

    // 🎯 4. Popup ตอนได้จุดโทษ
    public static class PenaltyEvent extends BaseGameEvent {
        public PenaltyEvent(String name, String path) { super(name, path); }
        public PenaltyEvent(String name, String path, boolean isHome) { super(name, path, isHome); }

        @Override
        public String getEventTitle() {
            if (isHome != null) return getEventTitle(isHome);
            return "🎯 PENALTY KICK!";
        }

        @Override
        public String getEventTitle(boolean isHome) {
            return String.format("🎯 PENALTY! %s awarded", getTeamPrefix(isHome));
        }

        @Override
        public Color getThemeColor() { return Color.rgb(153, 102, 255); } // สีม่วง
    }

    // 🔄 5. Popup ตอนเปลี่ยนตัวแบบ 2-in-1 (Sub Out & Sub In)
    public static class SubstitutionEvent extends BaseGameEvent {
        private final String playerOut;
        private final String playerIn;

        public SubstitutionEvent(String playerOut, String playerIn, String path) {
            super(playerIn, path);
            this.playerOut = playerOut;
            this.playerIn = playerIn;
        }

        public SubstitutionEvent(String playerOut, String playerIn, String path, boolean isHome) {
            super(playerIn, path, isHome);
            this.playerOut = playerOut;
            this.playerIn = playerIn;
        }

        public String getPlayerOut() { return playerOut; }
        public String getPlayerIn() { return playerIn; }

        @Override
        public String getPlayerName() {
            return String.format("🔴 OUT: %s   ➡   🟢 IN: %s", playerOut, playerIn);
        }

        @Override
        public String getEventTitle() {
            if (isHome != null) return getEventTitle(isHome);
            return "🔄 SUBSTITUTION";
        }

        @Override
        public String getEventTitle(boolean isHome) {
            String teamText = getTeamPrefix(isHome);
            return String.format("🔄 SUBSTITUTION (%s)", teamText);
        }

        @Override
        public Color getThemeColor() { return Color.rgb(255, 140, 0); } // สีส้ม
    }
}