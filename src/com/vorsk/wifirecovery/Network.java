package com.vorsk.wifirecovery;


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
	private static final String[] names = {"Open",
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
	
	//return the english name of the security type
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
    	return this.getSSID().toLowerCase().compareTo(other.getSSID().toLowerCase());
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
	
}
