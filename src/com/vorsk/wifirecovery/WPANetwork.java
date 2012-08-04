package com.vorsk.wifirecovery;

public class WPANetwork extends Network{
	
	public static final String key_mgmt = "WPA-PSK";
	
	private String key;
	
	public WPANetwork(String ssid, String key){
		super(ssid);
		setSecurity(Network.WPA);
		setWPA_Key(key);
	}

	//mutators
	public String getWPA_Key() {
		return key;
	}
	public void setWPA_Key(String key) {
		if ((key.length() >= 8) && (key.length() <= 63)){
			this.key = key;
		}
	}

	@Override
	protected String getSecurityDetails() {
		String key = "KEY: "+ this.getWPA_Key();
		return key;
	}
	
	public int getIcon(){
		return R.drawable.wpa;
	}
	
}
