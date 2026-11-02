package football.util;

import football.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PlayerRepository {

    /**
     * โหลดรายชื่อผู้เล่นจากไฟล์ CSV ใน classpath
     * เช่น "/players.csv" คือไฟล์ที่วางไว้ที่ src/main/resources/players.csv
     */
    public static List<Player> loadPlayersFromCSV(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        InputStream in = PlayerRepository.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Resource not found on classpath: " + path);
        }

        List<Player> players = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // ข้ามบรรทัดหัวตาราง (Header)
                    continue;
                }

                if (line == null || line.isBlank() || line.trim().startsWith("#")) continue;

                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                try {
                    String name = parts[0].trim();
                    Position position = Position.valueOf(parts[1].trim().toUpperCase());
                    int pace = Integer.parseInt(parts[2].trim());
                    int passing = Integer.parseInt(parts[3].trim());
                    int heading = Integer.parseInt(parts[4].trim());
                    int stamina = Integer.parseInt(parts[5].trim());
                    int shoot = Integer.parseInt(parts[6].trim());
                    int defense = Integer.parseInt(parts[7].trim());

                    if (position == Position.GK) {
                        int saving = (parts.length >= 9 && !parts[8].trim().isEmpty())
                                ? Integer.parseInt(parts[8].trim())
                                : 0;
                        players.add(new GoalKeeper(name, pace, passing, heading, stamina, shoot, defense, saving));
                    } else {
                        players.add(new Player(name, position, pace, passing, heading, stamina, shoot, defense));
                    }
                } catch (IllegalArgumentException e) {
                    // 📌 ป้องกันกรณีข้อมูลแถวใดแถวหนึ่งผิดพลาด (เช่น Enum ไม่ตรง หรือ สถิติบวกรวมกันไม่ได้ 24)
                    System.err.println("Skipping invalid player row in CSV [" + line + "]: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }

        return players;
    }
}