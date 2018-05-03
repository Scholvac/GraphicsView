//package de.sos.gvc.drawables;
//
//import java.awt.Color;
//import java.awt.LinearGradientPaint;
//import java.awt.MultipleGradientPaint;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.thoughtworks.xstream.XStream;
//
//import javafx.scene.paint.CycleMethod;
//
//public class StyleRepository {
//
//	private ArrayList<DrawableStyle>	mStyles = new ArrayList<>();
//	
//	
//	public static void main(String[] args) {
//		DrawableStyle style = new DrawableStyle();
//		style.setName("default");
//		style.setFillPaint(new LinearGradientPaint(-1, -1, 1, 1, new float[] {0, 1}, new Color[] {Color.RED, Color.BLUE}));
//		style.setLinePaint(Color.BLACK);
//		
//		
//		DrawableStyle style2 = new DrawableStyle();
//		style2.setName("default2");
//		style2.setFillPaint(new LinearGradientPaint(0, -1, 0, 1, new float[] {0, 1}, new Color[] {Color.YELLOW, Color.GREEN}));
//		style2.setLinePaint(Color.BLUE);
//	
//		StyleRepository rep = new StyleRepository();
//		rep.addStyle(style);
//		rep.addStyle(style2);
//		
//		
//		XStream s = new XStream();
//		String str = s.toXML(rep); System.out.println(str);
//		StyleRepository rep2 = (StyleRepository) s.fromXML(str);
//		
////		Gson gson = new GsonBuilder().setPrettyPrinting().create();
////		String str = gson.toJson(rep);
////		System.out.println(str);
////		StyleRepository rep2 = gson.fromJson(str, StyleRepository.class);
//		System.out.println();
//		
//		
//	}
//	public void addStyle(DrawableStyle style) {
//		mStyles.add(style);
//	}
//	
//	
//	
//	public static void loadFile(File f) {
//		
//	}
//}
