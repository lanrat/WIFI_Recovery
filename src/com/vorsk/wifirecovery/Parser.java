package com.vorsk.wifirecovery;

//for array lists
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
//for root
import com.stericson.RootTools.*;
//for logging
import android.util.Log;
//import android.widget.Toast;

public class Parser{
	private static final String TAG = "WIFI_Recovery Parser";
	private static final boolean DEBUG = false;
	
	private ArrayList<Network> networks = new ArrayList<Network>();
	private List<String> file;
	private Network[] networksArray;
	
	//ctor
	public Parser(String file_name){
		if (DEBUG) Log.d(TAG,"reading file");
		this.readFile(file_name);
		if (DEBUG) Log.d(TAG,"done reading file, building network");
		this.buildNetworks();
		if (DEBUG) Log.d(TAG,"done building networks");
	}
	
	private void buildNetworks(){
		//if we do not have a file
		if (file == null){
			return;
		}
		String line;
		Iterator<String> it = file.iterator();
		HashMap<String, String> network;
		
		while (it.hasNext()){
			//Log.d(TAG,"next!");
			line = it.next();
			if (line != null){
				if (DEBUG) Log.d(TAG,"line: "+line);
				
				if (line.startsWith("network") && line.endsWith("{")){
					if (DEBUG) Log.d(TAG,"found network block");
					network = new HashMap<String, String>();
					
					//we found a network block
					while(it.hasNext() && !line.contains("}")){
						line = it.next();
						if (line != null){
							if (line.contains("}")){
								if (DEBUG) Log.d(TAG,"end of data!");
								break;
							}
							if (DEBUG) Log.d(TAG,"adding data: "+ line);
							
							int sep = line.indexOf("=");
							if (DEBUG) Log.d(TAG,"Sep: "+sep);
							if (sep != -1)
							{
								if (DEBUG) Log.d(TAG,"A: "+line.substring(0, sep));
								if (DEBUG) Log.d(TAG,"B: "+line.substring(sep+1, line.length()));
								network.put(line.substring(0, sep).trim(), line.substring(sep+1, line.length()));
							}
							//String[] data = line.split("="); //breaks if the ssid has an = in is
							//network.put(data[0].trim(), data[1].trim());
						}
					}
					if (DEBUG) Log.d(TAG,"adding network");
					//network HashMap is complete, add it to the networks ArrayList
					this.addNetwork(network);
					if (DEBUG) Log.d(TAG,"network added");
				}
			}
			
		}
	}
	
	private void addNetwork(HashMap<String, String> networkData) {
		//first check to make sure the network contains required fields
		if (!networkData.containsKey("ssid")){
			if (DEBUG) Log.d(TAG,"malformed network entry, missing ssid");
			return;
		}
		
		//fixing bug for HTC sense phones
		if (networkData.containsKey("psk")) {
			if (DEBUG) Log.d(TAG,"found wpa network without key_mgmt, overrideing");
			networkData.put("key_mgmt", WPANetwork.key_mgmt);
		}
		
		if (!networkData.containsKey("key_mgmt")){
			if (DEBUG) Log.d(TAG,"malformed network entry, missing key_mgmt");
			return;
		}
		
		//determine the network type and build it
		String ssid = networkData.get("ssid");
		ssid = ssid.substring(1, ssid.length()-1);
		//Log.d(TAG,"adding network "+ssid);
		Network network = null;
		String type = networkData.get("key_mgmt");
		//Log.d(TAG,"type:"+type);
		if (type.equals(WEPNetwork.key_mgmt)){ //wep or open
			if (networkData.containsKey("wep_key0")){ //wep network
				//create wep network
				//Log.d(TAG,"found wep network");
				network = new WEPNetwork(ssid, networkData.get("wep_key0"));
				
			}else{ //open network
				//create open network
				network = new OpenNetwork(ssid);
				
			}
		}else if (type.equals(WPANetwork.key_mgmt)){ //wpa network
			//create wpa network
			if (networkData.containsKey("psk")){
				String psk = networkData.get("psk");
				network = new WPANetwork(ssid, psk.substring(1,psk.length()-1));
			}else{
				if (DEBUG) Log.d(TAG,"malformed wpa network (missing key)");
			}
			
		}else if (type.contains(EAPNetwork.key_mgmt)){ //eap network
			//create eap network, the long complicated one.....
			network = new EAPNetwork(ssid, EAPNetwork.findEAP(networkData.get("eap")));
			if (networkData.containsKey("phase2")){
				((EAPNetwork)network).setPhase2(EAPNetwork.findPhase2(networkData.get("phase2").replaceAll("\"", "")));
			}
			if (networkData.containsKey("identity")){
				String ident = networkData.get("identity");
				((EAPNetwork)network).setIdentity(ident.substring(1, ident.length()-1));
			}
			if (networkData.containsKey("password")){
				String pass = networkData.get("password");
				((EAPNetwork)network).setPassword(pass.substring(1, pass.length()-1));
			}
			if (networkData.containsKey("anonymous_identity")){
				String anon = networkData.get("anonymous_identity");
				((EAPNetwork)network).setAnonymous(anon.substring(1, anon.length()-1));
			}
			
		}else {
			if (DEBUG) Log.d(TAG,"unknown network type");
			return;
		}
		//Log.d(TAG,"Ready to add network");
		if (network != null){
			networks.add(network);
		}else{
			//Log.e(TAG, "Unknown Network Type");
		}
		//Log.d(TAG,"add complete");
		
		
	}
	
	private void readFile(String file_name){
		//check to make sure the file exists
		//int permissions = RootTools.getFilePermissions(file_name);
		//Log.d(TAG,"Permissions: "+permissions);
		
		//read the contends of the file into a variable
		try {
			this.file = RootTools.sendShell("cat " + file_name,WIFIRecoveryActivity.CMD_TIMEOUT);
			//this.file = RootTools.sendShell("cat " + file_name);
		} catch (Exception e) {
			if (DEBUG) Log.d(TAG,"cant access file: "+ file_name);
			//e.printStackTrace();
			//Toast.makeText(getApplicationContext(), "Hi there", Toast.LENGTH_SHORT).show();
		}
	}

	public Network[] getSortedNetworks() {
		//Log.d(TAG,"converting...	");
		if (networksArray == null){
			Collections.sort(networks);
			networksArray = networks.toArray(new Network[networks.size()]);
			//Arrays.sort(networksArray);
			return networksArray;
		}
		return networksArray;
	}
	
	public String[] getSSISs(){
		String[] SSIDs = new String[networks.size()];
		
		for (int i = 0; i < networks.size(); i++){
			SSIDs[i] = networks.get(i).getSSID();
		}
		return SSIDs;
		
	}



}
