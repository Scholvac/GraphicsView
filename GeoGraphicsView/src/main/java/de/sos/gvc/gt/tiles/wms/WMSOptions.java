package de.sos.gvc.gt.tiles.wms;


public class WMSOptions {
	
	public WMSOptions(String url, WMSVersion vers) {
		this(url, vers, "ENC");
	}
	public WMSOptions(String url, WMSVersion vers, String lay) {
		baseURL = url;
		version = vers;
		layer = lay;
	}
	public enum WMSVersion {
		VERSION_1_1_1, 
		VERSION_1_3_0
	}

	String 			format = "image/png";
	int				tileSize = 512;
	String 			styles = "";
	String 			srs = "EPSG:4326";
	String 			baseURL = "";
	String			layer = "ENC";
	WMSVersion		version;
}
