package com.ssafy.bbatty.global.util;

import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FilePathValidator {

    private static final List<String> FORBIDDEN_PATTERNS = Arrays.asList(
            "..", "~", "/etc", "/bin", "/usr", "/var", "/sys", "/proc", "/dev", "/root"
    );

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    public static void validateImagePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_FILE_PATH);
        }

        try {
            Path path = Paths.get(filePath);
            
            checkSecurityViolations(filePath);
            checkImageExtension(filePath);
            checkPathTraversal(path);
            
        } catch (InvalidPathException e) {
            throw new ApiException(ErrorCode.INVALID_FILE_PATH);
        }
    }

    public static void validateImageExists(String filePath) {
        validateImagePath(filePath);
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND);
        }
        
        if (!file.canRead()) {
            throw new ApiException(ErrorCode.FILE_ACCESS_DENIED);
        }
    }

    private static void checkSecurityViolations(String filePath) {
        String normalizedPath = filePath.toLowerCase();
        
        for (String forbiddenPattern : FORBIDDEN_PATTERNS) {
            if (normalizedPath.contains(forbiddenPattern)) {
                throw new ApiException(ErrorCode.FILE_PATH_SECURITY_VIOLATION);
            }
        }
    }

    private static void checkImageExtension(String filePath) {
        String lowerCasePath = filePath.toLowerCase();
        boolean hasValidExtension = ALLOWED_IMAGE_EXTENSIONS.stream()
                .anyMatch(lowerCasePath::endsWith);
        
        if (!hasValidExtension) {
            throw new ApiException(ErrorCode.INVALID_FILE_PATH);
        }
    }

    private static void checkPathTraversal(Path path) {
        Path normalizedPath = path.normalize();
        
        if (!normalizedPath.equals(path)) {
            throw new ApiException(ErrorCode.FILE_PATH_SECURITY_VIOLATION);
        }
    }
}