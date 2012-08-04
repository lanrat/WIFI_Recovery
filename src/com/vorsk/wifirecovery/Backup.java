package com.vorsk.wifirecovery;

import java.io.File;

import com.stericson.RootTools.RootTools;
import com.vorsk.wifirecovery.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class Backup extends Activity implements OnClickListener {
	private static final String TAG = "WIFI_Recovery Backup";
	private static final boolean DEBUG = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backup);
		
		//set the button listeners! condensed version
		findViewById(R.id.backup_back_button).setOnClickListener(this);
		findViewById(R.id.backup_button).setOnClickListener(this);
		findViewById(R.id.restore_button).setOnClickListener(this);
		findViewById(R.id.reset_button).setOnClickListener(this);

	}
	
	//hook the button up!
	public void onClick(View v){
		switch (v.getId()) {
		case R.id.backup_back_button:
			finish(); //go back to the main activity1
			break;
		case R.id.backup_button:
			this.backupCopy();
			break;
		case R.id.restore_button:
			this.restoreCopy();
			break;
		case R.id.reset_button:
			this.resetConfirm();
			break;
		default:
			break;
		}
	}
	
	private void backupCopy(){
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ){
			String location =  Environment.getExternalStorageDirectory().getPath() +"/"+ getString(R.string.backup_file_name);
			try {
				if (DEBUG) Log.d(TAG,"copying to: "+location);
				//RootTools.sendShell("cp " + WIFIRecoveryActivity.wpa_file + " " +location,WIFIRecoveryActivity.CMD_TIMEOUT);
				RootTools.copyFile(WIFIRecoveryActivity.wpa_file, location, false, false);
				Toast.makeText(getApplicationContext(), R.string.backup_done, Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				if (DEBUG) Log.d(TAG,"can't backup file");
				//e.printStackTrace();
				Toast.makeText(getApplicationContext(), R.string.backup_error, Toast.LENGTH_SHORT).show();
			}
		} else {
			//error!
			Toast.makeText(getApplicationContext(), R.string.sd_error, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void restoreCopy(){
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
			String location =  Environment.getExternalStorageDirectory().getPath() +"/"+ getString(R.string.backup_file_name);
			if (DEBUG) Log.d(TAG,"checking backup exists at: "+location);

			if (new File(location).isFile()){
				//do a test parse!
				if (new Parser(location).getSSISs().length <= 0){
					if (DEBUG) Log.d(TAG,"malformed backup file");
					Toast.makeText(getApplicationContext(), R.string.parse_error, Toast.LENGTH_SHORT).show();
					return;
				}
				
				WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE); //wtf?
				//get wifi state, if off turn on
				boolean wifiEnabled = false;
				if (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
						wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING ||
						wifi.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN){
					wifiEnabled = true;
					wifi.setWifiEnabled(false);
				}
				
				//copy file and set permissions to 0660
				try {
					
					if (DEBUG) Log.d(TAG,"copying file from: "+location);
					//RootTools.sendShell("cp " + location +" "+ WIFIRecoveryActivity.wpa_file,WIFIRecoveryActivity.CMD_TIMEOUT);
					RootTools.copyFile(location, WIFIRecoveryActivity.wpa_file, true, false);
					RootTools.sendShell("chown system:wifi "+ WIFIRecoveryActivity.wpa_file,WIFIRecoveryActivity.CMD_TIMEOUT);
					RootTools.sendShell("chmod 0660 "+ WIFIRecoveryActivity.wpa_file,WIFIRecoveryActivity.CMD_TIMEOUT);

					Toast.makeText(getApplicationContext(), R.string.restore_done, Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					if (DEBUG) Log.d(TAG,"can't restore backup file");
					Toast.makeText(getApplicationContext(), R.string.restore_error, Toast.LENGTH_SHORT).show();
					//e.printStackTrace();
				}
				
				//turn wifi on if previously turned off
				if (wifiEnabled){
					wifi.setWifiEnabled(true);
					wifi.startScan(); //reconnect
				}
			
			}else{
				Toast.makeText(getApplicationContext(), R.string.no_backup_file, Toast.LENGTH_SHORT).show();
			}
		} else {
			//error!
			Toast.makeText(getApplicationContext(), R.string.sd_error, Toast.LENGTH_SHORT).show();
		}
		
		quitAndRefresh();
	}
	
	//make the menu work
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.backup_menu, menu);
		return true;
	}
    //when a user selects a menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()) {
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
    
    
    private void resetConfirm(){
    	new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.reset_title)
        .setMessage(R.string.reset_message)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Backup.this.reset();    
            }
        })
        .setNegativeButton(R.string.no, null)
        .show();
    }
    
    private void reset(){
		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE); //wtf?
		//get wifi state, if off turn on
		boolean wifiEnabled = false;
		if (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
				wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING ||
				wifi.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN){
			wifiEnabled = true;
			wifi.setWifiEnabled(false);
		}
		
		try {
			if (DEBUG) Log.d(TAG,"deleting "+ WIFIRecoveryActivity.wpa_file);
			RootTools.sendShell("rm "+ WIFIRecoveryActivity.wpa_file,WIFIRecoveryActivity.CMD_TIMEOUT);
			//RootTools.sendShell("rm "+ WIFIRecoveryActivity.wpa_file);

			Toast.makeText(getApplicationContext(), R.string.reset_done, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			if (DEBUG) Log.d(TAG,"error deleting file");
			Toast.makeText(getApplicationContext(), R.string.reset_error, Toast.LENGTH_SHORT).show();
			//e.printStackTrace();
		}
		
		//turn wifi on if previously turned off
		if (wifiEnabled){
			wifi.setWifiEnabled(true);
		}
		
		quitAndRefresh();
    }
    
    private void quitAndRefresh(){
    	Intent resultIntent = new Intent();
    	resultIntent.putExtra("refresh", true);
    	setResult(Activity.RESULT_OK, resultIntent);
    	finish(); //this line may be annoying
    }
	
}
