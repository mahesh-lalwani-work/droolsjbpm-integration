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

import java.util.Objects;

/**
 * Represents the result of a validation operation in the file extension validation framework.
 * 
 * <p>This class encapsulates the outcome of validation operations, including:
 * <ul>
 *   <li>Validation status (valid/invalid)</li>
 *   <li>Error code for programmatic handling</li>
 *   <li>Human-readable error message</li>
 *   <li>Severity level (NONE, INFO, WARNING, ERROR)</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>
 * // Create a successful validation result
 * ValidationResult valid = ValidationResult.valid();
 * 
 * // Create an error result
 * ValidationResult error = ValidationResult.error(
 *     ValidationErrorCode.INVALID_EXTENSION, 
 *     "File 'virus.exe' has invalid extension"
 * );
 * 
 * // Create a warning result
 * ValidationResult warning = ValidationResult.warning(
 *     ValidationErrorCode.DUPLICATE_EXTENSIONS,
 *     "Duplicate extensions found: pdf, pdf"
 * );
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationErrorCode
 * @see Severity
 */
public class ValidationResult {
    
    private final boolean valid;
    private final ValidationErrorCode errorCode;
    private final String message;
    private final Severity severity;
    
    private ValidationResult(boolean valid, ValidationErrorCode errorCode, String message, Severity severity) {
        this.valid = valid;
        this.errorCode = errorCode;
        this.message = message;
        this.severity = severity;
    }
    
    /**
     * Creates a successful validation result with no errors or warnings.
     * 
     * @return a valid ValidationResult with no error code or message
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null, null, Severity.NONE);
    }
    
    /**
     * Creates a validation error result indicating validation failure.
     * 
     * @param code the error code identifying the type of validation failure
     * @param message a human-readable error message describing the failure
     * @return a ValidationResult indicating validation failure
     * @throws NullPointerException if code or message is null
     */
    public static ValidationResult error(ValidationErrorCode code, String message) {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
        return new ValidationResult(false, code, message, Severity.ERROR);
    }
    
    /**
     * Creates a validation warning result indicating validation passed but with a warning message.
     * 
     * <p>Warning results are considered valid but indicate potential issues that users should be aware of.
     * 
     * @param code the warning code identifying the type of warning
     * @param message a human-readable warning message
     * @return a ValidationResult indicating validation passed with warning
     * @throws NullPointerException if code or message is null
     */
    public static ValidationResult warning(ValidationErrorCode code, String message) {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
        return new ValidationResult(true, code, message, Severity.WARNING);
    }
    
    /**
     * Returns whether the validation passed successfully.
     * 
     * @return true if validation passed, false if validation failed
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Returns the error code associated with this validation result.
     * 
     * @return the ValidationErrorCode, or null if validation passed without errors
     */
    public ValidationErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * Returns the human-readable message associated with this validation result.
     * 
     * @return the message, or null if no message is associated
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Returns the severity level of this validation result.
     * 
     * @return the Severity level (NONE, INFO, WARNING, or ERROR)
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * Returns whether this validation result has an associated message.
     * 
     * @return true if a non-empty message is present, false otherwise
     */
    public boolean hasMessage() {
        return message != null && !message.isEmpty();
    }
    
    @Override
    public String toString() {
        if (valid && !hasMessage()) {
            return "ValidationResult{valid}";
        }
        return String.format("ValidationResult{valid=%s, code=%s, severity=%s, message='%s'}", 
                             valid, errorCode, severity, message);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid &&
               errorCode == that.errorCode &&
               Objects.equals(message, that.message) &&
               severity == that.severity;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errorCode, message, severity);
    }
}
