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

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for enhanced FileExtensionConfigService.
 * 
 * <p>This test class provides comprehensive coverage for the enhanced
 * FileExtensionConfigService functionality, including dynamic whitelist
 * support, thread-safe operations, and configuration management.
 * 
 * @author jBPM Team
 * @since 7.74.1
 */
public class FileExtensionConfigServiceTest {
    
    private String originalSystemProperty;
    
    @Before
    public void setUp() {
        // Save original system property
        originalSystemProperty = System.getProperty("org.kie.server.allowed.file.extensions");
        
        // Clear system property for clean test
        System.clearProperty("org.kie.server.allowed.file.extensions");
        
        // Initialize with default values
        FileExtensionConfigService.initialize();
    }
    
    @After
    public void tearDown() {
        // Restore original system property
        if (originalSystemProperty != null) {
            System.setProperty("org.kie.server.allowed.file.extensions", originalSystemProperty);
        } else {
            System.clearProperty("org.kie.server.allowed.file.extensions");
        }
    }
    
    @Test
    public void testInitialize_WithDefaultExtensions() {
        // Test initialization with default extensions
        FileExtensionConfigService.initialize();
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertNotNull("Extensions should not be null", extensions);
        assertFalse("Extensions should not be empty", extensions.isEmpty());
        assertTrue("Should contain PDF", extensions.contains("pdf"));
        assertTrue("Should contain DOCX", extensions.contains("docx"));
        assertTrue("Should contain XLSX", extensions.contains("xlsx"));
        assertTrue("Should contain TXT", extensions.contains("txt"));
        assertTrue("Should contain JPG", extensions.contains("jpg"));
        assertTrue("Should contain PNG", extensions.contains("png"));
    }
    
    @Test
    public void testInitialize_WithSystemProperty() {
        // Test initialization with system property
        System.setProperty("org.kie.server.allowed.file.extensions", "pdf,docx,xlsx");
        FileExtensionConfigService.initialize();
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertEquals("Should have 3 extensions", 3, extensions.size());
        assertTrue("Should contain PDF", extensions.contains("pdf"));
        assertTrue("Should contain DOCX", extensions.contains("docx"));
        assertTrue("Should contain XLSX", extensions.contains("xlsx"));
        assertFalse("Should not contain TXT", extensions.contains("txt"));
    }
    
    @Test
    public void testInitialize_WithEmptySystemProperty() {
        // Test initialization with empty system property
        System.setProperty("org.kie.server.allowed.file.extensions", "");
        FileExtensionConfigService.initialize();
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertNotNull("Extensions should not be null", extensions);
        assertFalse("Should fallback to default extensions", extensions.isEmpty());
        assertTrue("Should contain default PDF", extensions.contains("pdf"));
    }
    
    @Test
    public void testInitialize_WithInvalidSystemProperty() {
        // Test initialization with invalid system property
        System.setProperty("org.kie.server.allowed.file.extensions", "pdf,.docx,invalid.ext,xlsx");
        FileExtensionConfigService.initialize();
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertTrue("Should contain valid PDF", extensions.contains("pdf"));
        assertTrue("Should contain valid XLSX", extensions.contains("xlsx"));
        assertFalse("Should not contain invalid .docx", extensions.contains(".docx"));
        assertFalse("Should not contain invalid.ext", extensions.contains("invalid.ext"));
    }
    
    @Test
    public void testIsExtensionAllowed_ValidExtensions() {
        // Test valid extensions
        assertTrue("PDF should be allowed", FileExtensionConfigService.isExtensionAllowed("pdf"));
        assertTrue("DOCX should be allowed", FileExtensionConfigService.isExtensionAllowed("docx"));
        assertTrue("XLSX should be allowed", FileExtensionConfigService.isExtensionAllowed("xlsx"));
        assertTrue("TXT should be allowed", FileExtensionConfigService.isExtensionAllowed("txt"));
        assertTrue("JPG should be allowed", FileExtensionConfigService.isExtensionAllowed("jpg"));
        assertTrue("PNG should be allowed", FileExtensionConfigService.isExtensionAllowed("png"));
    }
    
    @Test
    public void testIsExtensionAllowed_InvalidExtensions() {
        // Test invalid extensions
        assertFalse("EXE should not be allowed", FileExtensionConfigService.isExtensionAllowed("exe"));
        assertFalse("BAT should not be allowed", FileExtensionConfigService.isExtensionAllowed("bat"));
        assertFalse("COM should not be allowed", FileExtensionConfigService.isExtensionAllowed("com"));
        assertFalse("ZIP should not be allowed", FileExtensionConfigService.isExtensionAllowed("zip"));
    }
    
    @Test
    public void testIsExtensionAllowed_CaseInsensitive() {
        // Test case insensitive validation
        assertTrue("Uppercase PDF should be allowed", FileExtensionConfigService.isExtensionAllowed("PDF"));
        assertTrue("Mixed case DocX should be allowed", FileExtensionConfigService.isExtensionAllowed("DocX"));
        assertTrue("Uppercase XLSX should be allowed", FileExtensionConfigService.isExtensionAllowed("XLSX"));
    }
    
    @Test
    public void testIsExtensionAllowed_NullAndEmpty() {
        // Test null and empty extensions
        assertFalse("Null extension should not be allowed", FileExtensionConfigService.isExtensionAllowed(null));
        assertFalse("Empty extension should not be allowed", FileExtensionConfigService.isExtensionAllowed(""));
        assertFalse("Whitespace extension should not be allowed", FileExtensionConfigService.isExtensionAllowed("   "));
    }
    
    @Test
    public void testGetAllowedExtensionsList() {
        // Test getting extensions as list
        List<String> extensions = FileExtensionConfigService.getAllowedExtensionsList();
        
        assertNotNull("Extensions list should not be null", extensions);
        assertFalse("Extensions list should not be empty", extensions.isEmpty());
        assertTrue("Should contain PDF", extensions.contains("pdf"));
        assertTrue("Should contain DOCX", extensions.contains("docx"));
    }
    
    @Test
    public void testUpdateAllowedExtensions_ValidExtensions() {
        // Test updating with valid extensions
        boolean result = FileExtensionConfigService.updateAllowedExtensions("pdf,docx,xlsx");
        
        assertTrue("Update should succeed", result);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertEquals("Should have 3 extensions", 3, extensions.size());
        assertTrue("Should contain PDF", extensions.contains("pdf"));
        assertTrue("Should contain DOCX", extensions.contains("docx"));
        assertTrue("Should contain XLSX", extensions.contains("xlsx"));
        assertFalse("Should not contain TXT", extensions.contains("txt"));
    }
    
    @Test
    public void testUpdateAllowedExtensions_WithSpaces() {
        // Test updating with extensions containing spaces
        boolean result = FileExtensionConfigService.updateAllowedExtensions(" pdf , docx , xlsx ");
        
        assertTrue("Update should succeed", result);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertEquals("Should have 3 extensions", 3, extensions.size());
        assertTrue("Should contain PDF", extensions.contains("pdf"));
        assertTrue("Should contain DOCX", extensions.contains("docx"));
        assertTrue("Should contain XLSX", extensions.contains("xlsx"));
    }
    
    @Test
    public void testUpdateAllowedExtensions_WithInvalidExtensions() {
        // Test updating with invalid extensions
        boolean result = FileExtensionConfigService.updateAllowedExtensions("pdf,.docx,invalid.ext,xlsx");
        
        assertTrue("Update should succeed with valid extensions", result);
        
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertTrue("Should contain valid PDF", extensions.contains("pdf"));
        assertTrue("Should contain valid XLSX", extensions.contains("xlsx"));
        assertFalse("Should not contain invalid .docx", extensions.contains(".docx"));
        assertFalse("Should not contain invalid.ext", extensions.contains("invalid.ext"));
    }
    
    @Test
    public void testUpdateAllowedExtensions_NullInput() {
        // Test updating with null input
        boolean result = FileExtensionConfigService.updateAllowedExtensions(null);
        
        assertFalse("Update with null should fail", result);
        
        // Original extensions should remain unchanged
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertTrue("Should still contain default PDF", extensions.contains("pdf"));
    }
    
    @Test
    public void testUpdateAllowedExtensions_EmptyInput() {
        // Test updating with empty input
        boolean result = FileExtensionConfigService.updateAllowedExtensions("");
        
        assertFalse("Update with empty string should fail", result);
        
        // Original extensions should remain unchanged
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertTrue("Should still contain default PDF", extensions.contains("pdf"));
    }
    
    @Test
    public void testUpdateAllowedExtensions_AllInvalid() {
        // Test updating with all invalid extensions
        boolean result = FileExtensionConfigService.updateAllowedExtensions(".invalid,invalid.ext,123");
        
        assertFalse("Update with all invalid extensions should fail", result);
        
        // Original extensions should remain unchanged
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertTrue("Should still contain default PDF", extensions.contains("pdf"));
    }
    
    @Test
    public void testGetCurrentConfigSource() {
        // Test getting current configuration source
        String source = FileExtensionConfigService.getCurrentConfigSource();
        
        assertNotNull("Config source should not be null", source);
        assertTrue("Should be default or system.property", 
                  "default".equals(source) || "system.property".equals(source));
    }
    
    @Test
    public void testGetCurrentConfigSource_AfterUpdate() {
        // Test config source after programmatic update
        FileExtensionConfigService.updateAllowedExtensions("pdf,docx");
        
        String source = FileExtensionConfigService.getCurrentConfigSource();
        assertEquals("Should be programmatic after update", "programmatic", source);
    }
    
    @Test
    public void testThreadSafety() throws InterruptedException {
        // Test thread safety with multiple concurrent operations
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                // Perform various operations
                for (int j = 0; j < 100; j++) {
                    FileExtensionConfigService.isExtensionAllowed("pdf");
                    FileExtensionConfigService.getAllowedExtensions();
                    FileExtensionConfigService.getAllowedExtensionsList();
                    
                    if (j % 10 == 0) {
                        FileExtensionConfigService.updateAllowedExtensions("pdf,docx,xlsx");
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify final state is consistent
        Set<String> extensions = FileExtensionConfigService.getAllowedExtensions();
        assertNotNull("Extensions should not be null after concurrent access", extensions);
        assertFalse("Extensions should not be empty after concurrent access", extensions.isEmpty());
    }
    
    @Test
    public void testExtensionValidation_AlphanumericOnly() {
        // Test that only alphanumeric extensions are accepted
        boolean result1 = FileExtensionConfigService.updateAllowedExtensions("pdf123,docx456");
        assertTrue("Alphanumeric extensions should be accepted", result1);
        
        boolean result2 = FileExtensionConfigService.updateAllowedExtensions("pdf-123,docx_456");
        assertFalse("Extensions with special characters should be rejected", result2);
    }
    
    @Test
    public void testExtensionValidation_LengthLimit() {
        // Test extension length validation
        boolean result1 = FileExtensionConfigService.updateAllowedExtensions("pdf,docx");
        assertTrue("Normal length extensions should be accepted", result1);
        
        boolean result2 = FileExtensionConfigService.updateAllowedExtensions("verylongextension");
        assertFalse("Extensions longer than 10 characters should be rejected", result2);
    }
    
    @Test
    public void testDefensiveCopy() {
        // Test that returned collections are defensive copies
        Set<String> extensions1 = FileExtensionConfigService.getAllowedExtensions();
        Set<String> extensions2 = FileExtensionConfigService.getAllowedExtensions();
        
        // Modify one copy
        extensions1.add("test");
        
        // Other copy should be unchanged
        assertFalse("Modifying one copy should not affect another", extensions2.contains("test"));
        
        // Original should be unchanged
        Set<String> original = FileExtensionConfigService.getAllowedExtensions();
        assertFalse("Modifying returned copy should not affect original", original.contains("test"));
    }
}