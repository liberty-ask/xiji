package com.xiji.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 文件上传验证工具类
 * @author liberty
 */
@Slf4j
public class FileUploadValidator {
    
    /**
     * 允许的文件扩展名（正常文件类型）
     */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        // 图片
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico",
        // 文档
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp",
        // 表格数据
        "csv",
        // 视频
        "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v",
        // 音频
        "mp3", "wav", "ogg", "aac", "flac", "m4a", "wma",
        // 压缩文件
        "zip", "rar", "7z", "tar", "gz", "bz2",
        // 其他常见文件
        "json", "xml", "html", "css", "md", "yml", "yaml"
    ));
    
    /**
     * 禁止的文件扩展名（危险文件类型）
     */
    private static final Set<String> FORBIDDEN_EXTENSIONS = new HashSet<>(Arrays.asList(
        // 可执行文件
        "exe", "bat", "cmd", "com", "scr", "msi", "dll", "so", "dylib", "bin", "app",
        // 脚本文件
        "sh", "bash", "zsh", "ps1", "vbs", "js", "jsp", "asp", "aspx", "php", "py", "rb", "pl", "cgi",
        // Java可执行文件
        "jar", "war", "ear", "class",
        // 安装包
        "deb", "rpm", "pkg", "dmg", "apk", "ipa",
        // 其他危险文件
        "reg", "sys", "drv", "vxd", "ocx", "cab"
    ));
    
    /**
     * 允许的MIME类型映射
     */
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = new HashMap<>();
    
    static {
        // 图片
        ALLOWED_MIME_TYPES.put("image", new HashSet<>(Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp",
            "image/webp", "image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon"
        )));
        
        // 文档
        ALLOWED_MIME_TYPES.put("document", new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "application/rtf",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "text/csv",
            "application/vnd.ms-excel"
        )));
        
        // 视频
        ALLOWED_MIME_TYPES.put("video", new HashSet<>(Arrays.asList(
            "video/mp4", "video/x-msvideo", "video/quicktime", "video/x-ms-wmv",
            "video/x-flv", "video/x-matroska", "video/webm", "video/mp4"
        )));
        
        // 音频
        ALLOWED_MIME_TYPES.put("audio", new HashSet<>(Arrays.asList(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/aac",
            "audio/flac", "audio/mp4", "audio/x-ms-wma"
        )));
        
        // 压缩文件
        ALLOWED_MIME_TYPES.put("archive", new HashSet<>(Arrays.asList(
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed",
            "application/x-tar", "application/gzip", "application/x-bzip2"
        )));
        
        // 其他
        ALLOWED_MIME_TYPES.put("other", new HashSet<>(Arrays.asList(
            "application/json", "application/xml", "text/xml",
            "text/html", "text/css", "text/markdown",
            "application/x-yaml", "text/yaml"
        )));
    }
    
    /**
     * 最大文件大小（50MB）
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    /**
     * 验证文件是否安全
     * @param file 上传的文件
     * @return 验证结果，如果验证通过返回null，否则返回错误信息
     */
    public static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "文件为空";
        }
        
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return "文件大小不能超过50MB";
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "文件名不能为空";
        }
        
        // 防止路径遍历攻击
        if (originalFileName.contains("..") || originalFileName.contains("/") || originalFileName.contains("\\")) {
            return "文件名包含非法字符";
        }
        
        // 获取文件扩展名
        String extension = getFileExtension(originalFileName);
        if (extension == null || extension.isEmpty()) {
            return "文件没有扩展名";
        }
        
        extension = extension.toLowerCase();
        
        // 检查是否为禁止的文件类型
        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            log.warn("禁止上传的文件类型：{}", extension);
            return "不允许上传此类型的文件";
        }
        
        // 检查是否为允许的文件类型
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("不支持的文件类型：{}", extension);
            return "不支持的文件类型：" + extension;
        }
        
        // 检查MIME类型（如果可能被伪造，这里作为辅助验证）
        String contentType = file.getContentType();
        if (contentType != null && !isValidMimeType(contentType, extension)) {
            log.warn("文件MIME类型不匹配：文件名={}, 声明类型={}, 扩展名={}", originalFileName, contentType, extension);
            // 不直接拒绝，因为MIME类型可能不准确，只记录警告
        }
        
        return null; // 验证通过
    }
    
    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return null;
    }
    
    /**
     * 验证MIME类型是否与扩展名匹配
     */
    private static boolean isValidMimeType(String contentType, String extension) {
        // 将MIME类型转换为小写进行比较
        String lowerContentType = contentType.toLowerCase();
        
        // 遍历所有允许的MIME类型
        for (Set<String> mimeTypes : ALLOWED_MIME_TYPES.values()) {
            if (mimeTypes.contains(lowerContentType)) {
                return true;
            }
        }
        
        // 如果没有找到匹配的MIME类型，进行扩展名到MIME类型的映射检查
        // 这里可以添加更严格的检查逻辑
        
        return false;
    }
    
    /**
     * 获取文件大小限制（字节）
     */
    public static long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
    
    /**
     * 获取允许的文件扩展名列表
     */
    public static Set<String> getAllowedExtensions() {
        return new HashSet<>(ALLOWED_EXTENSIONS);
    }
    
    /**
     * 获取禁止的文件扩展名列表
     */
    public static Set<String> getForbiddenExtensions() {
        return new HashSet<>(FORBIDDEN_EXTENSIONS);
    }
}


