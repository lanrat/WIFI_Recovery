package com.vorsk.wifirecovery.network;

import java.util.Locale;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


public abstract class Network implements Comparable<Network>{
	
	//ctor
	public Network(String ssid){
		this.setSSID(ssid);
	}
	
	private String SSID;
	private byte security_type;
	
	//constants to use for security types
	public static final byte OPEN = 0;
	public static final byte WEP = 1;
	public static final byte WPA = 2;
	public static final byte EAP = 3;
	private static final String[] names = {
		"Open",
		"WEP",
		"WPA/WPA2 PSK",
		"802.1x EAP"};
	
	//mutator for SSID
	public String getSSID() {
		return SSID;
	}
	public void setSSID(String sSID) {
		SSID = sSID;
	}
	
	//return the English name of the security type
	public String getSecurityName(){
		return names[security_type];
	}
	
	//security mutators
	public byte getSecurity() {
		return security_type;
	}
	public void setSecurity(byte type) {
		if( type >= 0 && type <= 3){
			this.security_type = type;
		}
	}
	
	//so that we can sort by SSID
    //@Override //this line should not be commented out, but eclipse gets angry?
    public int compareTo(Network other){
    	return this.getSSID().toLowerCase(Locale.getDefault())
    			.compareTo(other.getSSID().toLowerCase(Locale.getDefault()));
    }
    
    public String toString(){
    	return this.getSSID();
    }
    
    protected abstract String getSecurityDetails();
    
    public abstract int getIcon();
    
    public String getDetails() {
    	String details = "SSID: "+ this.getSSID()+"\n";
    	details += "Security: "+ this.getSecurityName()+"\n";
    	
    	details += this.getSecurityDetails();
		
    	return details.trim();
	}
    
    private String getQRString()
    {
    	//EAP not supported
    	if (this.security_type == EAP)
    	{
    		return null;
    	}
    	String ssid = this.getSSID();
    	String security = "nopass";
    	String pass = "";
    	if (this.security_type == WEP)
    	{
    		security = "WEP";
    		pass = ((WEPNetwork)this).getWEP_Key();
    	}else if (this.security_type == WPA)
    	{
    		security = "WPA";
    		pass = ((WPANetwork)this).getWPA_Key();
    	}
    	
    	String result = String.format("WIFI:S:%s;T:%s;P:%s;;",QREscape(ssid),QREscape(security),QREscape(pass));
    	return result;
    }
    
    private String QREscape(String in)
    {
    	in = in.replace("\\", "\\\\");
    	in = in.replace(";", "\\;");
    	in = in.replace(",", "\\,");
    	in = in.replace(":", "\\:");
    	return in;
    }
    
    public Bitmap getQRCode(int size)
    {
	    final int WHITE = 0xFFFFFFFF;
	    final int BLACK = 0xFF000000;
	    
    	String qrtext = getQRString();
    	if (qrtext == null)
    	{
    		return null;
    	}
    	
    	BitMatrix result = null;
		QRCodeWriter a = new QRCodeWriter();
		try {
			result = a.encode(qrtext, BarcodeFormat.QR_CODE, size, size);
		} catch (WriterException e) {
			return null;
		}
		
		 int width = result.getWidth();
		 int height = result.getHeight();
		 int[] pixels = new int[width * height];
		 // All are 0, or black, by default
		 for (int y = 0; y < height; y++) {
			 int offset = y * width;
		     for (int x = 0; x < width; x++) {
		    	 pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
		     }
		 }
		    
	    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
    }
	
}
