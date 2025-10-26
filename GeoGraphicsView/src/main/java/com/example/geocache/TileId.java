package com.example.geocache;

import java.util.Objects;

public final class TileId {
    private final int z,x,y;
    private final String style, ext;
    public TileId(int z, int x, int y, String style, String ext){ this.z=z; this.x=x; this.y=y; this.style=style; this.ext=ext; }
    public int getZ(){ return z; } public int getX(){ return x; } public int getY(){ return y; }
    public String getStyle(){ return style; } public String getExt(){ return ext; }
    public String cacheKey(){ return style + "/" + z + "/" + x + "/" + y + "." + ext; }
    @Override public boolean equals(Object o){ if(this==o) return true; if(!(o instanceof TileId)) return false; TileId t=(TileId)o; return z==t.z && x==t.x && y==t.y && Objects.equals(style,t.style) && Objects.equals(ext,t.ext); }
    @Override public int hashCode(){ return Objects.hash(z,x,y,style,ext); }
    @Override public String toString(){ return "TileId{"+cacheKey()+"}"; }
}
