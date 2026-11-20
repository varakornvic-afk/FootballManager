package football.util;

/**
 * หา path ของรูปผู้เล่นจากชื่อ ถ้าไม่มีรูปหรือชื่อว่างจะใช้รูปเริ่มต้น
 */
public final class PlayerImageResolver {

    private static final String DEFAULT_IMAGE = "/image/Player/Haalond.jpg";

    private PlayerImageResolver() {}

    /** คืน path รูปของผู้เล่นตามชื่อ ใช้รูปเริ่มต้นเมื่อไม่พบ */
    public static String getPlayerImagePath(String name) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_IMAGE;
        }
        String resourcePath = "/image/Player/" + name.trim() + ".jpg";
        if (PlayerImageResolver.class.getResource(resourcePath) != null) {
            return resourcePath;
        }
        return DEFAULT_IMAGE;
    }
}