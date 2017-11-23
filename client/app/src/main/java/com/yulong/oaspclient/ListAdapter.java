package com.yulong.oaspclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
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

        private ViewHolder(LinearLayout layout) {
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
        final OASPVerify info = OASPInfos.get(position);
        info.icon.setBounds(0, 0, 100, 100);
        holder.title.setCompoundDrawables(info.icon, null, null, null);
        holder.title.setText(info.name);
        holder.body.setText("Package: " + info.pkg + "\n");
        holder.body.append("Version: " + info.version);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setBackgroundColor(Color.YELLOW);
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
                    String msg = "App: " + info.name + "\nPkg: " + info.pkg + "\nVer: " + info.version + "\n\n";
                    // Case 1: this is a good app
                    if (info.status == OASPStatus.OK) {
                        msg += "OASP Check PASSED!";
                    }
                    // Case 2: this is a bad app
                    else if (info.status == OASPStatus.BAD) {
                        msg += "OASP: This app is bad!";
                    }
                    // Case 3: the server cannot make a decision
                    else if (info.status == OASPStatus.UNKNOWN) {
                        msg += "OASP: The server cannot make a decision!";
                    }
                    // Case 4: the server told us to ask another server
                    // TODO: parse the returned URL and send request again. Pay attention to avoid loops
                    else if (info.status == OASPStatus.REDIRECT) {
                        msg += "OASP: The server told us to consult another server!";
                    }
                    // Case 5: the server told us that our query is invalid
                    else if (info.status == OASPStatus.INVALID) {
                        msg += "OASP: The server told us that the query is invalid!";
                    }
                    // Case 6: not properly OASP signed
                    else if (info.status == OASPStatus.CORRUPTED) {
                        msg += "OASP: This app supports OASP but not properly signed!";
                    }
                    // Case 7: the app does not support OASP at all
                    else if (info.status == OASPStatus.UNSUPPORTED) {
                        msg += "This app does not support OASP";
                    }
                    // Case 8: fail to contact the server
                    else if (info.status == OASPStatus.COMM_ERROR) {
                        msg += "OASP: Communication Error";
                    } else {
                        msg += "You shouldn't see me.";
                    }
                    builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
                    builder.create().show();
                } catch (Exception x) {
                    x.printStackTrace();
                }

                new CountDownTimer(500, 1000) {
                    @Override
                    public void onTick(long arg0) {
                    }

                    @Override
                    public void onFinish() {
                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                }.start();
            }

        });
    }

    @Override
    public int getItemCount() {
        return OASPInfos.size();
    }
}


