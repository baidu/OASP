package com.yulong.oaspclient;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.oasp.OASPStatus;
import com.baidu.oasp.OASPVerify;

import java.util.ArrayList;

class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private ArrayList<OASPVerify> OASPInfos;
    private Context cxt;

    public ListAdapter(ArrayList<OASPVerify> data, Context c) {
        OASPInfos = data;
        cxt = c;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView body;
        private TextView status;

        private ViewHolder(LinearLayout layout) {
            super(layout);
            title = (TextView) layout.findViewById(R.id.title);
            body = (TextView) layout.findViewById(R.id.body);
            status = (TextView) layout.findViewById(R.id.status);
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
        final OASPVerify info = OASPInfos.get(position);
        info.icon.setBounds(0, 0, 100, 100);
        holder.title.setCompoundDrawables(info.icon, null, null, null);
        holder.title.setText(info.name);
        holder.body.setText("Package: " + info.pkg + "\n");
        holder.body.append("Version: " + info.version);

        TextView tv = holder.status;
        // Case 1: this is a good app
        if (info.status == OASPStatus.OK) {
            tv.setText("[√] OASP Check PASSED!");
            tv.setTextColor(Color.GREEN);
        }
        // Case 2: this is a bad app
        else if (info.status == OASPStatus.BAD) {
            tv.setText("[X] OASP: This app is bad!");
            tv.setTextColor(Color.RED);
        }
        // Case 3: the server cannot make a decision
        else if (info.status == OASPStatus.UNKNOWN) {
            tv.setText("OASP server cannot make a decision!");
            tv.setTextColor(Color.YELLOW);
        }
        // Case 4: the server told us to ask another server
        // TODO: parse the returned URL and send request again. Pay attention to avoid loops
        else if (info.status == OASPStatus.REDIRECT) {
            tv.setText("OASP: The server told us to consult another server!");
            tv.setTextColor(Color.YELLOW);
        }
        // Case 5: the server told us that our query is invalid
        else if (info.status == OASPStatus.INVALID) {
            tv.setText("OASP: The server told us that the query is invalid!");
            tv.setTextColor(Color.MAGENTA);
        }
        // Case 6: not properly OASP signed
        else if (info.status == OASPStatus.CORRUPTED) {
            tv.setText("[X] This app supports OASP but is corrupted!");
            tv.setTextColor(Color.RED);
        }
        // Case 7: the app does not support OASP at all
        else if (info.status == OASPStatus.UNSUPPORTED) {
            tv.setText("This app does not support OASP");
        }
        // Case 8: fail to contact the server
        else if (info.status == OASPStatus.COMM_ERROR) {
            tv.setText("[-] It supports OASP but communication to its OASP url failed");
            tv.setTextColor(Color.MAGENTA);
        } else {
            tv.setText("You shouldn't see me.");
        }
    }

    @Override
    public int getItemCount() {
        return OASPInfos.size();
    }
}


