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

import javax.servlet.ServletContext;

/**
 * Service for managing file extension configuration from web.xml context
 * parameters.
 * This service provides functionality to load and validate allowed file
 * extensions
 * for document uploads in JBPM.
 */
public class FileExtensionConfigService {

    private static final String ALLOWED_EXTENSIONS_PARAM = "org.kie.server.allowed.file.extensions";
    private static final String DEFAULT_EXTENSIONS = "pdf,docx,xlsx,txt,jpg,png";

    private static Set<String> allowedExtensions;

    /**
     * Initialize the service with the servlet context.
     * This method should be called during application startup.
     * 
     * @param servletContext the servlet context containing configuration parameters
     */
    public static void initialize(ServletContext servletContext) {
        String extensionsParam = servletContext.getInitParameter(ALLOWED_EXTENSIONS_PARAM);
        if (extensionsParam == null || extensionsParam.trim().isEmpty()) {
            extensionsParam = DEFAULT_EXTENSIONS;
        }

        allowedExtensions = new HashSet<>();
        String[] extensions = extensionsParam.split(",");
        for (String ext : extensions) {
            allowedExtensions.add(ext.trim().toLowerCase());
        }
    }

    /**
     * Initialize the service with system properties as fallback.
     * This method can be called when ServletContext is not available.
     */
    public static void initialize() {
        String extensionsParam = System.getProperty(ALLOWED_EXTENSIONS_PARAM);
        if (extensionsParam == null || extensionsParam.trim().isEmpty()) {
            extensionsParam = DEFAULT_EXTENSIONS;
        }

        allowedExtensions = new HashSet<>();
        String[] extensions = extensionsParam.split(",");
        for (String ext : extensions) {
            allowedExtensions.add(ext.trim().toLowerCase());
        }
    }

    /**
     * Get the set of allowed file extensions.
     * 
     * @return set of allowed file extensions (lowercase)
     */
    public static Set<String> getAllowedExtensions() {
        return allowedExtensions != null ? allowedExtensions : Collections.emptySet();
    }

    /**
     * Check if a file extension is allowed.
     * 
     * @param extension the file extension to check (case insensitive)
     * @return true if the extension is allowed, false otherwise
     */
    public static boolean isExtensionAllowed(String extension) {
        if (extension == null)
            return false;
        return getAllowedExtensions().contains(extension.toLowerCase().trim());
    }

    /**
     * Get the list of allowed file extensions as a list.
     * 
     * @return list of allowed file extensions
     */
    public static List<String> getAllowedExtensionsList() {
        return Arrays.asList(getAllowedExtensions().toArray(new String[0]));
    }
}
