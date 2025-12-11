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

/**
 * Enumeration of error codes for file extension validation operations.
 * 
 * <p>This enum provides standardized error codes for different types of validation failures
 * in the jBPM Documents Allowed Files feature. Each error code includes:
 * <ul>
 *   <li>A unique code string (e.g., "EXT_VAL_001")</li>
 *   <li>A human-readable description</li>
 * </ul>
 * 
 * <p>Error codes are organized by category:
 * <ul>
 *   <li><strong>001-099:</strong> Design-time validation errors (form designer)</li>
 *   <li><strong>101-199:</strong> Runtime validation errors (file uploads)</li>
 *   <li><strong>201-299:</strong> Process execution validation errors</li>
 *   <li><strong>301-399:</strong> API validation errors</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * ValidationResult result = ValidationResult.error(
 *     ValidationErrorCode.INVALID_EXTENSION,
 *     "File 'virus.exe' is not allowed"
 * );
 * 
 * if (result.getErrorCode() == ValidationErrorCode.INVALID_EXTENSION) {
 *     // Handle invalid extension error
 * }
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 */
public enum ValidationErrorCode {
    
    // Design-Time Errors (001-099)
    INVALID_FORMAT("EXT_VAL_001", "Invalid format for enabledFileExtensions"),
    SUBSET_VIOLATION("EXT_VAL_002", "Extensions not in master allowed list"),
    EMPTY_MASTER_LIST("EXT_VAL_003", "No master allowed extensions configured"),
    DUPLICATE_EXTENSIONS("EXT_VAL_004", "Duplicate extensions found"),
    
    // Runtime Errors (101-199)
    INVALID_EXTENSION("EXT_VAL_101", "File has invalid extension"),
    MULTIPLE_INVALID_FILES("EXT_VAL_102", "Multiple files have invalid extensions"),
    NO_CONFIGURATION("EXT_VAL_103", "No configuration found for validation"),
    MAX_FILES_EXCEEDED("EXT_VAL_104", "Maximum number of files exceeded"),
    FILE_NO_EXTENSION("EXT_VAL_105", "File has no extension"),
    CASE_MISMATCH_WARNING("EXT_VAL_106", "Case mismatch in extension"),
    
    // Process Execution Errors (201-299)
    PROCESS_START_VALIDATION_FAILED("EXT_VAL_201", "Process start validation failed"),
    TASK_COMPLETION_VALIDATION_FAILED("EXT_VAL_202", "Task completion validation failed"),
    PROCESS_START_VALIDATION_ERROR("EXT_VAL_203", "Process start validation error"),
    TASK_COMPLETION_VALIDATION_ERROR("EXT_VAL_204", "Task completion validation error"),
    
    // API Errors (301-399)
    FORM_CONTENT_INVALID("EXT_VAL_301", "Form content validation failed"),
    VALIDATION_SYSTEM_ERROR("EXT_VAL_302", "System error during validation"),
    CONFIGURATION_REFRESH_ERROR("EXT_VAL_303", "Configuration refresh error");
    
    private final String code;
    private final String description;
    
    /**
     * Constructs a ValidationErrorCode with the specified code and description.
     * 
     * @param code the unique error code string
     * @param description a human-readable description of the error
     */
    ValidationErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Returns the unique error code string.
     * 
     * @return the error code (e.g., "EXT_VAL_001")
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns the human-readable description of this error code.
     * 
     * @return the error description
     */
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return code + ": " + description;
    }
}
