package com.pricer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TickData model class.
 */
public class TickDataTest {

    @Test
    void testTickDataCreation() {
        LocalDateTime now = LocalDateTime.now();
        TickData tickData = new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now);

        assertEquals("P1", tickData.getSource());
        assertEquals("USD.INR", tickData.getCurrency());
        assertEquals("SPOT", tickData.getType());
        assertEquals(10000.0, tickData.getVolume());
        assertEquals(82.40, tickData.getBidRate());
        assertEquals(82.60, tickData.getAskRate());
        assertEquals(now, tickData.getTimestamp());
    }

    @Test
    void testTickDataDefaultConstructor() {
        TickData tickData = new TickData();

        assertNull(tickData.getSource());
        assertNull(tickData.getCurrency());
        assertNull(tickData.getType());
        assertNull(tickData.getVolume());
        assertNull(tickData.getBidRate());
        assertNull(tickData.getAskRate());
        assertNull(tickData.getTimestamp());
    }

    @Test
    void testTickDataSetters() {
        TickData tickData = new TickData();
        LocalDateTime now = LocalDateTime.now();

        tickData.setSource("P2");
        tickData.setCurrency("EUR.USD");
        tickData.setType("FORWARD");
        tickData.setVolume(20000.0);
        tickData.setBidRate(1.10);
        tickData.setAskRate(1.12);
        tickData.setTimestamp(now);

        assertEquals("P2", tickData.getSource());
        assertEquals("EUR.USD", tickData.getCurrency());
        assertEquals("FORWARD", tickData.getType());
        assertEquals(20000.0, tickData.getVolume());
        assertEquals(1.10, tickData.getBidRate());
        assertEquals(1.12, tickData.getAskRate());
        assertEquals(now, tickData.getTimestamp());
    }

    @Test
    void testTickDataEquality() {
        LocalDateTime now = LocalDateTime.now();
        TickData tickData1 = new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now);
        TickData tickData2 = new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now);
        TickData tickData3 = new TickData("P2", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now);

        assertEquals(tickData1, tickData2);
        assertNotEquals(tickData1, tickData3);
        assertEquals(tickData1.hashCode(), tickData2.hashCode());
    }

    @Test
    void testTickDataToString() {
        LocalDateTime now = LocalDateTime.now();
        TickData tickData = new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now);

        String toString = tickData.toString();
        assertTrue(toString.contains("P1"));
        assertTrue(toString.contains("USD.INR"));
        assertTrue(toString.contains("SPOT"));
        assertTrue(toString.contains("10000.0"));
        assertTrue(toString.contains("82.40"));
        assertTrue(toString.contains("82.60"));
    }
}