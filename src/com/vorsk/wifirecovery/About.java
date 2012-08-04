package com.vorsk.wifirecovery;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class About extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		//set the button listener!
		View aboutBackbutton = findViewById(R.id.about_back_button);
		aboutBackbutton.setOnClickListener(this);
		
		//set the about verson
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
		case R.id.about_back_button:
			finish(); //go back to the main activity1
			break;
		default:
			break;
		}
	}
	
}
