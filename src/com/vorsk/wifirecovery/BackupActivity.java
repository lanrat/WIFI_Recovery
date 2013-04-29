package com.vorsk.wifirecovery;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.vorsk.wifirecovery.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

public class BackupActivity extends SherlockActivity implements OnClickListener {
	private static final String TAG = "WIFI_Recovery Backup";
	private static final boolean DEBUG = false;
	private boolean refresh = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backup);
		setTitle(R.string.backup_title);
		
		//set the button listeners! condensed version
		findViewById(R.id.backup_button).setOnClickListener(this);
		findViewById(R.id.restore_button).setOnClickListener(this);
		findViewById(R.id.reset_button).setOnClickListener(this);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		overridePendingTransition(R.anim.slide_in, R.anim.slide_out);

	}
	
	//hook the button up!
	public void onClick(View v){
		switch (v.getId()) {
		case R.id.backup_button:
			BackupTask.backupNetworks(this);
			break;
		case R.id.restore_button:
			this.refresh = true;
			BackupTask.restoreNetworks(this);
			break;
		case R.id.reset_button:
			this.refresh = true;
			this.resetConfirm();
			break;
		default:
			break;
		}
	}
	
	//make the menu work
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.backup_action, menu);
		return true;
	}
    
    //when a user selects a menu item
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()) {
		case R.id.menu_about:
			//about box here
			HomeActivity.showAboutView(this);
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			return true;
		case android.R.id.home:
			finishAndRefresh();
			return true;
		//possibly add more menu items here
		default:
			return false;
		}
    }
    
    private void resetConfirm(){
    	final Activity activity = this;
    	new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.reset_title)
        .setMessage(R.string.reset_message)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	BackupTask.resetNetworks(activity);
            }
        })
        .setNegativeButton(R.string.no, null)
        .show();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
        	finishAndRefresh();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void finishAndRefresh(){
    	Intent resultIntent = new Intent();
    	if (refresh)
    	{
    		resultIntent.putExtra("refresh", true);
    	}
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	setResult(SherlockActivity.RESULT_OK, resultIntent);
    	finish();
    }
	
}
