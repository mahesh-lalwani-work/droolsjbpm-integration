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

/**
 * Represents the result of validating multiple files in a DocumentCollection field.
 * 
 * <p>This class encapsulates the validation results for multiple files, providing both
 * individual file results and overall validation status. It's designed for use with
 * DocumentCollection form fields that allow multiple file uploads.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Overall validation status (valid only if all files are valid)</li>
 *   <li>Per-file validation results with detailed error information</li>
 *   <li>Statistical information (total files, passed, failed)</li>
 *   <li>Convenience methods for error reporting</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * List&lt;String&gt; files = Arrays.asList("doc1.pdf", "virus.exe", "doc2.docx");
 * MultiFileValidationResult result = FileExtensionValidator.validateFiles(files, "pdf,docx");
 * 
 * if (!result.isOverallValid()) {
 *     logger.error("Validation failed for {} files: {}", 
 *                  result.getFilesFailed(), 
 *                  result.getInvalidFileNames());
 * }
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 * @see ValidationResult
 * @see FileExtensionValidator
 */
public class MultiFileValidationResult {
    
    private final boolean overallValid;
    private final int filesValidated;
    private final int filesPassed;
    private final int filesFailed;
    private final List<SingleFileResult> results;
    
    private MultiFileValidationResult(Builder builder) {
        this.overallValid = builder.overallValid;
        this.filesValidated = builder.filesValidated;
        this.filesPassed = builder.filesPassed;
        this.filesFailed = builder.filesFailed;
        this.results = builder.results;
    }
    
    public boolean isOverallValid() {
        return overallValid;
    }
    
    public int getFilesValidated() {
        return filesValidated;
    }
    
    public int getFilesPassed() {
        return filesPassed;
    }
    
    public int getFilesFailed() {
        return filesFailed;
    }
    
    public int getInvalidFileCount() {
        return filesFailed;
    }
    
    public int getTotalFileCount() {
        return filesValidated;
    }
    
    public List<SingleFileResult> getResults() {
        return results;
    }
    
    public List<String> getInvalidFileNames() {
        List<String> invalidNames = new ArrayList<>();
        for (SingleFileResult result : results) {
            if (!result.isValid()) {
                invalidNames.add(result.getFileName());
            }
        }
        return invalidNames;
    }
    
    /**
     * Represents the validation result for a single file within multi-file validation.
     * 
     * <p>This class encapsulates the validation result for an individual file,
     * including the file name, validation status, and detailed validation result.
     * 
     * @author jBPM Team
     * @since 7.74.1
     */
    public static class SingleFileResult {
        private final String fileName;
        private final boolean valid;
        private final ValidationResult validationResult;
        
        public SingleFileResult(String fileName, boolean valid, ValidationResult validationResult) {
            this.fileName = fileName;
            this.valid = valid;
            this.validationResult = validationResult;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public ValidationResult getValidationResult() {
            return validationResult;
        }
    }
    
    /**
     * Builder class for constructing MultiFileValidationResult instances.
     * 
     * <p>This builder allows for incremental construction of multi-file validation results
     * by adding individual file validation results one at a time.
     * 
     * <p>Usage example:
     * <pre>
     * MultiFileValidationResult.Builder builder = new MultiFileValidationResult.Builder();
     * builder.addResult("file1.pdf", ValidationResult.valid());
     * builder.addResult("file2.exe", ValidationResult.error(...));
     * MultiFileValidationResult result = builder.build();
     * </pre>
     * 
     * @author jBPM Team
     * @since 7.74.1
     */
    public static class Builder {
        private boolean overallValid = true;
        private int filesValidated = 0;
        private int filesPassed = 0;
        private int filesFailed = 0;
        private List<SingleFileResult> results = new ArrayList<>();
        
        public Builder addResult(String fileName, ValidationResult validationResult) {
            filesValidated++;
            
            if (validationResult.isValid()) {
                filesPassed++;
                results.add(new SingleFileResult(fileName, true, validationResult));
            } else {
                filesFailed++;
                overallValid = false;
                results.add(new SingleFileResult(fileName, false, validationResult));
            }
            
            return this;
        }
        
        /**
         * Alias method for addResult to maintain backward compatibility.
         * 
         * @param fileName the name of the file being validated
         * @param validationResult the validation result for this file
         * @return this builder instance for method chaining
         */
        public Builder addFileResult(String fileName, ValidationResult validationResult) {
            return addResult(fileName, validationResult);
        }
        
        public MultiFileValidationResult build() {
            return new MultiFileValidationResult(this);
        }
    }
}
