/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.impl.validation;

import java.util.Arrays;
import java.util.List;
import org.kie.server.services.impl.config.FileExtensionConfigService;

/**
 * Basic validator for file extensions in document uploads.
 * This class provides simple validation logic for both global and form-specific file extension restrictions.
 * Phase 1 implementation - basic validation only.
 */
public class FileExtensionValidator {
    
    /**
     * Validates if a file extension is allowed based on global configuration and form-specific settings.
     * 
     * @param fileName the name of the file to validate
     * @param allowedExtensions comma-separated list of allowed extensions for the specific form field
     * @return true if the file extension is allowed, false otherwise
     */
    public static boolean isValidFileExtension(String fileName, String allowedExtensions) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }
        
        // Check against global allowed extensions first
        if (!FileExtensionConfigService.isExtensionAllowed(extension)) {
            return false;
        }
        
        // If form-specific extensions are defined, check against them
        if (allowedExtensions != null && !allowedExtensions.trim().isEmpty()) {
            List<String> formAllowedExtensions = Arrays.asList(allowedExtensions.split(","));
            return formAllowedExtensions.stream()
                    .anyMatch(ext -> ext.trim().equalsIgnoreCase(extension));
        }
        
        return true;
    }
    
    /**
     * Extracts the file extension from a filename.
     * 
     * @param fileName the filename to extract extension from
     * @return the file extension (lowercase) or null if no valid extension found
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null) return null;
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * Generates a basic error message for file extension validation failures.
     * 
     * @param fileName the name of the file that failed validation
     * @param allowedExtensions comma-separated list of allowed extensions for the specific form field
     * @return a basic error message
     */
    public static String getValidationErrorMessage(String fileName, String allowedExtensions) {
        String extension = getFileExtension(fileName);
        
        if (extension == null) {
            return "File must have a valid extension";
        }
        
        if (!FileExtensionConfigService.isExtensionAllowed(extension)) {
            return "File extension '" + extension + "' is not allowed by system configuration";
        }
        
        if (allowedExtensions != null && !allowedExtensions.trim().isEmpty()) {
            return "File extension '" + extension + "' is not in the allowed list: " + allowedExtensions;
        }
        
        return "File extension '" + extension + "' is not allowed";
    }
}
