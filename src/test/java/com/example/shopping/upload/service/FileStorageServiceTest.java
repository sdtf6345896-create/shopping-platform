package com.example.shopping.upload.service;

import com.example.shopping.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    static final byte[] PNG_HEADER = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] WEBP_HEADER = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir
    Path uploadDir;

    private FileStorageService service() {
        return new FileStorageService(uploadDir.toString());
    }

    @Test
    void storeImage_savesRealImages_underRandomName() throws Exception {
        String url = service().storeImage(new MockMultipartFile("file", "../../evil.png", "image/png", PNG_HEADER));

        assertThat(url).matches("^/uploads/[0-9a-f-]{36}\\.png$");
        assertThat(Files.list(uploadDir)).hasSize(1);
    }

    @Test
    void storeImage_acceptsJpegAndWebpSignatures() {
        assertThat(service().storeImage(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG_HEADER)))
                .endsWith(".jpg");
        assertThat(service().storeImage(new MockMultipartFile("file", "a.webp", "image/webp", WEBP_HEADER)))
                .endsWith(".webp");
    }

    @Test
    void storeImage_rejectsFileWhoseContentDoesNotMatchDeclaredType() {
        MockMultipartFile disguised = new MockMultipartFile("file", "photo.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service().storeImage(disguised))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不是有效的圖片");
    }

    @Test
    void storeImage_rejectsJpegBytesDeclaredAsPng() {
        MockMultipartFile mismatched = new MockMultipartFile("file", "a.png", "image/png", JPEG_HEADER);

        assertThatThrownBy(() -> service().storeImage(mismatched)).isInstanceOf(BusinessException.class);
    }

    @Test
    void storeImage_rejectsNonImageContentType() {
        MockMultipartFile html = new MockMultipartFile("file", "a.html", "text/html", "<html>".getBytes());

        assertThatThrownBy(() -> service().storeImage(html))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPG / PNG / WEBP");
    }
}
