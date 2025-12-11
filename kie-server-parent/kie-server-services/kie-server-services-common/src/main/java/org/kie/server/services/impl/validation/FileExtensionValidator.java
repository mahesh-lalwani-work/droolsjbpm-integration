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
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.kie.server.services.impl.config.FileExtensionConfigService;

/**
 * Enhanced validator for file extensions in document uploads and form submissions.
 * 
 * <p>This class provides comprehensive validation logic for file extensions in the jBPM
 * Documents Allowed Files feature. It supports both global configuration validation
 * and form-specific extension restrictions.
 * 
 * <p>The validator implements a hierarchical validation approach:
 * <ol>
 *   <li><strong>Global validation:</strong> Checks against system-wide allowed extensions</li>
 *   <li><strong>Form-specific validation:</strong> Checks against form field restrictions</li>
 *   <li><strong>Multi-file validation:</strong> Handles DocumentCollection fields</li>
 * </ol>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Structured error reporting with ValidationResult objects</li>
 *   <li>Backward compatibility with existing boolean validation methods</li>
 *   <li>Support for both single files and document collections</li>
 *   <li>Case-insensitive extension matching</li>
 *   <li>Detailed error messages with file names and allowed extensions</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>
 * // Validate single file with structured result
 * ValidationResult result = FileExtensionValidator.validateFile("document.pdf", "pdf,docx");
 * if (!result.isValid()) {
 *     logger.error("Validation failed: {}", result.getMessage());
 * }
 * 
 * // Validate multiple files
 * List&lt;String&gt; files = Arrays.asList("doc1.pdf", "doc2.docx");
 * MultiFileValidationResult multiResult = FileExtensionValidator.validateFiles(files, "pdf,docx");
 * 
 * // Backward compatible validation
 * boolean isValid = FileExtensionValidator.isValidFileExtension("document.pdf", "pdf,docx");
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see MultiFileValidationResult
 * @see ValidationErrorCode
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
     * Validates a single file with detailed error information.
     * 
     * <p>This method performs comprehensive validation of a single file:
     * <ol>
     *   <li>Checks if the file name is null or empty</li>
     *   <li>Extracts and validates the file extension</li>
     *   <li>Validates against global allowed extensions</li>
     *   <li>Validates against form-specific allowed extensions (if provided)</li>
     * </ol>
     * 
     * <p>The method returns a structured ValidationResult with specific error codes
     * and detailed error messages for different failure scenarios.
     * 
     * @param fileName the name of the file to validate
     * @param allowedExtensions comma-separated list of allowed extensions for the specific form field
     * @return validation result with detailed error information
     */
    public static ValidationResult validateFile(String fileName, String allowedExtensions) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_EXTENSION,
                "File name cannot be null or empty"
            );
        }
        
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return ValidationResult.error(
                ValidationErrorCode.FILE_NO_EXTENSION,
                String.format("File '%s' has no extension. Only files with extensions are allowed.", fileName)
            );
        }
        
        // Check against global allowed extensions first
        if (!FileExtensionConfigService.isExtensionAllowed(extension)) {
            Set<String> globalAllowed = FileExtensionConfigService.getAllowedExtensions();
            return ValidationResult.error(
                ValidationErrorCode.INVALID_EXTENSION,
                String.format(
                    "File '%s' has extension '%s' which is not allowed by system configuration. " +
                    "Allowed extensions: %s",
                    fileName, extension, String.join(", ", globalAllowed)
                )
            );
        }
        
        // If form-specific extensions are defined, check against them
        if (allowedExtensions != null && !allowedExtensions.trim().isEmpty()) {
            Set<String> formAllowedExtensions = Arrays.stream(allowedExtensions.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            
            if (!formAllowedExtensions.contains(extension.toLowerCase())) {
                return ValidationResult.error(
                    ValidationErrorCode.INVALID_EXTENSION,
                    String.format(
                        "File '%s' has extension '%s' which is not in the form's allowed list: %s",
                        fileName, extension, allowedExtensions
                    )
                );
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates multiple files for DocumentCollection fields.
     * 
     * <p>This method validates a list of files and returns a comprehensive result
     * that includes validation status for each individual file as well as an
     * overall validation status.
     * 
     * <p>The result includes:
     * <ul>
     *   <li>Overall validation status (valid if all files are valid)</li>
     *   <li>Count of files validated, passed, and failed</li>
     *   <li>Individual validation results for each file</li>
     *   <li>List of invalid file names for easy error reporting</li>
     * </ul>
     * 
     * @param fileNames list of file names to validate
     * @param allowedExtensions comma-separated list of allowed extensions for the specific form field
     * @return multi-file validation result with detailed per-file information
     */
    public static MultiFileValidationResult validateFiles(List<String> fileNames, String allowedExtensions) {
        MultiFileValidationResult.Builder builder = new MultiFileValidationResult.Builder();
        
        for (String fileName : fileNames) {
            ValidationResult result = validateFile(fileName, allowedExtensions);
            builder.addResult(fileName, result);
        }
        
        return builder.build();
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
