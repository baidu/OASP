package com.yulong.oaspclient;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private ArrayList<com.yulong.oaspclient.AppInfo> appInfos;
    private Context cxt;

    public ListAdapter(ArrayList<com.yulong.oaspclient.AppInfo> data, Context c) {
        appInfos = data;
        cxt = c;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView body;

        public ViewHolder(LinearLayout layout) {
            super(layout);
            title = (TextView) layout.findViewById(R.id.title);
            body = (TextView) layout.findViewById(R.id.body);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        LinearLayout l = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item, parent, false);
        return new ViewHolder(l);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final com.yulong.oaspclient.AppInfo info = appInfos.get(position);
        info.icon.setBounds(0, 0, 100, 100);
        holder.title.setCompoundDrawables(info.icon, null, null, null);
        holder.title.setText(info.name);
        holder.body.append("Package: " + info.pkg + "\n");
        holder.body.append("Version: " + info.version);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.getDetails();
                new com.yulong.oaspclient.OASPCheck(cxt).execute(info);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appInfos.size();
    }
}


