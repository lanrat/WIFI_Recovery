package com.vorsk.wifirecovery;


public class EAPNetwork extends Network{
	
	//public static final String key_mgmt = "WPA-EAP IEEE8021X";
	public static final String key_mgmt = "WPA-EAP";
	
	//EAP Methods
	public static final byte PEAP = 0;
	public static final byte TLS = 1;
	public static final byte TTLS = 2;
	
	//Phase 2
	public static final byte NONE = 0;
	public static final byte PAP = 1;
	public static final byte MSCHAP = 2;
	public static final byte MSCHAPV2 = 3;
	public static final byte GTC = 4;
	
	//strings
	private static final String[] EAP_names = {
		"PEAP",
		"TLS",
		"TTLS",
		"None",
		"PAP",
		"MSCHAP",
		"MSCHAPV2",
		"GTC"};

	public static final int icon = R.drawable.eap;
	
	private byte eap_method;
	private byte phase2_auth = EAPNetwork.NONE;
	private String identity;
	private String password;
	private String anonymous;
	
	public EAPNetwork(String ssid, byte eap){
		super(ssid);
		setSecurity(Network.EAP);
		setEAP(eap);
	}
	
	public String getEAPMethodName(){
		return EAP_names[eap_method];
	}
	
	public String getPhase2Name(){
		return EAP_names[phase2_auth+3]; //add 3 for concationated offset
	}
	
	//given the string from the config file, determines the apropriate eap method
	public static byte findEAP(String eap){
		if (eap.equals("PEAP")){
			return PEAP;
		}else if (eap.equals("TLS")){
			return TLS;
		}else if (eap.equals("TTLS")){
			return TTLS;
		}
		return (Byte) null;
	}
	
	
	//given the string from the config file, determines the appropriate phase2
	public static byte findPhase2(String phase2){
		if (phase2.equals("auth=PAP")){
			return PAP;
		}else if (phase2.equals("auth=MSCHAP")){
			return MSCHAP;
		}else if (phase2.equals("auth=MSCHAPV2")){
			return MSCHAPV2;
		}else if (phase2.equals("auth=GTC")){
			return GTC;
		}
		return NONE;
	}

	//eap mutators
	public byte getEAP() {
		return eap_method;
	}
	public void setEAP(byte eap) {
		if ( eap >=0 && eap <= 2){
			this.eap_method = eap;
		}
	}

	//phase 2 mutators
	public byte getPhase2() {
		return phase2_auth;
	}
	public void setPhase2(byte phase2) {
		if (phase2 >= 0 && phase2 <= 4){
			this.phase2_auth = phase2;
		}
	}

	//more mutators
	public String getIdentity() {
		return identity;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getAnonymous() {
		return anonymous;
	}
	public void setAnonymous(String anonymous) {
		this.anonymous = anonymous;
	}

	@Override
	protected String getSecurityDetails() {
		String str = "EAP Method: "+this.getEAPMethodName()+"\n";
		str += "Phase2 Auth: "+this.getPhase2Name()+"\n";
		if (identity != null){
			str += "Identity: "+getIdentity()+"\n";
		}
		if (password != null){
			str += "Password: "+getPassword()+"\n";
		}
		if (anonymous != null){
			str += "Anonymous ID: "+getAnonymous()+"\n";
		}
		return str;
	}
	
	public int getIcon(){
		return R.drawable.eap;
	}

	
	
}
