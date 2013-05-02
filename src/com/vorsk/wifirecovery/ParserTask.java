package com.vorsk.wifirecovery;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;
import com.vorsk.wifirecovery.network.EAPNetwork;
import com.vorsk.wifirecovery.network.Network;
import com.vorsk.wifirecovery.network.OpenNetwork;
import com.vorsk.wifirecovery.network.WEPNetwork;
import com.vorsk.wifirecovery.network.WPANetwork;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.util.Log;

import android.widget.Toast;

public class ParserTask extends AsyncTask<Void, Void, NetworkArrayAdapter>{
	private static final String TAG = "WIFI_Recovery Parser";
	private static final boolean DEBUG = false;
	
	private static String tempfileName = "wpa.conf";
	private static Network[] networks;
	private ListActivity myActivity;
	ProgressDialog dialog;
	private int errorCode;
	private int screenOrientation;
	private static final int ERROR_NO_ROOT = 1;
	private static final int ERROR_NO_FILE = 2;
	
	//ctor
	private ParserTask(ListActivity activity){
		this.myActivity = activity;
		this.errorCode = 0;
	}
	
	//factory method for parsing the file
	public static void loadNetworks(ListActivity activity)
	{	
		(new ParserTask(activity)).execute();
	}
	
	
	//like init but forces data refresh
	public static void refresh(ListActivity activity)
	{	
		ParserTask.networks = null;
		ParserTask.loadNetworks(activity);
	}
	
	@Override
	protected void onPreExecute() {
		//this.screenOrientation = this.myActivity.getRequestedOrientation();
		//this.myActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
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
			FileReader file = this.readFile(file_name);
			if (file == null)
			{
				this.errorCode = ERROR_NO_FILE;
				return null;
			}
			this.buildNetworks(file);
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
		//this.myActivity.setRequestedOrientation(this.screenOrientation);
		
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
	
	private void buildNetworks(FileReader file){
		//if we do not have a file
		if (file == null){
			return;
		}
		BufferedReader in = new BufferedReader(file);
		String line;
		HashMap<String, String> networkData;
		ArrayList<Network> networkList = new ArrayList<Network>();
		Network network;
		
		try {
			while (in.ready())
			{
				line = in.readLine();
				if (line != null){
					if (DEBUG) Log.d(TAG,"line: "+line);
					
					if (line.startsWith("network") && line.endsWith("{")){
						if (DEBUG) Log.d(TAG,"found network block");
						networkData = new HashMap<String, String>();
						
						//we found a network block
						while(in.ready() && !line.contains("}")){
							line = in.readLine();
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
									networkData.put(line.substring(0, sep).trim(), line.substring(sep+1, line.length()));
								}
							}
						}
						if (DEBUG) Log.d(TAG,"adding network");
						//network HashMap is complete, add it to the networks ArrayList
						network = this.addNetwork(networkData);
						if (network != null)
						{
							networkList.add(network);
						}
						if (DEBUG) Log.d(TAG,"network added");
					}
				}
				
			}
		} catch (IOException e) {
			if (DEBUG) Log.e(TAG,"Error reading file");
			return;
		}
		this.deleteCachFile();
		Collections.sort(networkList);
		networks = networkList.toArray(new Network[networkList.size()]);
	}
	
	private Network addNetwork(HashMap<String, String> networkData) {
		//first check to make sure the network contains required fields
		if (!networkData.containsKey("ssid")){
			if (DEBUG) Log.d(TAG,"malformed network entry, missing ssid");
			return null;
		}
		
		//fixing bug for HTC sense phones
		if (networkData.containsKey("psk")) {
			if (DEBUG) Log.d(TAG,"found wpa network without key_mgmt, overrideing");
			networkData.put("key_mgmt", WPANetwork.key_mgmt);
		}
		
		if (!networkData.containsKey("key_mgmt")){
			if (DEBUG) Log.d(TAG,"malformed network entry, missing key_mgmt");
			return null;
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
				return null;
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
			return null;
		}
		if (DEBUG) Log.d(TAG,"Ready to add network");
		if (network != null){
			return network;
		}else{
			if (DEBUG) Log.e(TAG, "Unknown Network Type");
		}
		return null;
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
	
	private FileReader readFile(String file_name){
		//copy file to application temp dir
		String destination = this.myActivity.getCacheDir().getAbsolutePath()+"/"+tempfileName;
		if (!RootTools.copyFile(file_name, destination, false, false))
		{
			if (DEBUG) Log.d(TAG,"cant move file: "+ file_name+" to "+destination );
			return null;
		}
		//update the file permissions
		CommandCapture command = new CommandCapture(0,"chmod 666 "+destination);
		try {
			RootTools.getShell(true).add(command).waitForFinish();
		} catch (Exception e1) {
			if (DEBUG) Log.d(TAG,"unable to update permissions of: "+destination );
			return null;
		}
		
		//open the file
		try {
			return new FileReader(destination);
		} catch (FileNotFoundException e) {
			if (DEBUG) Log.d(TAG,"Unable to return file strem for temp file: "+destination);
			return null;
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
	
	private boolean deleteCachFile()
	{
		String destination = this.myActivity.getCacheDir().getAbsolutePath()+"/"+tempfileName;
		return (new RootTools()).deleteFileOrDirectory(destination, false);
	}

}
