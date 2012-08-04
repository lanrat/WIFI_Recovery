package com.vorsk.wifirecovery;

public class WEPNetwork extends Network{
	
	public static final String key_mgmt = "NONE";
	
	private String key;
	
	public WEPNetwork(String ssid, String key){
		super(ssid);
		setSecurity(Network.WEP);
		setWEP_Key(key);
	}

	//mutators
	public String getWEP_Key() {
		return key;
	}
	public void setWEP_Key(String key) {
		//should check to make sure there are only the chars 0-9 and A-F
		if ((key.length() == 10) ||
				(key.length() == 26) ||
				(key.length() == 58)){
			this.key = key;
		}
	}

	@Override
	protected String getSecurityDetails() {
		String key = "KEY: " + this.getWEP_Key();
		return key;
	}
	
	public int getIcon(){
		return R.drawable.wep;
	}
	
}
