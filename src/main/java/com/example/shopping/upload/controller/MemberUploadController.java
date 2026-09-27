package com.example.shopping.upload.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import com.example.shopping.upload.dto.UploadResponse;
import com.example.shopping.upload.service.FileStorageService;
import com.example.shopping.upload.service.MemberUploadLimiter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 會員上傳圖片(目前用於評論照片),需登入且有頻率限制 */
@RestController
@RequestMapping("/api/uploads")
public class MemberUploadController {

    private final FileStorageService fileStorageService;
    private final MemberUploadLimiter uploadLimiter;

    public MemberUploadController(FileStorageService fileStorageService, MemberUploadLimiter uploadLimiter) {
        this.fileStorageService = fileStorageService;
        this.uploadLimiter = uploadLimiter;
    }

    @PostMapping("/image")
    public ApiResponse<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        uploadLimiter.acquire(SecurityUtils.getCurrentUserId());
        return ApiResponse.success("上傳成功", new UploadResponse(fileStorageService.storeImage(file)));
    }
}
