package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;

public interface TilePayload {
	enum Kind { IMAGE, ENCODED }
	Kind kind();

	final class Image implements TilePayload {
		private final BufferedImage mValue;

		public Image(final BufferedImage value){
			this.mValue = value;
		}
		public BufferedImage value(){ return mValue; }
		@Override public Kind kind(){ return Kind.IMAGE; }
	}
	final class Encoded implements TilePayload {
		private final byte[] mValue;
		private final String mContentType;

		public Encoded(final byte[] value, final String contentType){
			this.mValue=value; this.mContentType=contentType;
		}
		public byte[] value(){ return mValue; }
		public String contentType(){ return mContentType; }
		@Override public Kind kind(){ return Kind.ENCODED; }
	}
}
