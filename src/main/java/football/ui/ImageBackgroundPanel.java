package football.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * พาเนลที่วาดภาพพื้นหลังเต็มพื้นที่ (รักษาสัดส่วน) พร้อมแผ่นดำโปร่งใสทับให้ตัวหนังสืออ่านง่าย
 * สลับภาพได้ด้วย {@link #setImage(String, int)}
 * ใส่เนื้อหาผ่าน {@link #getContent()} (BorderPane) แทน add(..., BorderLayout.X) ของ Swing
 */
public class ImageBackgroundPanel extends StackPane {
    private final Map<String, Image> cache = new HashMap<>();
    private final ImageView imageView = new ImageView();
    private final Rectangle overlay = new Rectangle();
    private final Rectangle clip = new Rectangle();
    private final BorderPane content = new BorderPane();
    private Image image;

    public ImageBackgroundPanel(String resourcePath, int overlayAlpha) {
        this.image = loadCached(resourcePath);

        imageView.setImage(image);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(false);
        imageView.setManaged(false);
        imageView.setMouseTransparent(true);

        overlay.setManaged(false);
        overlay.setMouseTransparent(true);
        setOverlayAlpha(overlayAlpha);

        setMinSize(0, 0);
        setClip(clip); // ตัดส่วนภาพที่เกินกรอบ
        getChildren().addAll(imageView, overlay, content);
    }

    /** ที่วางเนื้อหา (ใช้ setCenter / setSouth ... แทน BorderLayout) */
    public BorderPane getContent() {
        return content;
    }

    /** false ถ้าตอนนี้ไม่มีภาพ (ผู้เรียกควรใช้หน้าตาเดิมแทน) */
    public boolean hasImage() {
        return image != null;
    }

    /**
     * สลับไปใช้ภาพใหม่พร้อมความเข้มแผ่นดำใหม่
     *
     * @return true ถ้าสลับสำเร็จ, false ถ้าหาไฟล์ไม่เจอ (คงภาพเดิมไว้)
     */
    public boolean setImage(String resourcePath, int newOverlayAlpha) {
        Image loaded = loadCached(resourcePath);
        if (loaded == null) return false;
        this.image = loaded;
        imageView.setImage(loaded);
        setOverlayAlpha(newOverlayAlpha);
        requestLayout();
        return true;
    }

    private void setOverlayAlpha(int alpha) {
        overlay.setFill(Color.rgb(0, 0, 0, Math.max(0, Math.min(255, alpha)) / 255.0));
    }

    private Image loadCached(String resourcePath) {
        if (cache.containsKey(resourcePath)) return cache.get(resourcePath);
        Image loaded = load(resourcePath);
        cache.put(resourcePath, loaded);
        return loaded;
    }

    private static Image load(String resourcePath) {
        try (InputStream in = ImageBackgroundPanel.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            Image img = new Image(in);
            return img.isError() ? null : img;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = getWidth();
        double h = getHeight();

        clip.setWidth(w);
        clip.setHeight(h);
        overlay.setWidth(w);
        overlay.setHeight(h);
        overlay.relocate(0, 0);

        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) return;

        // ขยายให้เต็มพื้นที่โดยรักษาสัดส่วน (ส่วนที่เกินถูกตัดออก)
        double scale = Math.max(w / image.getWidth(), h / image.getHeight());
        double drawW = Math.ceil(image.getWidth() * scale);
        double drawH = Math.ceil(image.getHeight() * scale);
        imageView.setFitWidth(drawW);
        imageView.setFitHeight(drawH);
        imageView.relocate((w - drawW) / 2, (h - drawH) / 2);
    }
}
