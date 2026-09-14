package com.example.shopping.config;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 開發環境用:專案首次啟動時,若無任何管理員帳號,建立一組預設帳密方便登入測試。
 * 預設帳號:admin / admin123
 */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("系統管理員");
            adminRepository.save(admin);
        }
    }
}
