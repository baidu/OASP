package com.yulong.oaspclient;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the list view to show all the installed apps
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.main_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Collect all the apps' info
        ArrayList appList = new ArrayList();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        int isSysApp = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        for (ApplicationInfo app : packages) {
            // Ignore system applications
            if ((app.flags & isSysApp) == 0) {
                appList.add(new com.yulong.oaspclient.AppInfo(pm, app));
            }
        }

        // Sort app list based on the package name
        Collections.sort(appList, new Comparator<com.yulong.oaspclient.AppInfo>() {
            @Override
            public int compare(com.yulong.oaspclient.AppInfo app1, com.yulong.oaspclient.AppInfo app2) {
                return app1.pkg.compareTo(app2.pkg);
            }
        });

        // Show it
        RecyclerView.Adapter mAdapter = new ListAdapter(appList, this);
        mRecyclerView.setAdapter(mAdapter);
    }
}


