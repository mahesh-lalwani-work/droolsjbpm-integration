/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.impl.config;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletContext;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FileExtensionConfigServiceTest {

    private static final String PARAM_NAME = "org.kie.server.allowed.file.extensions";

    @Mock
    private ServletContext servletContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInitialize_WithWebXmlParameter() {
        // Mock web.xml parameter
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("pdf,docx,xlsx");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertNotNull("Extensions should not be null", extensions);
        assertEquals("Should have 3 extensions", 3, extensions.size());
        assertTrue("Should contain pdf", extensions.contains("pdf"));
        assertTrue("Should contain docx", extensions.contains("docx"));
        assertTrue("Should contain xlsx", extensions.contains("xlsx"));
    }

    @Test
    public void testInitialize_WithoutWebXmlParameter() {
        // No web.xml parameter configured - should use DEFAULT_EXTENSIONS
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn(null);
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertNotNull("Extensions should not be null", extensions);
        assertFalse("Extensions should use defaults when not configured", 
                   extensions.isEmpty());
        // Should contain DEFAULT_EXTENSIONS = "pdf,docx,xlsx,txt,jpg,png"
        assertTrue("Should contain pdf from defaults", extensions.contains("pdf"));
    }

    @Test
    public void testGetAllowedExtensions_WithMultipleExtensions() {
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("pdf,docx,xlsx");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertNotNull("Extensions should not be null", extensions);
        assertEquals("Should have exactly 3 extensions", 3, extensions.size());
        assertTrue("Should contain pdf", extensions.contains("pdf"));
        assertTrue("Should contain docx", extensions.contains("docx"));
        assertTrue("Should contain xlsx", extensions.contains("xlsx"));
    }

    @Test
    public void testGetAllowedExtensions_EmptyConfiguration() {
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertNotNull("Extensions should not be null", extensions);
        // Empty string falls back to DEFAULT_EXTENSIONS
        assertFalse("Should use defaults when empty", extensions.isEmpty());
    }

    @Test
    public void testExtensionsAreLowercase() {
        // Test that extensions are converted to lowercase
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("PDF,DOCX,XLSX");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertTrue("PDF should be converted to lowercase", extensions.contains("pdf"));
        assertTrue("DOCX should be converted to lowercase", extensions.contains("docx"));
        assertTrue("XLSX should be converted to lowercase", extensions.contains("xlsx"));
        assertFalse("Should not contain uppercase PDF", extensions.contains("PDF"));
    }

    @Test
    public void testExtensionsAreTrimmed() {
        // Test that extensions are trimmed
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn(" pdf , docx , xlsx ");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertTrue("Should contain trimmed pdf", extensions.contains("pdf"));
        assertTrue("Should contain trimmed docx", extensions.contains("docx"));
        assertTrue("Should contain trimmed xlsx", extensions.contains("xlsx"));
        assertEquals("Should have 3 extensions after trimming", 3, extensions.size());
    }

    @Test
    public void testSingleExtension() {
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("pdf");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertEquals("Should have 1 extension", 1, extensions.size());
        assertTrue("Should contain pdf", extensions.contains("pdf"));
    }

    @Test
    public void testManyExtensions() {
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("pdf,doc,docx,xls,xlsx,ppt,pptx,txt,jpg,png,gif");
        
        FileExtensionConfigService.initialize(servletContext);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        
        assertEquals("Should have 11 extensions", 11, extensions.size());
        assertTrue("Should contain pdf", extensions.contains("pdf"));
        assertTrue("Should contain gif", extensions.contains("gif"));
    }

    @Test
    public void testInitializeCalledTwice() {
        // Test reinitializing the service
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("pdf,docx");
        
        FileExtensionConfigService.initialize(servletContext);
        Set<String> firstCall = FileExtensionConfigService.getAllowedExtensions();
        assertEquals(2, firstCall.size());
        
        // Reinitialize with different values
        when(servletContext.getInitParameter(PARAM_NAME))
            .thenReturn("pdf,docx,xlsx");
        
        FileExtensionConfigService.initialize(servletContext);
        Set<String> secondCall = FileExtensionConfigService.getAllowedExtensions();
        assertEquals(3, secondCall.size());
    }
}

