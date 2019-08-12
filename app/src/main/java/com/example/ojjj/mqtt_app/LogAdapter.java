package com.example.ojjj.mqtt_app;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

class LogAdapter extends BaseAdapter implements Serializable{
    private  static transient LayoutInflater inflater;
    private static transient ArrayList<String> loglist, logtime,logtopic;
    public LogAdapter(Activity activity, ArrayList<String> data, ArrayList<String> time,ArrayList<String> topic){
        inflater = activity.getLayoutInflater();
        loglist = data;
        logtime = time;
        logtopic = topic;
    }
    //데이터가 변화했을때 == 데이터가 왔을때
    public void notifyDataChanged(ArrayList<String> data, ArrayList<String> time,ArrayList<String> topic){
        loglist = data;
        logtime = time;
        logtopic = topic;

        this.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return loglist.size();
    }
    @Override
    public Object getItem(int position) {
        return null;
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }
    @Override
    public View getView(int position, View v, ViewGroup parent) {
        if(v == null){
            v = inflater.inflate(R.layout.list_inner, null);
        }

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });


        TextView tv1, tv2,tv3;
        tv1 = v.findViewById(R.id.when);
        tv2 = v.findViewById(R.id.who);
        tv3 = v.findViewById(R.id.what);
        tv1.setText(logtime.get(position));
        tv2.setText(logtopic.get(position));
        tv3.setText(loglist.get(position));
        return v;
    }
}
class topicAdapter extends BaseAdapter {
    private ArrayList<String>  topicList;
    private ArrayList<String> newtopic;
    private LayoutInflater inflater;
    public topicAdapter(Activity activity, ArrayList<String> topicList,ArrayList<String> newtopic){
        inflater = activity.getLayoutInflater(); this.topicList = topicList;this.newtopic = newtopic;}
    @Override
    public int getCount() {
        return topicList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = inflater.inflate(R.layout.list_inner_topics,null);
        }


        TextView tv = view.findViewById(R.id.topic);
        TextView ntv = view.findViewById(R.id.check);
        tv.setText(topicList.get(i).toString());
        ntv.setText(newtopic.get(i).toString());
        return view;
    }
}