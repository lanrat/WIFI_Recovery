package com.vorsk.wifirecovery.network;

import com.vorsk.wifirecovery.R;

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
		//check removed because android allows invalid keys
		//check if key is enclosed in quotes, remove if so.
		if (key.charAt(0) == '"' && key.charAt(key.length()-1) == '"')
		{
			key = key.substring(1,key.length()-1);
		}
		this.key = key;
	}

	@Override
	protected String getSecurityDetails() {
		String key = "KEY: " + this.getWEP_Key();
		return key;
	}
	
	public int getIcon(){
		return R.drawable.network_wep;
	}
	
}
