package com.amd.radio;

import java.util.ArrayList;
import java.util.Arrays;

import com.haoke.mediaservice.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Radio_favorite_gridview_adapter extends BaseAdapter {
	
	ArrayList<RadioStation> mRadioStationList ;
	Context mContext ;
	boolean selectedList[];
	boolean editMode = false;
	
	public boolean[] getSelectedList() {
		return selectedList;
	}

	public Radio_favorite_gridview_adapter(ArrayList<RadioStation> list , Context mContext) {
		this.mRadioStationList = list ;
		this.mContext = mContext ;
		selectedList = new boolean[mRadioStationList.size()];
		Arrays.fill(selectedList, false);
	}
	
	/*
	 * 若返回值为true，则表示为编辑模式
	 */
	public boolean changeSelected(int positon){
		if (editMode) {
			selectedList[positon] = !selectedList[positon];
			notifyDataSetChanged();
			return true;
		}
		return false;
    }

	@Override
	public int getCount() {
		return mRadioStationList.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null ;
		if(convertView == null){
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(R.layout.radio_favorite_gridview_item, null);
			holder.select = (ImageView) convertView.findViewById(R.id.radio_grid_item_select);
			holder.freq = (TextView) convertView.findViewById(R.id.radio_favorite_item_tv_hz);
			holder.freq_name = (TextView) convertView.findViewById(R.id.radio_favorite_item_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		RadioStation station = mRadioStationList.get(position);
		holder.freq.setText(station.getSfreq());
		holder.freq_name.setText(station.getStationName());
		if (editMode) {
			if(selectedList[position]){
				convertView.setBackgroundResource(R.drawable.bac_list_on);  //选中项背景  
				holder.freq.setTextColor(Color.parseColor("#55AFFE"));
				holder.select.setImageResource(R.drawable.radio_selected_icon);
		    }else{  
		     	holder.freq.setTextColor(Color.parseColor("#FFFFFF"));
		       	convertView.setBackgroundResource(R.drawable.bac_list_normal);
		       	holder.select.setImageResource(R.drawable.radio_selected_nomal);
		    }
			holder.select.setVisibility(View.VISIBLE);
		} else {
			holder.freq.setTextColor(Color.parseColor("#FFFFFF"));
	       	convertView.setBackgroundResource(R.drawable.bac_list_normal);
			holder.select.setVisibility(View.INVISIBLE);
		}

		return convertView;
	}
	
	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mRadioStationList.size() == 0 ? null : mRadioStationList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	class ViewHolder {
		ImageView select;
		TextView freq ;
		TextView freq_name ;
	}

	public void enterEditMode() {
		if (!editMode) {
			editMode = true;
			Arrays.fill(selectedList, false);
			notifyDataSetChanged();
		}
	}
	
	public void exitEditMode() {
		if (editMode) {
			editMode = false;
			Arrays.fill(selectedList, false);
			notifyDataSetChanged();
		}
	}
	
	public void selectAll() {
		if(editMode) {
			Arrays.fill(selectedList, true);
			notifyDataSetChanged();
		}
	}
	public void unSelectAll() {
		if(editMode) {
			Arrays.fill(selectedList, false);
			notifyDataSetChanged();
		}
	}
	
	public ArrayList<RadioStation> deleteSelected() {
		ArrayList<RadioStation> list = new ArrayList<RadioStation>();
		if (editMode) {
			for(int i=selectedList.length-1; i>=0; i--) {
				if (selectedList[i]) {
					list.add(mRadioStationList.remove(i));
				}
			}
			selectedList = new boolean[mRadioStationList.size()];
			Arrays.fill(selectedList, false);
			notifyDataSetChanged();
		}
		return list;
	}
}
