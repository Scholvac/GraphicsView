package de.sos.gv.geo.tiles.chain;

import java.util.Objects;

public final class TileId {
	private final int z,x,y;
	private final String style, ext;

	public TileId(final int z, final int x, final int y, final String style, final String ext){
		this.z=z; this.x=x; this.y=y; this.style=style; this.ext=ext;
	}
	public int getZ(){ return z; } public int getX(){ return x; } public int getY(){ return y; }
	public String getStyle(){ return style; }
	public String getExt(){ return ext; }
	public String cacheKey(){ return style + "/" + z + "/" + x + "/" + y + "." + ext; }

	@Override
	public boolean equals(final Object o){
		if(this==o) return true;
		if(!(o instanceof TileId)) return false;
		final TileId t=(TileId)o;
		return z==t.z && x==t.x && y==t.y
				&& Objects.equals(style,t.style)
				&& Objects.equals(ext,t.ext);
	}
	@Override public int hashCode(){ return Objects.hash(z,x,y,style,ext); }
	@Override public String toString(){ return "TileId{"+cacheKey()+"}"; }
}
