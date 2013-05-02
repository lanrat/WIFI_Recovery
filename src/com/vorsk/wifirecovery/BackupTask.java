package com.vorsk.wifirecovery;

import java.io.File;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class BackupTask extends AsyncTask<Integer, Void, Void>{
	private static final String TAG = "WIFI_Recovery BackupTask";
	private static final boolean DEBUG = false;
	
	private Activity myActivity;
	ProgressDialog dialog;
	
	private int errorCode;
	private int toastError;
	private static final int ERROR_NO_ROOT = 1;
	private static final int ERROR_TOAST = 2;
	private static final int ACTION_BACKUP = 1;
	private static final int ACTION_RESTORE = 2;
	private static final int ACTION_RESET = 3;
	
	//ctor
	private BackupTask(Activity activity){
		this.myActivity = activity;
	}
	
	//factory methods
	public static void backupNetworks(Activity activity)
	{	
		(new BackupTask(activity)).execute(ACTION_BACKUP);
	}
	public static void restoreNetworks(Activity activity)
	{	
		(new BackupTask(activity)).execute(ACTION_RESTORE);
	}
	public static void resetNetworks(Activity activity)
	{	
		(new BackupTask(activity)).execute(ACTION_RESET);
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
	protected Void doInBackground(Integer... args) {
		if (!this.checkRoot()){
			this.errorCode = ERROR_NO_ROOT;
			return null;
		}
		String file_name = getConfigFile();
		if (file_name == null)
		{
			this.errorCode = ERROR_TOAST;
			this.toastError = R.string.find_error;
			return null;
		}
		
		switch (args[0]) {
		case ACTION_BACKUP:
			this.backup(file_name);
			break;
		case ACTION_RESTORE:
			this.restoreCopy(file_name);
			break;
		case ACTION_RESET:
			this.reset(file_name);
			break;
		default:
			break;
		}
		
		return null;
	}
	
	private void backup(String filename){
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ){
			String location =  Environment.getExternalStorageDirectory().getPath() +"/"+ this.myActivity.getString(R.string.backup_file_name);
			try {
				if (DEBUG) Log.d(TAG,"copying to: "+location);
				RootTools.copyFile(filename, location, false, false);
			} catch (Exception e) {
				if (DEBUG) Log.d(TAG,"can't backup file");
				this.errorCode = ERROR_TOAST;
				this.toastError = R.string.backup_error;
			}
		} else {
			//error!
			this.errorCode = ERROR_TOAST;
			this.toastError = R.string.sd_error;
		}
	}
	
	private void restoreCopy(String filename){
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
			String location =  Environment.getExternalStorageDirectory().getPath() +"/"+ this.myActivity.getString(R.string.backup_file_name);
			if (DEBUG) Log.d(TAG,"checking backup exists at: "+location);

			if (new File(location).isFile()){
				//TODO do a test parse!
 
				WifiManager wifi = (WifiManager)this.myActivity.getSystemService(Context.WIFI_SERVICE);
				//get wifi state, if off turn on
				boolean wifiEnabled = false;
				if (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
						wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING ||
						wifi.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN){
					wifiEnabled = true;
					wifi.setWifiEnabled(false);
				}
				
				//copy file and set permissions to 0660
				if (DEBUG) Log.d(TAG,"copying file from: "+location);
				RootTools.copyFile(location, filename, true, false);
				
				CommandCapture chown = new CommandCapture(0,"chown system:wifi "+filename);
				CommandCapture chmod = new CommandCapture(0,"chmod 0660 "+filename);
				try {
					RootTools.getShell(true).add(chown).waitForFinish();
					RootTools.getShell(true).add(chmod).waitForFinish();
				} catch (Exception e) {
					if (DEBUG) Log.d(TAG,"unable to update permissions of: "+filename );
					this.errorCode = ERROR_TOAST;
					this.toastError = R.string.restore_error;
				}
				
				//turn wifi on if previously turned off
				if (wifiEnabled){
					wifi.setWifiEnabled(true);
					wifi.startScan(); //reconnect
				}
			
			}else{
				this.errorCode = ERROR_TOAST;
				this.toastError = R.string.no_backup_file;
			}
		} else {
			this.errorCode = ERROR_TOAST;
			this.toastError = R.string.sd_error;
		}
	}
	
    private void reset(String filename){
		WifiManager wifi = (WifiManager)this.myActivity.getSystemService(Context.WIFI_SERVICE);
		//get wifi state, if off turn on
		boolean wifiEnabled = false;
		if (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
				wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING ||
				wifi.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN){
			wifiEnabled = true;
			wifi.setWifiEnabled(false);
		}
		
		try {
			if (DEBUG) Log.d(TAG,"deleting "+ filename);
			(new RootTools()).deleteFileOrDirectory(filename, false);
		} catch (Exception e) {
			if (DEBUG) Log.d(TAG,"error deleting file");
			this.errorCode = ERROR_TOAST;
			this.toastError = R.string.reset_error;
		}
		
		//turn wifi on if previously turned off
		if (wifiEnabled){
			wifi.setWifiEnabled(true);
		}
    }
	
	@Override
	protected void onPostExecute(Void result) {
		//close loading message
		try {
			dialog.dismiss();
		} catch (Exception e) {
			//do nothing
		}
		
		if (result == null){
			if (this.errorCode == ERROR_TOAST){
				Toast.makeText(myActivity, this.toastError, Toast.LENGTH_SHORT).show();
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
	}
	
	private boolean checkRoot() {
		if (DEBUG) Log.d(TAG, "testing for root");
		if (RootTools.isAccessGiven()) {
			if (DEBUG) Log.d(TAG, "We have root!");
			return true;
		}
		if (DEBUG) Log.d(TAG, "Root Failure");
		return false;
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

}
