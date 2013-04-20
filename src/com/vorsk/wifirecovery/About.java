package com.vorsk.wifirecovery;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class About extends SherlockActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		setTitle(R.string.about_title);
		

		//set the about version
		TextView version = (TextView) findViewById(R.id.about_version);
		try {
			version.append(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			version.append("Unknown");
		}
	}
	
	//hook the button up!
	public void onClick(View v){
		switch (v.getId()) {
		case R.id.abs__action_bar:
			finish(); //go back to the main activity
			break;
		default:
			break;
		}
	}
	
}
