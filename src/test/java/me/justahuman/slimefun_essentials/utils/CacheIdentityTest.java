package me.justahuman.slimefun_essentials.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CacheIdentityTest {
    @Test
    void cacheIdentityIsStableAndSeparatesAddressesAndServerIds() {
        String first = CacheIdentity.forServer("play.example.net:25565", "server-a");
        assertEquals(first, CacheIdentity.forServer("play.example.net:25565", "server-a"));
        assertNotEquals(first, CacheIdentity.forServer("play.example.net:25566", "server-a"));
        assertNotEquals(first, CacheIdentity.forServer("play.example.net:25565", "server-b"));
        assertEquals(64, first.length());
    }
}
