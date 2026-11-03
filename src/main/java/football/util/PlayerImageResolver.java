package football.util;

public final class PlayerImageResolver {

    private static final String DEFAULT_IMAGE = "/image/Player/Haalond.jpg";

    private PlayerImageResolver() {}

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