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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.server.services.impl.config.FileExtensionConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced interceptor for file upload validation with dynamic whitelist support.
 * 
 * <p>This interceptor provides comprehensive validation for file uploads in KIE Server,
 * supporting both global configuration and form-specific extension restrictions. It
 * implements the enhanced runtime validation requirements for the jBPM Documents
 * Allowed Files feature.
 * 
 * <p>Key features:
 * <ul>
 *   <li><strong>Dynamic whitelist:</strong> Uses web.xml configuration with runtime updates</li>
 *   <li><strong>Form-specific validation:</strong> Validates against form field restrictions</li>
 *   <li><strong>Multi-file support:</strong> Handles DocumentCollection fields</li>
 *   <li><strong>Process execution hooks:</strong> Integrates with process and task execution</li>
 *   <li><strong>Structured error reporting:</strong> Provides detailed validation results</li>
 * </ul>
 * 
 * <p>Validation hierarchy:
 * <ol>
 *   <li><strong>Global validation:</strong> Checks against system-wide allowed extensions</li>
 *   <li><strong>Form-specific validation:</strong> Checks against form field restrictions</li>
 *   <li><strong>Fallback validation:</strong> Uses default extensions if no specific rules</li>
 * </ol>
 * 
 * <p>Usage examples:
 * <pre>
 * // Intercept document upload
 * ValidationResult result = FileUploadInterceptor.validateDocumentUpload(
 *     "document.pdf", "pdf,docx", containerId);
 * 
 * // Intercept process start with form data
 * ValidationResult result = FileUploadInterceptor.validateProcessStart(
 *     processData, containerId, processId);
 * 
 * // Intercept task completion with form data
 * ValidationResult result = FileUploadInterceptor.validateTaskCompletion(
 *     taskData, containerId, taskId);
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see ValidationErrorCode
 * @see FileExtensionValidator
 * @see FormSubmissionValidator
 */
public class FileUploadInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadInterceptor.class);
    
    /**
     * Validates a document upload with dynamic whitelist support.
     * 
     * <p>This method performs comprehensive validation of document uploads:
     * <ol>
     *   <li>Checks against global allowed extensions from web.xml</li>
     *   <li>Validates against form-specific extensions (if provided)</li>
     *   <li>Provides detailed error information for validation failures</li>
     * </ol>
     * 
     * <p>The validation uses the dynamic whitelist from web.xml configuration,
     * which can be updated at runtime through system properties or configuration
     * management systems.
     * 
     * @param fileName the name of the file being uploaded
     * @param formAllowedExtensions comma-separated list of extensions allowed by the form
     * @param containerId the container ID for context-specific validation
     * @return validation result with detailed error information
     */
    public static ValidationResult validateDocumentUpload(String fileName, String formAllowedExtensions, String containerId) {
        logger.debug("Validating document upload: fileName={}, formExtensions={}, containerId={}", 
                    fileName, formAllowedExtensions, containerId);
        
        try {
            // Get current global allowed extensions (dynamic whitelist)
            Set<String> globalAllowedExtensions = FileExtensionConfigService.getAllowedExtensions();
            logger.debug("Current global allowed extensions: {}", globalAllowedExtensions);
            
            // Validate the file using the enhanced validator
            ValidationResult result = FileExtensionValidator.validateFile(fileName, formAllowedExtensions);
            
            if (!result.isValid()) {
                logger.warn("Document upload validation failed for file '{}': {} - {}", 
                           fileName, result.getErrorCode(), result.getMessage());
            } else {
                logger.debug("Document upload validation successful for file '{}'", fileName);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during document upload validation for file '{}'", fileName, e);
            return ValidationResult.error(
                ValidationErrorCode.VALIDATION_SYSTEM_ERROR,
                String.format("System error during file validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Validates multiple document uploads with dynamic whitelist support.
     * 
     * <p>This method performs comprehensive validation of multiple document uploads,
     * typically used for DocumentCollection fields. It validates each file individually
     * and provides an overall validation result.
     * 
     * @param fileNames list of file names being uploaded
     * @param formAllowedExtensions comma-separated list of extensions allowed by the form
     * @param containerId the container ID for context-specific validation
     * @return multi-file validation result with detailed error information
     */
    public static MultiFileValidationResult validateMultipleDocumentUploads(List<String> fileNames, 
                                                                           String formAllowedExtensions, 
                                                                           String containerId) {
        logger.debug("Validating multiple document uploads: fileCount={}, formExtensions={}, containerId={}", 
                    fileNames != null ? fileNames.size() : 0, formAllowedExtensions, containerId);
        
        try {
            // Get current global allowed extensions (dynamic whitelist)
            Set<String> globalAllowedExtensions = FileExtensionConfigService.getAllowedExtensions();
            logger.debug("Current global allowed extensions: {}", globalAllowedExtensions);
            
            // Validate multiple files using the enhanced validator
            MultiFileValidationResult result = FileExtensionValidator.validateFiles(fileNames, formAllowedExtensions);
            
            if (!result.isOverallValid()) {
                logger.warn("Multiple document upload validation failed: {} invalid files out of {}", 
                           result.getInvalidFileCount(), result.getTotalFileCount());
            } else {
                logger.debug("Multiple document upload validation successful for {} files", result.getTotalFileCount());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during multiple document upload validation", e);
            // Create a failure result for all files
            MultiFileValidationResult.Builder builder = new MultiFileValidationResult.Builder();
            for (String fileName : fileNames) {
                builder.addFileResult(fileName, ValidationResult.error(
                    ValidationErrorCode.VALIDATION_SYSTEM_ERROR,
                    String.format("System error during file validation: %s", e.getMessage())
                ));
            }
            return builder.build();
        }
    }
    
    /**
     * Validates process start data with form submission validation.
     * 
     * <p>This method intercepts process start operations and validates all document
     * fields in the process start form data. It uses the enhanced form submission
     * validator to ensure all files comply with extension restrictions.
     * 
     * <p>This is a process execution hook that integrates with the process start
     * workflow to provide comprehensive validation before process execution begins.
     * 
     * @param processData the process start form data containing document fields
     * @param containerId the container ID for context-specific validation
     * @param processId the process ID for context-specific validation
     * @return validation result indicating whether process start can proceed
     */
    public static ValidationResult validateProcessStart(Map<String, Object> processData, 
                                                       String containerId, 
                                                       String processId) {
        logger.debug("Validating process start: containerId={}, processId={}, dataKeys={}", 
                    containerId, processId, processData != null ? processData.keySet() : "null");
        
        try {
            // Use the form submission validator for comprehensive validation
            ValidationResult result = FormSubmissionValidator.validateProcessStartData(processData, null);
            
            if (!result.isValid()) {
                logger.warn("Process start validation failed for process '{}' in container '{}': {} - {}", 
                           processId, containerId, result.getErrorCode(), result.getMessage());
            } else {
                logger.debug("Process start validation successful for process '{}' in container '{}'", 
                           processId, containerId);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during process start validation for process '{}' in container '{}'", 
                        processId, containerId, e);
            return ValidationResult.error(
                ValidationErrorCode.PROCESS_START_VALIDATION_ERROR,
                String.format("System error during process start validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Validates task completion data with form submission validation.
     * 
     * <p>This method intercepts task completion operations and validates all document
     * fields in the task completion form data. It uses the enhanced form submission
     * validator to ensure all files comply with extension restrictions.
     * 
     * <p>This is a process execution hook that integrates with the task completion
     * workflow to provide comprehensive validation before task completion.
     * 
     * @param taskData the task completion form data containing document fields
     * @param containerId the container ID for context-specific validation
     * @param taskId the task ID for context-specific validation
     * @return validation result indicating whether task completion can proceed
     */
    public static ValidationResult validateTaskCompletion(Map<String, Object> taskData, 
                                                         String containerId, 
                                                         String taskId) {
        logger.debug("Validating task completion: containerId={}, taskId={}, dataKeys={}", 
                    containerId, taskId, taskData != null ? taskData.keySet() : "null");
        
        try {
            // Use the form submission validator for comprehensive validation
            ValidationResult result = FormSubmissionValidator.validateTaskCompletionData(taskData, null);
            
            if (!result.isValid()) {
                logger.warn("Task completion validation failed for task '{}' in container '{}': {} - {}", 
                           taskId, containerId, result.getErrorCode(), result.getMessage());
            } else {
                logger.debug("Task completion validation successful for task '{}' in container '{}'", 
                           taskId, containerId);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during task completion validation for task '{}' in container '{}'", 
                        taskId, containerId, e);
            return ValidationResult.error(
                ValidationErrorCode.TASK_COMPLETION_VALIDATION_ERROR,
                String.format("System error during task completion validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Refreshes the dynamic whitelist from configuration sources.
     * 
     * <p>This method allows runtime updates to the allowed file extensions by
     * refreshing the configuration from web.xml context parameters and system
     * properties. This enables dynamic configuration updates without server restart.
     * 
     * <p>Note: This method should be called when configuration changes are detected
     * or when a configuration refresh is requested through administrative interfaces.
     * 
     * @return validation result indicating whether the refresh was successful
     */
    public static ValidationResult refreshDynamicWhitelist() {
        logger.info("Refreshing dynamic whitelist configuration");
        
        try {
            // Re-initialize the configuration service
            FileExtensionConfigService.initialize();
            
            Set<String> currentExtensions = FileExtensionConfigService.getAllowedExtensions();
            logger.info("Dynamic whitelist refreshed successfully. Current extensions: {}", currentExtensions);
            
            return ValidationResult.valid();
            
        } catch (Exception e) {
            logger.error("Error refreshing dynamic whitelist", e);
            return ValidationResult.error(
                ValidationErrorCode.CONFIGURATION_REFRESH_ERROR,
                String.format("Failed to refresh dynamic whitelist: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Gets the current dynamic whitelist configuration.
     * 
     * <p>This method provides access to the current allowed file extensions
     * from the dynamic whitelist. It's useful for administrative interfaces
     * and debugging purposes.
     * 
     * @return set of currently allowed file extensions
     */
    public static Set<String> getCurrentDynamicWhitelist() {
        return FileExtensionConfigService.getAllowedExtensions();
    }
    
    /**
     * Validates if a specific extension is currently allowed by the dynamic whitelist.
     * 
     * <p>This method provides a quick check against the current dynamic whitelist
     * configuration without performing full file validation.
     * 
     * @param extension the file extension to check
     * @return true if the extension is currently allowed, false otherwise
     */
    public static boolean isExtensionAllowedInDynamicWhitelist(String extension) {
        return FileExtensionConfigService.isExtensionAllowed(extension);
    }
}
