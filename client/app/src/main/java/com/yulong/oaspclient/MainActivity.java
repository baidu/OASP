package com.yulong.oaspclient;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.baidu.oasp.OASPVerify;

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
        RecyclerView mRecyclerView = findViewById(R.id.main_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Collect all the apps' info
        ArrayList<OASPVerify> appList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        int isSysApp = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        for (ApplicationInfo app : packages) {
            // Ignore system applications
            if ((app.flags & isSysApp) == 0) {
                OASPVerify oaspInfo = new OASPVerify(pm, app);
                dump(oaspInfo);
                appList.add(oaspInfo);
            }
        }

        // Sort app list based on the package name
        Collections.sort(appList, new Comparator<OASPVerify>() {
            @Override
            public int compare(OASPVerify app1, OASPVerify app2) {
                return app1.pkg.compareTo(app2.pkg);
            }
        });

        // Show it
        RecyclerView.Adapter mAdapter = new ListAdapter(appList, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void dump(OASPVerify oaspInfo) {
        Log.e("OASP", "========================================================");
        Log.e("OASP", "App: " + oaspInfo.name);
        Log.e("OASP", "Status: " + oaspInfo.status.name());

        Log.e("OASP", "Package: " + oaspInfo.pkg);
        Log.e("OASP", "Path: " + oaspInfo.path);
        Log.e("OASP", "Version: " + oaspInfo.version);
        Log.e("OASP", "Version Code: " + oaspInfo.verCode);
        Log.e("OASP", "Signing Cert: " + oaspInfo.apk_cert);
        Log.e("OASP", "MF hash: " + oaspInfo.mf_hash);
        Log.e("OASP", "OASP Cert: " + oaspInfo.oasp_cert);
        Log.e("OASP", "OASP URL: " + oaspInfo.oasp_url);
        Log.e("OASP", "APK hash: " + oaspInfo.hash);

        if (oaspInfo.oasp_url_certs != null) {
            Log.e("OASP", "OASP URL Certs:");
            for (String cert : oaspInfo.oasp_url_certs) {
                Log.e("OASP", "    " + cert);
            }
        }
        if (oaspInfo.oasp_old_certs != null) {
            Log.e("OASP", "OASP Old Certs:");
            for (String cert : oaspInfo.oasp_old_certs) {
                Log.e("OASP", "    " + cert);
            }
        }
        Log.e("OASP", "========================================================");
    }
}
