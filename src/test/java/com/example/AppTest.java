package com.example;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the App class
 */
public class AppTest {
    
    @Test
    public void testAppExists() {
        assertNotNull("App class should exist", new App());
    }
}
