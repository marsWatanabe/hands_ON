package com.aojt.mgmt.config;

import com.aojt.mgmt.entity.InventoryCenter;
import com.aojt.mgmt.repository.InventoryCenterRepository;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Inserts sample inventory centers for local development (app.seed-data=true). */
@Configuration
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataInitializer {

    @Bean
    ApplicationRunner seedCenters(InventoryCenterRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    center("東京センター", "100-0001", "東京都千代田区千代田1-1-1", "03-1234-5678", "山田太郎", 0, 1000, 850),
                    center("大阪センター", "530-0001", "大阪府大阪市北区梅田1-1-1", "06-1234-5678", "田中一郎", 0, 800, 120),
                    center("名古屋センター", "450-0002", "愛知県名古屋市中村区名駅1-1-1", "052-123-4567", "佐藤花子", 1, 600, 0),
                    center("福岡センター", "810-0001", "福岡県福岡市中央区天神1-1-1", "092-123-4567", "鈴木次郎", 0, 500, 300),
                    center("札幌センター", "060-0001", "北海道札幌市中央区北1条西1-1", "011-123-4567", "高橋三郎", 1, 400, 40)));
        };
    }

    private static InventoryCenter center(String name, String postCode, String address, String phone,
            String manager, int status, int max, int current) {
        InventoryCenter c = new InventoryCenter();
        c.setCenterName(name);
        c.setPostCode(postCode);
        c.setAddress(address);
        c.setPhoneNumber(phone);
        c.setManagerName(manager);
        c.setOperationalStatus(status);
        c.setMaxStorageCapacity(max);
        c.setCurrentStorageCapacity(current);
        c.setNotes("");
        return c;
    }
}
