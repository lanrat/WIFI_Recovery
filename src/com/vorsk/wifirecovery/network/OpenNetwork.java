package com.vorsk.wifirecovery.network;

import com.vorsk.wifirecovery.R;

public class OpenNetwork extends Network{
	
	public static final String key_mgmt = "NONE";
	
	public OpenNetwork(String ssid){
		super(ssid);
		setSecurity(Network.OPEN);
	}

	@Override
	protected String getSecurityDetails() {
		return "";
	}
	
	public int getIcon(){
		return R.drawable.open;
	}
	
}
