package de.sos.gvc.styles.impl;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import de.sos.gvc.styles.DrawableStyle;
import de.sos.gvc.styles.IStyleDatabase;

public class StyleDatabase implements IStyleDatabase
{

	private HashMap<String, DrawableStyle> 		mStyles = new HashMap<>();


	@Override
	public DrawableStyle addStyle(String name, DrawableStyle style) {
		if (style == null || name == null)
			throw new NullPointerException("Name and style may not be null");
		return mStyles.put(name, style);
	}

	@Override
	public DrawableStyle removeStyle(String name) {
		return mStyles.remove(name);
	}




	@Override
	public DrawableStyle getStyle(String name) {
		return mStyles.get(name);
	}

	@Override
	public DrawableStyle loadStyleFromXML(String xml) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
	        XMLDecoder decoder = new XMLDecoder(bais);
	        DrawableStyle style = (DrawableStyle)decoder.readObject();
	        decoder.close();
	        bais.close();
	        return style;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String writeStyleToXML(DrawableStyle style) {
        try {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		XMLEncoder encoder = new XMLEncoder(baos);
            encoder.setExceptionListener(new ExceptionListener() {
                    @Override
					public void exceptionThrown(Exception e) {
                        System.out.println("Exception! :"+e.toString());
                    }
            });
            encoder.writeObject(style);
            encoder.close();
            String str = new String(baos.toByteArray());
			baos.close();
			return str;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	@Override
	public void loadStyles(InputStream stream) {
		try {
	        XMLDecoder decoder = new XMLDecoder(stream);
	        HashMap<String, DrawableStyle> style = (HashMap<String, DrawableStyle>)decoder.readObject();
	        decoder.close();
	        mStyles.putAll(style);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeStyles(OutputStream stream) {
		XMLEncoder encoder = new XMLEncoder(stream);
        encoder.setExceptionListener(new ExceptionListener() {
                @Override
				public void exceptionThrown(Exception e) {
                    System.out.println("Exception! :"+e.toString());
                }
        });
        encoder.writeObject(mStyles);
        encoder.close();
	}



}
