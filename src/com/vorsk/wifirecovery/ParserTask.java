package com.vorsk.wifirecovery;

//for array lists
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
//for root
import com.stericson.RootTools.RootTools;
import com.vorsk.wifirecovery.network.EAPNetwork;
import com.vorsk.wifirecovery.network.Network;
import com.vorsk.wifirecovery.network.OpenNetwork;
import com.vorsk.wifirecovery.network.WEPNetwork;
import com.vorsk.wifirecovery.network.WPANetwork;
//for logging
//import android.content.Context;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
//import android.widget.Toast;
import android.widget.Toast;

public class ParserTask extends AsyncTask<Void, Void, NetworkArrayAdapter>{
	private static final String TAG = "WIFI_Recovery Parser";
	private static final boolean DEBUG = true;
	
	private ArrayList<Network> networksListRM = new ArrayList<Network>(); //TODO remove this variable
	private List<String> file; //TODO remove the need for this
	private static Network[] networks;
	private ListActivity myActivity;
	ProgressDialog dialog;
	private int errorCode;
	private static final int ERROR_NO_ROOT = 1;
	private static final int ERROR_NO_FILE = 2;
	
	//ctor
	private ParserTask(ListActivity activity){
		this.myActivity = activity;
		this.errorCode = 0;
	}
	
	//factory method for parsing the file
	public static void init(ListActivity activity)
	{	
		ParserTask p = new ParserTask(activity);
		p.execute();
	}
	
	
	//like init but forces data refresh
	public static void refresh(ListActivity activity)
	{	
		ParserTask.networks = null;
		ParserTask.init(activity);
	}
	
	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(this.myActivity);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMessage(this.myActivity.getString(R.string.loading));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
	}
	
	@Override
	protected NetworkArrayAdapter doInBackground(Void... args) {
		if (networks == null)
		{
			if (!this.checkRoot()){
				this.errorCode = ERROR_NO_ROOT;
				return null;
			}
			String file_name = getConfigFile();
			if (file_name == null)
			{
				this.errorCode = ERROR_NO_FILE;
				return null;
			}
			if (!this.readFile(file_name))
			{
				this.errorCode = ERROR_NO_FILE;
				return null;
			}
			this.buildNetworks();
			if (DEBUG) Log.d(TAG,"done building "+networksListRM.size()+" networks");
			
			Collections.sort(networksListRM);
			networks = networksListRM.toArray(new Network[networksListRM.size()]);
		}
		
		if (DEBUG) Log.d(TAG, "building adapter");
		return new NetworkArrayAdapter(this.myActivity,networks);
	}
	
	@Override
	protected void onPostExecute(NetworkArrayAdapter result) {
		//close loading message
		dialog.dismiss();
		
		if (result == null){
			if (this.errorCode == ERROR_NO_FILE){
				Toast.makeText(myActivity, R.string.find_error, Toast.LENGTH_SHORT).show();
				return;
			}else if (this.errorCode == ERROR_NO_ROOT)
			{
				// display dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(this.myActivity);
				builder.setMessage(this.myActivity.getString(R.string.root_error)).setCancelable(false)
						.setNeutralButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								myActivity.finish();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
		
		//update the list of networks
		if (DEBUG) Log.d(TAG, "setting adapter");
		this.myActivity.setListAdapter(result);
		
	}
	
	
	private boolean checkRoot() {
		if (DEBUG) Log.d(TAG, "testing for root");
		if (RootTools.isAccessGiven()) {
			if (DEBUG) Log.d(TAG, "We have root!");
			// the app has been granted root access
			return true;
		}

		if (DEBUG) Log.d(TAG, "Root Failure");

		return false;
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
			byte eap = EAPNetwork.findEAP(networkData.get("eap"));
			if (eap == EAPNetwork.UNKNOWN){
				if (DEBUG) Log.d(TAG,"unknown eap network type");
				return;
			}
			network = new EAPNetwork(ssid, eap);
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
			networksListRM.add(network);
		}else{
			//Log.e(TAG, "Unknown Network Type");
		}
		//Log.d(TAG,"add complete");
		
		
	}
	
	
	private String getConfigFile() {
		if (DEBUG) Log.d(TAG, "looking for config file");
		String[] files = this.myActivity.getResources().getStringArray(R.array.wpa_files);

		for (int i = 0; i < files.length; i++) {
			if (RootTools.exists(files[i])) {
				if (DEBUG)
					Log.d(TAG, "found: " + files[i]);
				return files[i];
			}
		}

		if (DEBUG)
			Log.d(TAG, "Could not find any config file");
		return null;
	}
	
	
	//TODO REDO with temp files
	//THIS causes errors reading the file on startup sometimes
	private boolean readFile(String file_name){
		//check to make sure the file exists
		//int permissions = RootTools.getFilePermissions(file_name);
		//Log.d(TAG,"Permissions: "+permissions);
		
		//read the contends of the file into a variable
		try {
			this.file = RootTools.sendShell("cat " + file_name,HomeActivity.CMD_TIMEOUT);
			//this.file = RootTools.sendShell("cat " + file_name);
			return true;
		} catch (Exception e) {
			if (DEBUG) Log.d(TAG,"cant access file: "+ file_name);
			return false;
			//e.printStackTrace();
			//Toast.makeText(getApplicationContext(), "Hi there", Toast.LENGTH_SHORT).show();
		}
	}

	public Network[] getNetworks() {
		return networks;
	}
	
	public String[] getSSIDs(){
		String[] SSIDs = new String[networks.length];
		
		for (int i = 0; i < networks.length; i++){
			SSIDs[i] = networks[i].getSSID();
		}
		return SSIDs;
		
	}

}
