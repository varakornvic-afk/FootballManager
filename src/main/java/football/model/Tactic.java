package football.model;

public interface Tactic {
    String getName();

    String getDescription();

    /** true ถ้าแทคติกนี้ได้เปรียบเมื่อเจอกับ other */
    boolean beats(Tactic other);

    int fitScore(Player player);
}