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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for form submission data containing document fields.
 * 
 * <p>This validator provides comprehensive validation for form submission data
 * that contains document fields, supporting both process start and task completion
 * scenarios. It integrates with the business rule validation service to ensure
 * all file extensions comply with the configured restrictions.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Process start form data validation</li>
 *   <li>Task completion form data validation</li>
 *   <li>Document field extraction and validation</li>
 *   <li>Multi-file support for DocumentCollection fields</li>
 *   <li>Integration with hierarchical validation rules</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>
 * // Validate process start form data
 * Map&lt;String, Object&gt; processData = new HashMap&lt;&gt;();
 * processData.put("document", "contract.pdf");
 * ValidationResult result = FormSubmissionValidator.validateProcessStartData(processData, "pdf,docx");
 * 
 * // Validate task completion form data
 * Map&lt;String, Object&gt; taskData = new HashMap&lt;&gt;();
 * taskData.put("attachments", Arrays.asList("doc1.pdf", "doc2.docx"));
 * ValidationResult result = FormSubmissionValidator.validateTaskCompletionData(taskData, "pdf,docx");
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see ValidationErrorCode
 * @see BusinessRuleValidationService
 */
public class FormSubmissionValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(FormSubmissionValidator.class);
    
    /**
     * Validates process start form data for document field compliance.
     * 
     * <p>This method validates all document fields in the process start form data
     * against the configured allowed extensions. It extracts document fields
     * and validates them using the business rule validation service.
     * 
     * @param processData the process start form data containing document fields
     * @param allowedExtensions comma-separated list of allowed extensions (can be null for hierarchical validation)
     * @return validation result indicating whether process start can proceed
     */
    public static ValidationResult validateProcessStartData(Map<String, Object> processData, String allowedExtensions) {
        logger.debug("Validating process start form data with {} fields", 
                    processData != null ? processData.size() : 0);
        
        if (processData == null || processData.isEmpty()) {
            return ValidationResult.valid();
        }
        
        try {
            // Use the business rule validation service for comprehensive validation
            ValidationResult result = BusinessRuleValidationService.validateProcessStart(processData, allowedExtensions);
            
            if (!result.isValid()) {
                logger.warn("Process start form data validation failed: {} - {}", 
                           result.getErrorCode(), result.getMessage());
            } else {
                logger.debug("Process start form data validation successful");
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during process start form data validation", e);
            return ValidationResult.error(
                ValidationErrorCode.PROCESS_START_VALIDATION_ERROR,
                String.format("System error during process start form validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Validates task completion form data for document field compliance.
     * 
     * <p>This method validates all document fields in the task completion form data
     * against the configured allowed extensions. It extracts document fields
     * and validates them using the business rule validation service.
     * 
     * @param taskData the task completion form data containing document fields
     * @param allowedExtensions comma-separated list of allowed extensions (can be null for hierarchical validation)
     * @return validation result indicating whether task completion can proceed
     */
    public static ValidationResult validateTaskCompletionData(Map<String, Object> taskData, String allowedExtensions) {
        logger.debug("Validating task completion form data with {} fields", 
                    taskData != null ? taskData.size() : 0);
        
        if (taskData == null || taskData.isEmpty()) {
            return ValidationResult.valid();
        }
        
        try {
            // Use the business rule validation service for comprehensive validation
            ValidationResult result = BusinessRuleValidationService.validateTaskCompletion(taskData, allowedExtensions);
            
            if (!result.isValid()) {
                logger.warn("Task completion form data validation failed: {} - {}", 
                           result.getErrorCode(), result.getMessage());
            } else {
                logger.debug("Task completion form data validation successful");
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during task completion form data validation", e);
            return ValidationResult.error(
                ValidationErrorCode.TASK_COMPLETION_VALIDATION_ERROR,
                String.format("System error during task completion form validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Validates a specific form field for document compliance.
     * 
     * <p>This method validates a specific form field that may contain document data,
     * supporting both single files and document collections.
     * 
     * @param fieldName the name of the form field
     * @param fieldValue the value of the form field
     * @param allowedExtensions comma-separated list of allowed extensions (can be null for hierarchical validation)
     * @return validation result indicating whether the field is valid
     */
    public static ValidationResult validateFormField(String fieldName, Object fieldValue, String allowedExtensions) {
        logger.debug("Validating form field '{}' with value type: {}", 
                    fieldName, fieldValue != null ? fieldValue.getClass().getSimpleName() : "null");
        
        if (fieldValue == null) {
            return ValidationResult.valid();
        }
        
        try {
            // Use the business rule validation service for field validation
            ValidationResult result = BusinessRuleValidationService.validateFormField(fieldName, fieldValue, allowedExtensions);
            
            if (!result.isValid()) {
                logger.warn("Form field '{}' validation failed: {} - {}", 
                           fieldName, result.getErrorCode(), result.getMessage());
            } else {
                logger.debug("Form field '{}' validation successful", fieldName);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during form field '{}' validation", fieldName, e);
            return ValidationResult.error(
                ValidationErrorCode.FORM_CONTENT_INVALID,
                String.format("System error during form field validation: %s", e.getMessage())
            );
        }
    }
    
    /**
     * Extracts document fields from form data.
     * 
     * <p>This method identifies and extracts document fields from form submission data,
     * supporting both single document fields and document collection fields.
     * 
     * @param formData the form submission data
     * @return list of document field names found in the form data
     */
    public static List<String> extractDocumentFields(Map<String, Object> formData) {
        List<String> documentFields = new ArrayList<>();
        
        if (formData == null || formData.isEmpty()) {
            return documentFields;
        }
        
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            if (isDocumentField(fieldName, fieldValue)) {
                documentFields.add(fieldName);
            }
        }
        
        logger.debug("Extracted {} document fields from form data: {}", documentFields.size(), documentFields);
        return documentFields;
    }
    
    /**
     * Determines if a form field contains document data.
     * 
     * <p>This method analyzes a form field to determine if it contains document data
     * based on field name patterns and value types.
     * 
     * @param fieldName the name of the form field
     * @param fieldValue the value of the form field
     * @return true if the field contains document data, false otherwise
     */
    private static boolean isDocumentField(String fieldName, Object fieldValue) {
        if (fieldValue == null) {
            return false;
        }
        
        // Check for common document field name patterns
        String lowerFieldName = fieldName.toLowerCase();
        if (lowerFieldName.contains("document") || 
            lowerFieldName.contains("file") || 
            lowerFieldName.contains("attachment") ||
            lowerFieldName.contains("upload")) {
            return true;
        }
        
        // Check if the value looks like a file reference
        if (fieldValue instanceof String) {
            String stringValue = (String) fieldValue;
            return isFileReference(stringValue);
        }
        
        // Check if the value is a list of file references
        if (fieldValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listValue = (List<Object>) fieldValue;
            if (!listValue.isEmpty() && listValue.get(0) instanceof String) {
                String firstValue = (String) listValue.get(0);
                return isFileReference(firstValue);
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a string value represents a file reference.
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
}