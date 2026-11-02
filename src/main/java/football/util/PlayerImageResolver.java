package football.util;

public final class PlayerImageResolver {

    private PlayerImageResolver() {}

    public static String getPlayerImagePath(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "/image/Player/allstars.jpeg";
        }
        String resourcePath = "/image/Player/" + name.trim() + ".jpg";
        if (PlayerImageResolver.class.getResource(resourcePath) != null) {
            return resourcePath;
        }
        return "/image/Player/Haalond.jpg";
    }
}