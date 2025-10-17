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

package org.kie.server.services.impl.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced service for managing file extension configuration with dynamic whitelist support.
 * 
 * <p>This service provides comprehensive functionality to load, validate, and dynamically
 * update allowed file extensions for document uploads in jBPM. It supports runtime
 * configuration updates through multiple sources including web.xml context parameters,
 * system properties, and programmatic updates.
 * 
 * <p>Key features:
 * <ul>
 *   <li><strong>Dynamic whitelist:</strong> Runtime configuration updates without server restart</li>
 *   <li><strong>Thread-safe operations:</strong> Concurrent access protection with read-write locks</li>
 *   <li><strong>Multiple configuration sources:</strong> web.xml, system properties, programmatic</li>
 *   <li><strong>Fallback support:</strong> Graceful degradation to default extensions</li>
 *   <li><strong>Validation support:</strong> Extension format and content validation</li>
 * </ul>
 * 
 * <p>Configuration hierarchy (in order of precedence):
 * <ol>
 *   <li><strong>Programmatic updates:</strong> Direct method calls for runtime updates</li>
 *   <li><strong>System properties:</strong> JVM system property overrides</li>
 *   <li><strong>web.xml context parameters:</strong> Servlet context configuration</li>
 *   <li><strong>Default extensions:</strong> Hardcoded fallback values</li>
 * </ol>
 * 
 * <p>Usage examples:
 * <pre>
 * // Initialize from servlet context
 * FileExtensionConfigService.initialize(servletContext);
 * 
 * // Check if extension is allowed
 * boolean allowed = FileExtensionConfigService.isExtensionAllowed("pdf");
 * 
 * // Get current allowed extensions
 * Set&lt;String&gt; extensions = FileExtensionConfigService.getAllowedExtensions();
 * 
 * // Update configuration dynamically
 * FileExtensionConfigService.updateAllowedExtensions("pdf,docx,xlsx");
 * </pre>
 * 
 * @author jBPM Team
 * @since 7.74.1
 */
public class FileExtensionConfigService {

    private static final Logger logger = LoggerFactory.getLogger(FileExtensionConfigService.class);
    
    private static final String ALLOWED_EXTENSIONS_PARAM = "org.kie.server.allowed.file.extensions";
    private static final String DEFAULT_EXTENSIONS = "pdf,docx,xlsx,txt,jpg,png";
    
    // Thread-safe configuration management
    private static final ReadWriteLock configLock = new ReentrantReadWriteLock();
    private static Set<String> allowedExtensions;
    private static String currentConfigSource = "uninitialized";

    /**
     * Initialize the service with the servlet context.
     * 
     * <p>This method should be called during application startup to load configuration
     * from web.xml context parameters. It provides the highest priority configuration
     * source after programmatic updates.
     * 
     * @param servletContext the servlet context containing configuration parameters
     */
    public static void initialize(ServletContext servletContext) {
        configLock.writeLock().lock();
        try {
            String extensionsParam = servletContext.getInitParameter(ALLOWED_EXTENSIONS_PARAM);
            if (extensionsParam == null || extensionsParam.trim().isEmpty()) {
                extensionsParam = DEFAULT_EXTENSIONS;
                currentConfigSource = "default";
            } else {
                currentConfigSource = "web.xml";
            }

            allowedExtensions = parseExtensions(extensionsParam);
            logger.info("FileExtensionConfigService initialized from {} with extensions: {}", 
                       currentConfigSource, allowedExtensions);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Initialize the service with system properties as fallback.
     * 
     * <p>This method can be called when ServletContext is not available or as a
     * fallback mechanism. It loads configuration from JVM system properties.
     * 
     * <p>This method is thread-safe and can be called multiple times to refresh
     * configuration from system properties.
     */
    public static void initialize() {
        configLock.writeLock().lock();
        try {
            String extensionsParam = System.getProperty(ALLOWED_EXTENSIONS_PARAM);
            if (extensionsParam == null || extensionsParam.trim().isEmpty()) {
                extensionsParam = DEFAULT_EXTENSIONS;
                currentConfigSource = "default";
            } else {
                currentConfigSource = "system.property";
            }

            allowedExtensions = parseExtensions(extensionsParam);
            logger.info("FileExtensionConfigService initialized from {} with extensions: {}", 
                       currentConfigSource, allowedExtensions);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Get the set of allowed file extensions.
     * 
     * <p>This method provides thread-safe access to the current allowed extensions.
     * It returns a defensive copy to prevent external modification of the internal
     * configuration state.
     * 
     * @return set of allowed file extensions (lowercase)
     */
    public static Set<String> getAllowedExtensions() {
        configLock.readLock().lock();
        try {
            return allowedExtensions != null ? new HashSet<>(allowedExtensions) : Collections.emptySet();
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Check if a file extension is allowed.
     * 
     * <p>This method performs a thread-safe check against the current allowed extensions.
     * The check is case-insensitive and handles null input gracefully.
     * 
     * @param extension the file extension to check (case insensitive)
     * @return true if the extension is allowed, false otherwise
     */
    public static boolean isExtensionAllowed(String extension) {
        if (extension == null) {
            return false;
        }
        
        configLock.readLock().lock();
        try {
            return allowedExtensions != null && allowedExtensions.contains(extension.toLowerCase().trim());
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Get the list of allowed file extensions as a list.
     * 
     * <p>This method provides thread-safe access to the current allowed extensions
     * as a list. It returns a defensive copy to prevent external modification.
     * 
     * @return list of allowed file extensions
     */
    public static List<String> getAllowedExtensionsList() {
        configLock.readLock().lock();
        try {
            return allowedExtensions != null ? 
                Arrays.asList(allowedExtensions.toArray(new String[0])) : 
                Collections.emptyList();
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Update the allowed extensions dynamically at runtime.
     * 
     * <p>This method allows runtime updates to the allowed file extensions without
     * requiring a server restart. It validates the input format and updates the
     * configuration thread-safely.
     * 
     * <p>The extensions parameter should be a comma-separated list of file extensions
     * without dots or spaces. Invalid extensions will be filtered out with a warning.
     * 
     * @param extensions comma-separated list of allowed file extensions
     * @return true if the update was successful, false if validation failed
     */
    public static boolean updateAllowedExtensions(String extensions) {
        if (extensions == null || extensions.trim().isEmpty()) {
            logger.warn("Attempted to update allowed extensions with null or empty value");
            return false;
        }
        
        configLock.writeLock().lock();
        try {
            Set<String> newExtensions = parseExtensions(extensions);
            if (newExtensions.isEmpty()) {
                logger.warn("No valid extensions found in input: {}", extensions);
                return false;
            }
            
            allowedExtensions = newExtensions;
            currentConfigSource = "programmatic";
            logger.info("Allowed extensions updated programmatically to: {}", allowedExtensions);
            return true;
            
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * Get the current configuration source.
     * 
     * <p>This method provides information about where the current configuration
     * was loaded from, which is useful for debugging and administrative purposes.
     * 
     * @return string indicating the current configuration source
     */
    public static String getCurrentConfigSource() {
        configLock.readLock().lock();
        try {
            return currentConfigSource;
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * Parse and validate a comma-separated list of file extensions.
     * 
     * <p>This method parses the input string, validates each extension, and returns
     * a set of valid extensions. Invalid extensions are logged as warnings and
     * excluded from the result.
     * 
     * @param extensionsParam comma-separated list of file extensions
     * @return set of valid, normalized file extensions
     */
    private static Set<String> parseExtensions(String extensionsParam) {
        Set<String> extensions = new HashSet<>();
        String[] extensionArray = extensionsParam.split(",");
        
        for (String ext : extensionArray) {
            String normalizedExt = ext.trim().toLowerCase();
            
            // Validate extension format
            if (isValidExtension(normalizedExt)) {
                extensions.add(normalizedExt);
            } else {
                logger.warn("Invalid file extension format ignored: '{}'", ext);
            }
        }
        
        return extensions;
    }
    
    /**
     * Validate if a file extension has a valid format.
     * 
     * <p>This method checks if an extension meets the basic format requirements:
     * <ul>
     *   <li>Not null or empty</li>
     *   <li>Contains only alphanumeric characters</li>
     *   <li>Reasonable length (1-10 characters)</li>
     *   <li>No dots or special characters</li>
     * </ul>
     * 
     * @param extension the extension to validate
     * @return true if the extension format is valid, false otherwise
     */
    private static boolean isValidExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        // Check length
        if (extension.length() > 10) {
            return false;
        }
        
        // Check for valid characters (alphanumeric only)
        return extension.matches("^[a-zA-Z0-9]+$");
    }
}
