package football.ui;

import football.event.GameEvent;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;

/**
 * ป๊อปอัปแจ้งเหตุการณ์ในแมตช์ (ประตู เซฟ ใบ จุดโทษ เปลี่ยนตัว)
 * แสดงรูปผู้เล่นและหัวข้อตามข้อมูลของ GameEvent
 */
public class EventPopupDialog extends Stage {
    private static final int IMG_BOX_W = 220;
    private static final int IMG_BOX_H = 270;

    public EventPopupDialog(Stage parent, GameEvent ev, boolean isHomeTeam) {
        initOwner(parent);
        initModality(Modality.NONE);
        initStyle(StageStyle.UNDECORATED);

        // กำหนดสีพื้นหลังและข้อความระบุฝั่ง
        Color bgColor = isHomeTeam ? Color.rgb(15, 35, 60) : Color.rgb(60, 20, 20); // ฟ้าเข้ม vs แดงเข้ม
        Color themeColor = ev.getThemeColor();

        // 1. หัวข้อ Event (event สร้างข้อความที่มีฝั่งทีมมาให้แล้ว เช่น GOAL! Your Team)
        EmojiText lblTitle = new EmojiText(ev.getEventTitle(isHomeTeam),
                Font.font("Segoe UI", FontWeight.BOLD, 24), themeColor);

        // 2. รูปนักเตะ (กรอบ 220x270px แต่รักษาสัดส่วนภาพ ไม่ยืด)
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(IMG_BOX_W, IMG_BOX_H);
        imgBox.setMinSize(IMG_BOX_W, IMG_BOX_H);
        imgBox.setMaxSize(IMG_BOX_W, IMG_BOX_H);

        String imgPath = ev.getImagePath();
        if (imgPath != null) {
            // ตัดส่วนเกินกรณีติด path แบบเดิมมา
            if (imgPath.startsWith("src/main/resources")) {
                imgPath = imgPath.replace("src/main/resources", "");
            }
            if (!imgPath.startsWith("/")) {
                imgPath = "/" + imgPath;
            }

            // ดึงไฟล์รูปจากในไฟล์ JAR ผ่าน Classpath
            URL imgUrl = getClass().getResource(imgPath);
            if (imgUrl != null) {
                Image img = new Image(imgUrl.toExternalForm());
                if (!img.isError()) {
                    ImageView view = new ImageView(img);
                    view.setFitWidth(IMG_BOX_W);
                    view.setFitHeight(IMG_BOX_H);
                    view.setPreserveRatio(true); // ย่อ/ขยายให้พอดีกรอบ โดยใช้อัตราเดียวกันทั้งกว้างและสูง
                    view.setSmooth(true);
                    imgBox.getChildren().add(view);
                }
            }
        }

        // 3. ชื่อนักเตะ
        EmojiText lblName = new EmojiText(ev.getPlayerName(),
                Font.font("Segoe UI", FontWeight.BOLD, 22), Color.WHITE);

        VBox root = new VBox(12, lblTitle, imgBox, lblName);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 35, 20, 35));
        root.setBackground(new Background(new BackgroundFill(bgColor, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setBorder(new Border(new BorderStroke(themeColor, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(4))));

        setScene(new Scene(root));

        // วางไว้กลางหน้าต่างหลัก (รู้ขนาดจริงหลังแสดงแล้ว)
        setOnShown(e -> {
            if (parent != null) {
                setX(parent.getX() + (parent.getWidth() - getWidth()) / 2);
                setY(parent.getY() + (parent.getHeight() - getHeight()) / 2);
            }
        });
    }

    /** แสดง popup แบบไม่บล็อก แล้วปิดเองหลัง 3.5 วินาที */
    public static EventPopupDialog showPopup(Stage parent, GameEvent ev, boolean isHomeTeam) {
        EventPopupDialog dialog = new EventPopupDialog(parent, ev, isHomeTeam);
        dialog.show();

        // ค้างแสดงผล 3.5 วินาที (3500 ms)
        PauseTransition autoClose = new PauseTransition(Duration.millis(3500));
        autoClose.setOnFinished(e -> dialog.close());
        autoClose.play();
        return dialog;
    }
}
