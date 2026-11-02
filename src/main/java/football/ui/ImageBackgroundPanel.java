package football.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * พาเนลที่วาดภาพพื้นหลังเต็มพื้นที่ (รักษาสัดส่วน) พร้อมแผ่นดำโปร่งใสทับให้ตัวหนังสืออ่านง่าย
 * สลับภาพได้ด้วย {@link #setImage(String, int)}
 */
public class ImageBackgroundPanel extends JPanel {
    private final Map<String, Image> cache = new HashMap<>();
    private Image image;
    private int overlayAlpha; // 0-255

    public ImageBackgroundPanel(String resourcePath, int overlayAlpha, LayoutManager layout) {
        super(layout);
        this.overlayAlpha = overlayAlpha;
        this.image = loadCached(resourcePath);
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
        this.overlayAlpha = newOverlayAlpha;
        repaint();
        return true;
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
            return ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;

        int imgW = image.getWidth(null);
        int imgH = image.getHeight(null);
        if (imgW <= 0 || imgH <= 0) return;

        // ขยายให้เต็มพื้นที่โดยรักษาสัดส่วน (ส่วนที่เกินถูกตัดออก)
        double scale = Math.max((double) getWidth() / imgW, (double) getHeight() / imgH);
        int drawW = (int) Math.ceil(imgW * scale);
        int drawH = (int) Math.ceil(imgH * scale);
        int x = (getWidth() - drawW) / 2;
        int y = (getHeight() - drawH) / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(image, x, y, drawW, drawH, null);
        if (overlayAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }
}