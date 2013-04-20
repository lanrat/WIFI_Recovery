package com.vorsk.wifirecovery;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import android.os.Bundle;
import android.provider.Settings;

//for logging
import android.util.Log;
//list stuff
//import android.app.Activity;
import android.app.AlertDialog;
//import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.ListView;

//do I need this?
import android.view.View;

//extend ListActivity in place of activity
public class WIFIRecoveryActivity extends SherlockListActivity {
	private static final String TAG = "WIFI_Recovery Activity";
	private static final boolean DEBUG = true;

	private static final int REFRESH = 5; // why not?
	public static final int CMD_TIMEOUT = 2000; // timeout for cmd commands, 2
												// seconds
	public static String wpa_file;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG)
			Log.d(TAG, "onCreate");
		// use my custom list with buttons and empty list support
		setContentView(R.layout.list);
		
		setTitle(R.string.home_title);


		// start up the parser
		Parser parser = new Parser(this);
		parser.execute();
	}

	

	// this is deprecated, but I'm using it anyways
	/*
	 * @Override public Object onRetainNonConfigurationInstance(){ return
	 * this.networks; }
	 */

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (DEBUG)
			Log.d(TAG, "clicked!");
		Network network = (Network) getListAdapter().getItem(position);
		if (DEBUG)
			Log.d(TAG, "got network");

		infoPopUp(network).show();
	}

	public AlertDialog infoPopUp(final Network network) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(network.getDetails())
				.setCancelable(true)
				.setNegativeButton(R.string.button_dismiss,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss(); // or cancel?, who knows, it
													// works.
							}
						})
				.setPositiveButton(R.string.button_share,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// does this share the data?
								Intent share = new Intent(
										android.content.Intent.ACTION_SEND);
								share.setType("text/plain");
								share.putExtra(
										android.content.Intent.EXTRA_SUBJECT,
										"Wireless Settings");
								share.putExtra(
										android.content.Intent.EXTRA_TEXT,
										network.getDetails());
								startActivity(share);

							}
						});

		AlertDialog alert = builder.create();

		return alert;
	}


	// TODO REDO
	private void refresh() {
		this.onCreate(null);
	}

	// make the menu work
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		//actionbar menu
		getSupportMenuInflater().inflate(R.menu.home_action, menu);
		
		return true;
	}

	// when a user selects a menu item
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_backup:
			startActivityForResult(new Intent(this, Backup.class),
					WIFIRecoveryActivity.REFRESH);
			return true;
		case R.id.menu_refresh:
			// refresh the networks (the easy way)
			this.refresh();
			return true;
		case R.id.menu_about:
			// about box here
			startActivity(new Intent(this, About.class));
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			return true;
			// possibly add more menu items here
		default:
			return false;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// networks changed, refresh
		if (requestCode == WIFIRecoveryActivity.REFRESH
				&& resultCode == SherlockListActivity.RESULT_OK) {
			if (DEBUG)
				Log.d(TAG, "got back the activity");
			if (data.getBooleanExtra("refresh", false)) {
				if (DEBUG)
					Log.d(TAG, "updating networks");
				this.refresh();
			}
		}
	}


}