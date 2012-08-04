package com.vorsk.wifirecovery;

import com.stericson.RootTools.*;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;

//for logging
import android.util.Log;
//list stuff
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.ListView;
import android.widget.Toast;
//menus
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
//do I need this?
import android.view.View;


//extend ListActivity in place of activity
public class WIFIRecoveryActivity extends ListActivity {
	private static final String TAG = "WIFI_Recovery Activity";
	private static final boolean DEBUG = false;
	
	private Network[] networks;
	private static final int REFRESH = 5; //why not?
	public static final int CMD_TIMEOUT = 2000; //timeout for cmd commands, 2 seconds
	public static String wpa_file;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //use my custom list with buttons and empty list support
        setContentView(R.layout.list);
                
        if (DEBUG) Log.d(TAG,"onCreate");
        //make the empty list display
        //getListView().setEmptyView(findViewById(R.id.empty_list));
        

        Object data = getLastNonConfigurationInstance(); //Deprecated, using anyway
        if (data == null){
        	if (DEBUG) Log.d(TAG,"getting networks");
	    	//check for root
	    	this.checkRoot();
	    	
	    	//start up the parser
	    	Parser parser = new Parser(getConfigFile());
	    	//Retrieve the networks
	    	networks = parser.getSortedNetworks();
        }else{
        	networks = (Network[]) data;
        }
        
        if (DEBUG) Log.d(TAG,"building adapter");

    	NetworkArrayAdapter adapter = new NetworkArrayAdapter(this, networks);
    	
    	if (DEBUG)  Log.d(TAG,"setting adapter");
        setListAdapter(adapter);
        
        this.updateTitle();
        //new UpdateStatus().start();
    }
    
    public void updateTitle(){
        //title stuff
		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE); //wtf?
		if (wifi.isWifiEnabled())
		{
			String ssid = wifi.getConnectionInfo().getSSID();
			if (ssid== null)
			{
				ssid = getString(R.string.wifi_disconnected);
			}else{
				ssid = getString(R.string.wifi_on) +" "+ ssid;
			}
			setTitle(ssid);
		}else{
			setTitle(R.string.wifi_off);
		}
    }
    
    //update the network list if the user have changed somethign in the settings
    @Override
    public void onResume(){
    	super.onResume();
    	this.refresh();
    }
	

    //this is deprecated, but I'm using it anyways
    @Override
    public Object onRetainNonConfigurationInstance(){
    	return this.networks;
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
    	if (DEBUG) Log.d(TAG,"clicked!");
		Network network = (Network)getListAdapter().getItem(position);
		if (DEBUG) Log.d(TAG,"got network");

		//AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//network.infoPopUp(builder).show();
		infoPopUp(network).show();
	}
    
    public AlertDialog infoPopUp(final Network network)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage( network.getDetails() )
	       .setCancelable(true)
	       .setNegativeButton(R.string.button_dismiss, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               dialog.dismiss(); //or cancel?, who knows, it works.
	           }
	       })
	       .setPositiveButton(R.string.button_share, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               //does this share the data?
	        	   Intent share = new Intent(android.content.Intent.ACTION_SEND);
	               share.setType("text/plain");
	               share.putExtra(android.content.Intent.EXTRA_SUBJECT, "Wireless Settings");
	               share.putExtra(android.content.Intent.EXTRA_TEXT, network.getDetails() );
	               startActivity(share);
	        	   
	           }
	       });
    	
    	AlertDialog alert = builder.create();
    	
    	return alert;
    }

    private boolean checkRoot()
    {   
    	if (RootTools.isRootAvailable()) {
    	    // su exists, do something
    		if (DEBUG) Log.d(TAG,"su exists");
			if (RootTools.isAccessGiven()) {
				if (DEBUG) Log.d(TAG,"We have root!");
			    // your app has been granted root access
				return true;
			}
    	}
    	if (DEBUG) Log.d(TAG,"Root Failure");
    	
    	//display dialog
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(getString(R.string.root_error))
    	       .setCancelable(false)
    	       .setNeutralButton("OK", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	               finish();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    	
    	return false;	 
    }
    
    private void refresh(){
		this.networks = null;
		this.onCreate(null);
    }
    
    public void launchBackupActivity(View v){
		startActivityForResult(new Intent(this,Backup.class), WIFIRecoveryActivity.REFRESH);
    }
    
    
	//make the menu work
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
    //when a user selects a menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()) {
		case R.id.refresh:
			//refresh the networks (the easy way)
			this.refresh();
			return true;
		case R.id.about:
			//about box here
			startActivity(new Intent(this, About.class));
			return true;
		case R.id.wifi_settings:
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			return true;
		//possibly add more menu items here
		default:
			return false;
		}
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
    	//networks changed, refresh
    	if (requestCode == WIFIRecoveryActivity.REFRESH && resultCode == Activity.RESULT_OK)
    	{
    		if (DEBUG) Log.d(TAG,"got back the activity");
    		if (data.getBooleanExtra("refresh", false)){
    			if (DEBUG) Log.d(TAG,"updating networks");
    			this.refresh();
    		}
    	}
    }
    
    private String getConfigFile(){
    	if (DEBUG) Log.d(TAG,"looking for config file");
    	String[] files = getResources().getStringArray(R.array.wpa_files);
		try {
			for (int i = 0; i < files.length; i++ ){
			String result =  RootTools.sendShell("ls "+files[i],CMD_TIMEOUT).iterator().next();

			//if (result.equals("Found")){
			if (result.equals(files[i])){
				wpa_file = files[i];
				if (DEBUG) Log.d(TAG,"found: "+wpa_file);
				return wpa_file;
			}
			}
		} catch (Exception e) {
			if (DEBUG) Log.d(TAG,"error finding file");
			//e.printStackTrace();
		}
		Toast.makeText(getApplicationContext(), R.string.find_error, Toast.LENGTH_SHORT).show();
		if (DEBUG) Log.d(TAG,"Could not find any config file");
		return null;
    }

    
}