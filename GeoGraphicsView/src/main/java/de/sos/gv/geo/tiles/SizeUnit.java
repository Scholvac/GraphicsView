package de.sos.gv.geo.tiles;

public enum SizeUnit {
	Byte,
	KiloByte,
	MegaByte,
	GigaByte;

	public long toBytes(final long size) {
		switch(this) {
		case Byte:
			return size;
		case KiloByte:
			return size * 1024;
		case MegaByte:
			return size * 1024 * 1024;
		case GigaByte:
			return size * 1024 * 1024 * 1024;
		}
		return 0;
	}

	@Override
	public String toString() {
		switch(this) {
		case Byte: return "B";
		case KiloByte: return "KB";
		case MegaByte: return "MB";
		case GigaByte: return "GB";
		}
		return null;
	}
}
