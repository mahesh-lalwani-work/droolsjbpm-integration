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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.server.services.impl.config.FileExtensionConfigService;

/**
 * Service for business rule validation of file extensions during process and task execution.
 * 
 * <p>This service provides comprehensive validation logic for file extensions during
 * business process execution, including:
 * <ul>
 *   <li>Process start validation</li>
 *   <li>Task completion validation</li>
 *   <li>Multi-file validation for DocumentCollection fields</li>
 *   <li>Integration with process variables and form data</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Validates file extensions against hierarchical configuration (form → global → defaults)</li>
 *   <li>Supports both single files and document collections</li>
 *   <li>Provides structured error reporting for process execution</li>
 *   <li>Integrates with jBPM process variables and task data</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>
 * // Validate process start with file attachments
 * Map&lt;String, Object&gt; processVariables = new HashMap&lt;&gt;();
 * processVariables.put("document", "contract.pdf");
 * ValidationResult result = BusinessRuleValidationService.validateProcessStart(processVariables, "pdf,docx");
 * 
 * // Validate task completion with multiple files
 * List&lt;String&gt; files = Arrays.asList("doc1.pdf", "doc2.docx");
 * ValidationResult taskResult = BusinessRuleValidationService.validateTaskCompletion(files, "pdf,docx");
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see ValidationErrorCode
 * @see FileExtensionValidator
 */
public class BusinessRuleValidationService {
    
    /**
     * Validates file extensions during process start.
     * 
     * <p>This method validates all file-related variables in the process start data
     * against the configured allowed extensions. It checks:
     * <ul>
     *   <li>Document fields with file names</li>
     *   <li>DocumentCollection fields with multiple files</li>
     *   <li>Process variables containing file references</li>
     * </ul>
     * 
     * @param processVariables map of process variables containing file data
     * @param allowedExtensions comma-separated list of allowed extensions for the process
     * @return validation result with detailed error information
     */
    public static ValidationResult validateProcessStart(Map<String, Object> processVariables, String allowedExtensions) {
        if (processVariables == null || processVariables.isEmpty()) {
            return ValidationResult.valid();
        }
        
        List<String> allFiles = new ArrayList<>();
        
        // Extract file names from process variables
        for (Map.Entry<String, Object> entry : processVariables.entrySet()) {
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // Handle single file (Document field)
            if (value instanceof String) {
                String fileName = (String) value;
                if (isFileReference(fileName)) {
                    allFiles.add(fileName);
                }
            }
            // Handle multiple files (DocumentCollection field)
            else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> fileList = (List<Object>) value;
                for (Object fileObj : fileList) {
                    if (fileObj instanceof String) {
                        String fileName = (String) fileObj;
                        if (isFileReference(fileName)) {
                            allFiles.add(fileName);
                        }
                    }
                }
            }
        }
        
        // Validate all collected files
        if (!allFiles.isEmpty()) {
            MultiFileValidationResult multiResult = FileExtensionValidator.validateFiles(allFiles, allowedExtensions);
            if (!multiResult.isOverallValid()) {
                return ValidationResult.error(
                    ValidationErrorCode.PROCESS_START_VALIDATION_FAILED,
                    String.format(
                        "Process start validation failed: %d files failed validation. Invalid files: %s",
                        multiResult.getFilesFailed(),
                        multiResult.getInvalidFileNames()
                    )
                );
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates file extensions during task completion.
     * 
     * <p>This method validates file attachments and form data during task completion,
     * ensuring all files meet the extension requirements for the specific task.
     * 
     * @param taskData map of task data containing file information
     * @param allowedExtensions comma-separated list of allowed extensions for the task
     * @return validation result with detailed error information
     */
    public static ValidationResult validateTaskCompletion(Map<String, Object> taskData, String allowedExtensions) {
        if (taskData == null || taskData.isEmpty()) {
            return ValidationResult.valid();
        }
        
        List<String> allFiles = new ArrayList<>();
        
        // Extract file names from task data
        for (Map.Entry<String, Object> entry : taskData.entrySet()) {
            Object value = entry.getValue();
            
            if (value == null) {
                continue;
            }
            
            // Handle single file
            if (value instanceof String) {
                String fileName = (String) value;
                if (isFileReference(fileName)) {
                    allFiles.add(fileName);
                }
            }
            // Handle multiple files
            else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> fileList = (List<Object>) value;
                for (Object fileObj : fileList) {
                    if (fileObj instanceof String) {
                        String fileName = (String) fileObj;
                        if (isFileReference(fileName)) {
                            allFiles.add(fileName);
                        }
                    }
                }
            }
        }
        
        // Validate all collected files
        if (!allFiles.isEmpty()) {
            MultiFileValidationResult multiResult = FileExtensionValidator.validateFiles(allFiles, allowedExtensions);
            if (!multiResult.isOverallValid()) {
                return ValidationResult.error(
                    ValidationErrorCode.TASK_COMPLETION_VALIDATION_FAILED,
                    String.format(
                        "Task completion validation failed: %d files failed validation. Invalid files: %s",
                        multiResult.getFilesFailed(),
                        multiResult.getInvalidFileNames()
                    )
                );
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates file extensions for a specific form field.
     * 
     * <p>This method validates file extensions for a specific form field,
     * supporting both single files and document collections.
     * 
     * @param fieldName the name of the form field
     * @param fieldValue the value of the form field (String for single file, List for multiple)
     * @param allowedExtensions comma-separated list of allowed extensions for this field
     * @return validation result with detailed error information
     */
    public static ValidationResult validateFormField(String fieldName, Object fieldValue, String allowedExtensions) {
        if (fieldValue == null) {
            return ValidationResult.valid();
        }
        
        // Handle single file
        if (fieldValue instanceof String) {
            String fileName = (String) fieldValue;
            if (isFileReference(fileName)) {
                ValidationResult result = FileExtensionValidator.validateFile(fileName, allowedExtensions);
                if (!result.isValid()) {
                    return ValidationResult.error(
                        ValidationErrorCode.FORM_CONTENT_INVALID,
                        String.format("Field '%s': %s", fieldName, result.getMessage())
                    );
                }
            }
        }
        // Handle multiple files
        else if (fieldValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> fileList = (List<Object>) fieldValue;
            List<String> files = new ArrayList<>();
            
            for (Object fileObj : fileList) {
                if (fileObj instanceof String) {
                    String fileName = (String) fileObj;
                    if (isFileReference(fileName)) {
                        files.add(fileName);
                    }
                }
            }
            
            if (!files.isEmpty()) {
                MultiFileValidationResult multiResult = FileExtensionValidator.validateFiles(files, allowedExtensions);
                if (!multiResult.isOverallValid()) {
                    return ValidationResult.error(
                        ValidationErrorCode.FORM_CONTENT_INVALID,
                        String.format("Field '%s': %d files failed validation. Invalid files: %s", 
                                     fieldName, multiResult.getFilesFailed(), multiResult.getInvalidFileNames())
                    );
                }
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates file extensions against the hierarchical configuration.
     * 
     * <p>This method implements the 3-tier validation hierarchy:
     * <ol>
     *   <li>Form-specific extensions (if provided)</li>
     *   <li>Global allowed extensions from web.xml/Manage Preferences</li>
     *   <li>Default extensions as fallback</li>
     * </ol>
     * 
     * @param fileName the file name to validate
     * @param formAllowedExtensions form-specific allowed extensions (can be null)
     * @return validation result with detailed error information
     */
    public static ValidationResult validateHierarchical(String fileName, String formAllowedExtensions) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        // Determine effective allowed extensions using hierarchy
        String effectiveExtensions = determineEffectiveExtensions(formAllowedExtensions);
        
        return FileExtensionValidator.validateFile(fileName, effectiveExtensions);
    }
    
    /**
     * Determines the effective allowed extensions using the hierarchical configuration.
     * 
     * @param formAllowedExtensions form-specific extensions (highest priority)
     * @return effective extensions string for validation
     */
    private static String determineEffectiveExtensions(String formAllowedExtensions) {
        // Tier 1: Form-specific extensions (highest priority)
        if (formAllowedExtensions != null && !formAllowedExtensions.trim().isEmpty()) {
            return formAllowedExtensions;
        }
        
        // Tier 2: Global allowed extensions from web.xml/Manage Preferences
        Set<String> globalAllowed = FileExtensionConfigService.getAllowedExtensions();
        if (!globalAllowed.isEmpty()) {
            return String.join(",", globalAllowed);
        }
        
        // Tier 3: Default extensions (fallback)
        return "pdf,docx,xlsx,txt,jpg,png"; // Default fallback
    }
    
    /**
     * Checks if a string value represents a file reference.
     * 
     * <p>This method determines if a string value is likely a file reference
     * based on common patterns (contains extension, not just a path, etc.).
     * 
     * @param value the value to check
     * @return true if the value appears to be a file reference
     */
    private static boolean isFileReference(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        // Check if it has a file extension
        int lastDotIndex = value.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == value.length() - 1) {
            return false;
        }
        
        // Check if the extension part looks like a file extension
        String extension = value.substring(lastDotIndex + 1).toLowerCase();
        return extension.matches("^[a-zA-Z0-9]{1,10}$"); // 1-10 alphanumeric characters
    }
    
    /**
     * Validates file extensions for process execution with error handling.
     * 
     * <p>This method provides robust validation with comprehensive error handling
     * for process execution scenarios.
     * 
     * @param processData map containing process data with file references
     * @param allowedExtensions comma-separated list of allowed extensions
     * @return validation result with detailed error information
     */
    public static ValidationResult validateProcessExecution(Map<String, Object> processData, String allowedExtensions) {
        try {
            return validateProcessStart(processData, allowedExtensions);
        } catch (Exception e) {
            return ValidationResult.error(
                ValidationErrorCode.PROCESS_START_VALIDATION_ERROR,
                String.format("Error during process validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Validates file extensions for task execution with error handling.
     * 
     * <p>This method provides robust validation with comprehensive error handling
     * for task execution scenarios.
     * 
     * @param taskData map containing task data with file references
     * @param allowedExtensions comma-separated list of allowed extensions
     * @return validation result with detailed error information
     */
    public static ValidationResult validateTaskExecution(Map<String, Object> taskData, String allowedExtensions) {
        try {
            return validateTaskCompletion(taskData, allowedExtensions);
        } catch (Exception e) {
            return ValidationResult.error(
                ValidationErrorCode.TASK_COMPLETION_VALIDATION_ERROR,
                String.format("Error during task validation: %s", e.getMessage())
            );
        }
    }
}
