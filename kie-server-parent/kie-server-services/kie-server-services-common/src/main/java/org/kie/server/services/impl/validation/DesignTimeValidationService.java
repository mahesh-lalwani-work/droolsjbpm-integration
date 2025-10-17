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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.server.services.impl.config.FileExtensionConfigService;

/**
 * Service for design-time validation of file extension configurations in the form designer.
 * 
 * <p>This service provides validation logic to ensure that form field configurations
 * are valid during the design phase, including:
 * <ul>
 *   <li>Format validation for enabledFileExtensions</li>
 *   <li>Subset validation against master allowed list</li>
 *   <li>Duplicate detection</li>
 *   <li>Real-time feedback for form designers</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Validates that form-specific extensions are subsets of master allowed list</li>
 *   <li>Provides structured error messages with specific error codes</li>
 *   <li>Supports both Business Central and KIE Server configurations</li>
 *   <li>Real-time validation for form designer UI</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>
 * // Validate form field configuration
 * ValidationResult result = DesignTimeValidationService.validateFormFieldExtensions("pdf,docx", "pdf,docx,xlsx");
 * if (!result.isValid()) {
 *     // Handle validation error
 * }
 * 
 * // Validate against master list
 * ValidationResult masterResult = DesignTimeValidationService.validateAgainstMasterList("pdf,exe");
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see ValidationErrorCode
 * @see FileExtensionConfigService
 */
public class DesignTimeValidationService {
    
    /**
     * Validates the format and content of enabledFileExtensions configuration.
     * 
     * <p>This method performs comprehensive validation of the enabledFileExtensions string:
     * <ol>
     *   <li>Checks for null or empty values</li>
     *   <li>Validates format (comma-separated, no spaces, no dots)</li>
     *   <li>Detects duplicate extensions</li>
     *   <li>Validates against master allowed list (if available)</li>
     * </ol>
     * 
     * @param enabledFileExtensions the comma-separated list of extensions to validate
     * @param masterAllowedExtensions the master list of allowed extensions (can be null)
     * @return validation result with detailed error information
     */
    public static ValidationResult validateFormFieldExtensions(String enabledFileExtensions, String masterAllowedExtensions) {
        // Check for null or empty
        if (enabledFileExtensions == null || enabledFileExtensions.trim().isEmpty()) {
            return ValidationResult.valid(); // Empty is valid (uses fallback)
        }
        
        // Validate format
        ValidationResult formatResult = validateFormat(enabledFileExtensions);
        if (!formatResult.isValid()) {
            return formatResult;
        }
        
        // Check for duplicates
        ValidationResult duplicateResult = validateNoDuplicates(enabledFileExtensions);
        if (!duplicateResult.isValid()) {
            return duplicateResult;
        }
        
        // Validate against master list if provided
        if (masterAllowedExtensions != null && !masterAllowedExtensions.trim().isEmpty()) {
            ValidationResult subsetResult = validateSubset(enabledFileExtensions, masterAllowedExtensions);
            if (!subsetResult.isValid()) {
                return subsetResult;
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates that form extensions are a subset of the master allowed list.
     * 
     * <p>This method ensures that all extensions specified in the form field
     * are also present in the master allowed list from web.xml or Manage Preferences.
     * 
     * @param formExtensions comma-separated list of form-specific extensions
     * @param masterExtensions comma-separated list of master allowed extensions
     * @return validation result indicating if form extensions are valid subset
     */
    public static ValidationResult validateSubset(String formExtensions, String masterExtensions) {
        if (formExtensions == null || formExtensions.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        if (masterExtensions == null || masterExtensions.trim().isEmpty()) {
            return ValidationResult.error(
                ValidationErrorCode.EMPTY_MASTER_LIST,
                "No master allowed extensions configured. Cannot validate form extensions."
            );
        }
        
        Set<String> formExts = parseExtensions(formExtensions);
        Set<String> masterExts = parseExtensions(masterExtensions);
        
        Set<String> invalidExtensions = new HashSet<>(formExts);
        invalidExtensions.removeAll(masterExts);
        
        if (!invalidExtensions.isEmpty()) {
            return ValidationResult.error(
                ValidationErrorCode.SUBSET_VIOLATION,
                String.format(
                    "Extensions %s are not in the master allowed list. Master list: %s",
                    invalidExtensions, masterExtensions
                )
            );
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the format of the enabledFileExtensions string.
     * 
     * <p>Validates that the string follows the required format:
     * <ul>
     *   <li>Comma-separated values</li>
     *   <li>No spaces around commas</li>
     *   <li>No dots in extensions</li>
     *   <li>Lowercase extensions</li>
     * </ul>
     * 
     * @param extensions the extensions string to validate
     * @return validation result indicating format validity
     */
    public static ValidationResult validateFormat(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        // Check for spaces around commas
        if (extensions.contains(", ") || extensions.contains(" ,")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Extensions must be comma-separated without spaces. Use 'pdf,docx' not 'pdf, docx'"
            );
        }
        
        // Check for dots in extensions
        if (extensions.contains(".")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Extensions must not include dots. Use 'pdf' not '.pdf'"
            );
        }
        
        // Validate individual extensions
        String[] extArray = extensions.split(",");
        for (String ext : extArray) {
            String trimmed = ext.trim();
            if (trimmed.isEmpty()) {
                return ValidationResult.error(
                    ValidationErrorCode.INVALID_FORMAT,
                    "Empty extension found in list. Remove empty entries."
                );
            }
            
            // Check for invalid characters
            if (!trimmed.matches("^[a-zA-Z0-9]+$")) {
                return ValidationResult.error(
                    ValidationErrorCode.INVALID_FORMAT,
                    String.format("Extension '%s' contains invalid characters. Only letters and numbers allowed.", trimmed)
                );
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates that there are no duplicate extensions in the list.
     * 
     * @param extensions comma-separated list of extensions
     * @return validation result indicating if duplicates exist
     */
    public static ValidationResult validateNoDuplicates(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        Set<String> extSet = parseExtensions(extensions);
        String[] extArray = extensions.split(",");
        
        if (extSet.size() != extArray.length) {
            Set<String> duplicates = new HashSet<>();
            Set<String> seen = new HashSet<>();
            
            for (String ext : extArray) {
                String trimmed = ext.trim().toLowerCase();
                if (!seen.add(trimmed)) {
                    duplicates.add(trimmed);
                }
            }
            
            return ValidationResult.warning(
                ValidationErrorCode.DUPLICATE_EXTENSIONS,
                String.format("Duplicate extensions found: %s. Consider removing duplicates.", duplicates)
            );
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates extensions against the master allowed list from web.xml or Manage Preferences.
     * 
     * <p>This method checks if the provided extensions are allowed by the system configuration.
     * It uses the FileExtensionConfigService to get the master allowed list.
     * 
     * @param extensions comma-separated list of extensions to validate
     * @return validation result indicating if extensions are in master list
     */
    public static ValidationResult validateAgainstMasterList(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        Set<String> masterAllowed = FileExtensionConfigService.getAllowedExtensions();
        if (masterAllowed.isEmpty()) {
            return ValidationResult.warning(
                ValidationErrorCode.EMPTY_MASTER_LIST,
                "No master allowed extensions configured in web.xml. All extensions will be allowed."
            );
        }
        
        return validateSubset(extensions, String.join(",", masterAllowed));
    }
    
    /**
     * Parses a comma-separated extensions string into a set of normalized extensions.
     * 
     * @param extensions comma-separated extensions string
     * @return set of normalized (lowercase, trimmed) extensions
     */
    private static Set<String> parseExtensions(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        return Arrays.stream(extensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
    
    /**
     * Provides real-time validation feedback for form designer UI.
     * 
     * <p>This method is designed to be called as the user types in the form designer,
     * providing immediate feedback on the validity of the extensions configuration.
     * 
     * @param extensions the current extensions string being typed
     * @param masterExtensions the master allowed extensions (can be null)
     * @return validation result with immediate feedback
     */
    public static ValidationResult validateRealTime(String extensions, String masterExtensions) {
        // For real-time validation, we're more lenient - only check format
        if (extensions == null || extensions.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        // Quick format check
        if (extensions.contains(".")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Remove dots from extensions (use 'pdf' not '.pdf')"
            );
        }
        
        if (extensions.contains(", ") || extensions.contains(" ,")) {
            return ValidationResult.warning(
                ValidationErrorCode.INVALID_FORMAT,
                "Remove spaces around commas (use 'pdf,docx' not 'pdf, docx')"
            );
        }
        
        return ValidationResult.valid();
    }
}
