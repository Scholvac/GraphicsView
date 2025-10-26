package com.example.geocache;

public interface EvictionListener {
    void onEvict(String key, TilePayload payload);
}
