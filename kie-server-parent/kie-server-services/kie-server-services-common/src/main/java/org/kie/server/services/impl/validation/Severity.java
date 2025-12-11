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
 * Enumeration of severity levels for validation messages and results.
 * 
 * <p>This enum defines the severity levels used throughout the file extension validation framework
 * to categorize the importance and impact of validation messages:
 * 
 * <ul>
 *   <li><strong>NONE:</strong> No issues or messages</li>
 *   <li><strong>INFO:</strong> Informational messages that don't affect validation</li>
 *   <li><strong>WARNING:</strong> Validation passes but user should be aware of potential issues</li>
 *   <li><strong>ERROR:</strong> Validation failed and must be addressed</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * ValidationResult result = ValidationResult.warning(
 *     ValidationErrorCode.DUPLICATE_EXTENSIONS,
 *     "Duplicate extensions found"
 * );
 * 
 * if (result.getSeverity() == Severity.WARNING) {
 *     // Show warning to user but allow operation to continue
 * }
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 */
public enum Severity {
    /** No issue or message associated with the validation result */
    NONE,
    
    /** Informational message that doesn't affect validation outcome */
    INFO,
    
    /** Warning - validation passes but user should be aware of potential issues */
    WARNING,
    
    /** Error - validation failed and must be addressed before proceeding */
    ERROR
}
