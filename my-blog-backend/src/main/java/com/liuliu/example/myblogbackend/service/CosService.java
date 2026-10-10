package com.liuliu.example.myblogbackend.service;

import com.liuliu.example.myblogbackend.common.ErrorCode;
import com.liuliu.example.myblogbackend.exception.BusinessException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 腾讯云 COS 图片上传服务
 * <p>
 * 上走后端转发模式（前端→后端→COS），密钥不暴露给前端。
 */
@Slf4j
@Service
public class CosService {

    private final COSClient cosClient;
    private final String bucket;
    private final String domainPrefix;

    @Value("${cos.avatar-max-bytes:2097152}")
    private long avatarMaxBytes;

    @Value("${cos.article-max-bytes:5242880}")
    private long articleMaxBytes;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    public CosService(
            @Value("${cos.secret-id}") String secretId,
            @Value("${cos.secret-key}") String secretKey,
            @Value("${cos.bucket}") String bucket,
            @Value("${cos.region}") String region) {
        var credentials = new BasicCOSCredentials(secretId, secretKey);
        var clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        this.cosClient = new COSClient(credentials, clientConfig);
        this.bucket = bucket;
        this.domainPrefix = "https://" + bucket + ".cos." + region + ".myqcloud.com";
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    /**
     * 上传图片到 COS
     *
     * @param file  上传的文件
     * @param type  "avatar" 或 "article"
     * @param userId 用户 ID（用于 key 隔离）
     * @return 图片公网 URL
     */
    public String uploadImage(MultipartFile file, String type, Long userId) {
        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 jpg/png/gif/webp 格式");
        }

        // 2. 校验文件大小
        long maxBytes = "avatar".equals(type) ? avatarMaxBytes : articleMaxBytes;
        if (file.getSize() > maxBytes) {
            long maxMb = maxBytes / 1024 / 1024;
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过 " + maxMb + "MB");
        }

        // 3. 生成 key：avatar/{userId}/{uuid}.{ext} 或 article/{userId}/{timestamp}.{ext}
        String ext = getExtension(file.getOriginalFilename(), contentType);
        String key = "avatar".equals(type)
                ? "avatar/" + userId + "/" + UUID.randomUUID() + "." + ext
                : "article/" + userId + "/" + System.currentTimeMillis() + "." + ext;

        // 4. 上传
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);

            PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, metadata);
            cosClient.putObject(request);

            String url = domainPrefix + "/" + key;
            log.info("COS 上传成功: type={}, userId={}, key={}", type, userId, key);
            return url;
        } catch (Exception e) {
            log.error("COS 上传失败: type={}, userId={}", type, userId, e);
            throw new BusinessException(ErrorCode.ERROR, "图片上传失败，请稍后重试");
        }
    }

    /** 从文件名或 contentType 推断扩展名 */
    private String getExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
