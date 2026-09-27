package com.example.shopping.upload.service;

import com.example.shopping.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("無法建立上傳目錄:" + this.uploadDir, e);
        }
    }

    /**
     * 儲存上傳的圖片,回傳可直接用來顯示的相對路徑(例如 /uploads/xxx.jpg)。
     * 檔名一律重新產生為 UUID,不採用使用者上傳的原始檔名,避免路徑穿越或檔名衝突。
     */
    public String storeImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("請選擇要上傳的檔案");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("僅支援 JPG / PNG / WEBP 格式的圖片");
        }

        // Content-Type 是客戶端宣告的,可以偽造;再檢查檔頭確認真的是宣告的圖片格式
        if (!hasImageSignature(file, contentType)) {
            throw new BusinessException("檔案內容不是有效的圖片");
        }

        String filename = UUID.randomUUID() + EXTENSION_BY_CONTENT_TYPE.get(contentType);
        Path target = uploadDir.resolve(filename);

        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException("檔案儲存失敗,請稍後再試");
        }

        return "/uploads/" + filename;
    }

    /** 依檔頭(magic bytes)判斷是否為宣告的圖片格式 */
    static boolean hasImageSignature(MultipartFile file, String contentType) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8
                    && (head[2] & 0xFF) == 0xFF;
            case "image/png" -> read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N'
                    && head[3] == 'G' && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
            case "image/webp" -> read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            default -> false;
        };
    }
}
