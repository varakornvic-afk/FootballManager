package football.logic;

/** เก็บข้อมูลการเปลี่ยนตัว (Sub Out / Sub In) */
public class SubInfo {
    public final String playerOut;
    public final String playerIn;
    public final boolean isHome;

    public SubInfo(String playerOut, String playerIn, boolean isHome) {
        this.playerOut = playerOut;
        this.playerIn = playerIn;
        this.isHome = isHome;
    }
}