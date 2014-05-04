/*
 * tata inc. 
 * Copyright (c) 2011-2014 All Rights Reserved.
 */
package com.example.stopwatch;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


/**
 * <p>TODO</p>
 *
 * @author <a href="mailto:yangkaifeng1985@126.com">Kaifeng Yang</a>
 * @version $Id: LapRecordAdapter.java, v0.1 Apr 24, 2014 8:27:21 PM, Kaifeng Yang Exp $
 */
public class LapRecordAdapter extends BaseAdapter {
	private Context context;
	private List<LapRecord> lapRecordList = new ArrayList<LapRecord>();
	
	public LapRecordAdapter (Context context) {
		this.context = context;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return lapRecordList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return lapRecordList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		//if (null == convertView) {
			LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
			convertView = layoutInflater.inflate(R.layout.lap_record_list, null);
			TextView lapNOText = (TextView)convertView.findViewById(R.id.lapNO);
			TextView lapTimeText = (TextView)convertView.findViewById(R.id.lapTime);
			TextView deltaTimeText = (TextView)convertView.findViewById(R.id.deltaTime);
		//}
		
		lapNOText.setText(lapRecordList.get(position).lapNO);
		lapTimeText.setText(lapRecordList.get(position).lapTime);
		deltaTimeText.setText(lapRecordList.get(position).deltaTime);
		
		return convertView;
	}
	
	public void addLapRecord(LapRecord lapRecord) {
		lapRecordList.add(0, lapRecord);
		notifyDataSetChanged();
	}
	
	public void clearAll() {
		lapRecordList.clear();
		notifyDataSetChanged();
	}

}
