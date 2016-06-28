package com.uplynk.sampleplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.uplynk.media.MediaPlayer.UplynkTrackInfo;

import java.util.List;

public class TrackAdapter extends ArrayAdapter<UplynkTrackInfo> {
    public TrackAdapter(Context context, int textViewResourceId, List<UplynkTrackInfo> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public UplynkTrackInfo getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ((TextView) view).setTextColor(Color.BLACK);
        return view;
    }
}
