package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the App class
 */
public class AppTest {
    
    @Test
    public void testAppExists() {
        assertNotNull(new App(), "App class should exist");
    }
}
