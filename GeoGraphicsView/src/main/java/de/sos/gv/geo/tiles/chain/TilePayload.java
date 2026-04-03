package de.sos.gv.geo.tiles.chain;

import java.awt.image.BufferedImage;

/**
 * Discriminated union of the two data forms a tile can take as it moves through the pipeline.
 *
 * <ul>
 *   <li>{@link Image}   — a decoded {@link java.awt.image.BufferedImage}; ready to paint, costly in memory.</li>
 *   <li>{@link Encoded} — raw compressed bytes (PNG/JPEG); cheap to store, needs decoding before use.</li>
 * </ul>
 *
 * {@link TileChain} converts between the two forms via {@link ImageIOTranscoder} whenever a stage
 * requires a different format than the one available.
 * Touch this if a third payload form (e.g. GPU texture) is ever needed.
 */
public interface TilePayload {
	/** Discriminator used by {@link TileStage#provides()} and {@link TileStage#accepts()}. */
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
