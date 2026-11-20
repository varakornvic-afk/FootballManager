package football.model;

/**
 * แผนการเล่น (แทคติก) ของทีม ทุกแผนต้องบอกชื่อ คำอธิบาย และแผนที่ตัวเองชนะ
 * รวมถึงให้คะแนนความเหมาะของผู้เล่นกับแผน (fitScore)
 * เป็นอินเทอร์เฟซเพื่อให้ MatchEngine เรียกใช้ได้โดยไม่ต้องรู้ว่าเป็นแผนไหน
 */
public interface Tactic {
    String getName();

    String getDescription();

    /** true ถ้าแทคติกนี้ได้เปรียบเมื่อเจอกับ other */
    boolean beats(Tactic other);

    int fitScore(Player player);
}