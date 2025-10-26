package com.example.geocache;

import java.awt.image.BufferedImage;

public interface TilePayload {
    enum Kind { IMAGE, ENCODED }
    Kind kind();

    final class Image implements TilePayload {
        private final BufferedImage value;
        public Image(BufferedImage value){ this.value = value; }
        public BufferedImage value(){ return value; }
        @Override public Kind kind(){ return Kind.IMAGE; }
    }
    final class Encoded implements TilePayload {
        private final byte[] value;
        private final String contentType;
        public Encoded(byte[] value, String contentType){ this.value=value; this.contentType=contentType; }
        public byte[] value(){ return value; }
        public String contentType(){ return contentType; }
        @Override public Kind kind(){ return Kind.ENCODED; }
    }
}
