package com.vorsk.wifirecovery;

import com.vorsk.wifirecovery.network.Network;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NetworkArrayAdapter extends ArrayAdapter<Network>{

	private final Context context;
	private final Network[] values;

	public NetworkArrayAdapter(Context context, Network[] values) {
		super(context, R.layout.rowlayout, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		
		TextView textView = (TextView) rowView.findViewById(R.id.name);
		TextView securityText = (TextView) rowView.findViewById(R.id.security);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		
		textView.setText(values[position].getSSID()); //set the text
		imageView.setImageResource(values[position].getIcon()); //set the image
		
		securityText.setText(values[position].getSecurityName());

		return rowView;
	}
	
}
