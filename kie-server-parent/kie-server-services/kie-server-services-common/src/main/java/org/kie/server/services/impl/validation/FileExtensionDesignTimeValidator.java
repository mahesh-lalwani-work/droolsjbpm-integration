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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.server.services.impl.config.FileExtensionConfigService;

/**
 * Design-time validator for file extension configuration in form fields.
 * 
 * <p>This validator ensures that the file extensions specified in form field definitions
 * (enabledFileExtensions property) are valid and comply with the master allowed list
 * configured in web.xml or system properties.
 * 
 * <p>The validator performs the following checks:
 * <ul>
 *   <li><strong>Format validation:</strong> Ensures proper comma-separated format without spaces or dots</li>
 *   <li><strong>Subset validation:</strong> Verifies that field extensions are a subset of the master list</li>
 *   <li><strong>Duplicate detection:</strong> Identifies and warns about duplicate extensions</li>
 *   <li><strong>Case handling:</strong> Performs case-insensitive validation</li>
 * </ul>
 * 
 * <p>Usage examples:
 * <pre>
 * // Validate field extensions against master list
 * Set&lt;String&gt; masterList = Set.of("pdf", "docx", "xlsx");
 * ValidationResult result = FileExtensionDesignTimeValidator
 *     .validateFieldExtensions("pdf,docx", masterList);
 * 
 * // Validate extension format
 * ValidationResult formatResult = FileExtensionDesignTimeValidator
 *     .validateExtensionFormat("pdf,docx,xlsx");
 * 
 * // Validate against current configuration
 * ValidationResult configResult = FileExtensionDesignTimeValidator
 *     .validateAgainstCurrentMasterList("pdf,docx");
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see ValidationErrorCode
 */
public class FileExtensionDesignTimeValidator {
    
    /**
     * Validates that field extensions are a valid subset of the master allowed list.
     * 
     * <p>This method performs comprehensive validation of form field extensions:
     * <ul>
     *   <li>Checks if field extensions are null or empty (valid - means use master list)</li>
     *   <li>Validates the format of the extension string</li>
     *   <li>Detects duplicate extensions and issues warnings</li>
     *   <li>Ensures all field extensions are present in the master list</li>
     * </ul>
     * 
     * @param fieldExtensions comma-separated extensions from form field (e.g., "pdf,docx")
     * @param masterList set of allowed extensions from web.xml or system properties
     * @return validation result with detailed error information
     */
    public static ValidationResult validateFieldExtensions(String fieldExtensions, Set<String> masterList) {
        // Handle null/empty cases
        if (fieldExtensions == null || fieldExtensions.trim().isEmpty()) {
            return ValidationResult.valid(); // Empty means "use master list"
        }
        
        if (masterList == null || masterList.isEmpty()) {
            return ValidationResult.error(
                ValidationErrorCode.EMPTY_MASTER_LIST,
                "No master allowed extensions list configured. " +
                "Please configure via web.xml or system properties."
            );
        }
        
        // Validate format first
        ValidationResult formatResult = validateExtensionFormat(fieldExtensions);
        if (!formatResult.isValid()) {
            return formatResult;
        }
        
        // Parse extensions
        Set<String> fieldExts = parseExtensions(fieldExtensions);
        
        // Check for duplicates
        List<String> duplicates = findDuplicates(fieldExtensions);
        if (!duplicates.isEmpty()) {
            return ValidationResult.warning(
                ValidationErrorCode.DUPLICATE_EXTENSIONS,
                "Duplicate extensions found: " + String.join(", ", duplicates) +
                ". They will be automatically removed."
            );
        }
        
        // Check subset: fieldExts ⊆ masterList
        Set<String> invalidExts = new HashSet<>(fieldExts);
        invalidExts.removeAll(masterList);
        
        if (!invalidExts.isEmpty()) {
            return ValidationResult.error(
                ValidationErrorCode.SUBSET_VIOLATION,
                String.format(
                    "Extensions '%s' are not in the master allowed list. " +
                    "Allowed extensions: %s",
                    String.join(", ", invalidExts),
                    String.join(", ", masterList)
                )
            );
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the format of the extension string.
     * 
     * <p>This method ensures the extension string follows the correct format:
     * <ul>
     *   <li><strong>Valid format:</strong> "pdf,docx,xlsx" (comma-separated, no spaces, no dots)</li>
     *   <li><strong>Invalid formats:</strong> "pdf, docx", ".pdf,.docx", "pdf docx"</li>
     * </ul>
     * 
     * <p>Validation rules:
     * <ul>
     *   <li>No spaces allowed in the string</li>
     *   <li>No dots allowed (extensions should not include the dot prefix)</li>
     *   <li>Only alphanumeric characters, commas, hyphens, and underscores allowed</li>
     *   <li>No consecutive commas or leading/trailing commas</li>
     * </ul>
     * 
     * @param extensions the extension string to validate
     * @return validation result indicating format validity
     */
    public static ValidationResult validateExtensionFormat(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            return ValidationResult.valid();
        }
        
        // Check for spaces
        if (extensions.contains(" ")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Extensions contain spaces. Use format: 'pdf,docx,xlsx' (no spaces)"
            );
        }
        
        // Check for dots
        if (extensions.contains(".")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Extensions contain dots. Use format: 'pdf,docx,xlsx' (no dots)"
            );
        }
        
        // Check for valid characters (alphanumeric, comma, hyphen, underscore)
        if (!extensions.matches("^[a-zA-Z0-9,\\-_]+$")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Extensions contain invalid characters. Only letters, numbers, and commas allowed."
            );
        }
        
        // Check for consecutive commas or leading/trailing commas
        if (extensions.matches(".*,,.*") || extensions.startsWith(",") || extensions.endsWith(",")) {
            return ValidationResult.error(
                ValidationErrorCode.INVALID_FORMAT,
                "Invalid comma placement. Use format: 'pdf,docx,xlsx'"
            );
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates field extensions against the current master list from FileExtensionConfigService.
     * 
     * <p>This is a convenience method that automatically retrieves the current master list
     * from the FileExtensionConfigService and validates the field extensions against it.
     * 
     * @param fieldExtensions comma-separated extensions from form field
     * @return validation result indicating whether field extensions are valid
     */
    public static ValidationResult validateAgainstCurrentMasterList(String fieldExtensions) {
        Set<String> masterList = FileExtensionConfigService.getAllowedExtensions();
        return validateFieldExtensions(fieldExtensions, masterList);
    }
    
    /**
     * Parses comma-separated extensions into a set.
     * 
     * <p>This method converts the comma-separated string into a set of normalized extensions:
     * <ul>
     *   <li>Converts to lowercase for case-insensitive comparison</li>
     *   <li>Trims whitespace from each extension</li>
     *   <li>Filters out empty strings</li>
     * </ul>
     * 
     * @param extensions comma-separated extension string
     * @return set of normalized extensions
     */
    private static Set<String> parseExtensions(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        return Arrays.stream(extensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
    
    /**
     * Finds duplicate extensions in a comma-separated string.
     * 
     * <p>This method identifies duplicate extensions by normalizing each extension
     * (lowercase, trimmed) and tracking which ones have been seen before.
     * 
     * @param extensions comma-separated extension string
     * @return list of duplicate extensions found
     */
    private static List<String> findDuplicates(String extensions) {
        String[] parts = extensions.split(",");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new java.util.ArrayList<>();
        
        for (String ext : parts) {
            String normalized = ext.trim().toLowerCase();
            if (!seen.add(normalized)) {
                duplicates.add(normalized);
            }
        }
        
        return duplicates;
    }
}
