package com.yulong.oaspclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.framework.util.jar.JarEntry;
import android.framework.util.jar.JarFile;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Enumeration;

class OASPCheck extends AsyncTask<com.yulong.oaspclient.AppInfo, String, OASPStatus> {
    private Context cxt;
    private com.yulong.oaspclient.AppInfo info;

    public OASPCheck(Context c) {
        cxt = c;
    }

    @Override
    protected OASPStatus doInBackground(com.yulong.oaspclient.AppInfo... params) {
        info = params[0];   // App info is saved to be later used in onPostExecute

        // If the app does not support IDSIG at all
        if (info.oaspUrl == null) {
            return OASPStatus.UNSUPPORTED;
        }

        // If it has IDSIG but incorrectly signed, the check fails
        if (idsigVerify(info.path) == false) {
            return OASPStatus.CORRUPTED;
        }

        // If it has IDSIG_OLD but incorrectly signed, the check fails too.
        // This prevents an attacker to update an App signed by IDSIG_OLD to one signed by IDSIG
        //     without actually being the owner of IDSIG_OLD.
        if (info.hasIdsigOld && !idsigOldVerify(info.path)) {
            return OASPStatus.CORRUPTED;
        }

        try {
            // Now post the required info to the OASP server
            JSONObject data = new JSONObject();
            data.put("pkg", info.pkg);
            data.put("ver", info.verCode);
            data.put("hash", info.hash);
            data.put("cert", info.cert);
            data.put("idsig", info.idsig);

            URL url = new URL(info.oaspUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data.toString());
            wr.close();

            // Get the feedback from the OASP server
            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            JSONObject rst = new JSONObject(result.toString());
            in.close();
            conn.disconnect();

            // onPostExecute will consume the returned status
            int status = (int) rst.get("oasp_result");
            if (OASPStatus.contains(status)) {
                return OASPStatus.fromInt(status);
            } else {
                return OASPStatus.COMM_ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If there's any exception during the online check, we deem it as communication error
        return OASPStatus.COMM_ERROR;
    }


    @Override
    protected void onPostExecute(OASPStatus result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        String msg = "App: " + info.name + "\nPkg: " + info.pkg + "\nVer: " + info.version + "\n\n";
        // Case 1: this is a good app
        if (result == OASPStatus.OK) {
            msg += "OASP Check PASSED!";
        }
        // Case 2: this is a bad app
        else if (result == OASPStatus.BAD) {
            msg += "OASP: This app is bad!";
        }
        // Case 3: the server cannot make a decision
        else if (result == OASPStatus.UNKNOWN) {
            msg += "OASP: The server cannot make a decision!";
        }
        // Case 4: the server told us to ask another server
        // TODO: parse the returned URL and send request again. Pay attention to avoid loops
        else if (result == OASPStatus.REDIRECT) {
            msg += "OASP: The server told us to consult another server!";
        }
        // Case 5: the server told us that our query is invalid
        else if (result == OASPStatus.INVALID) {
            msg += "OASP: The server told us that the query is invalid!";
        }
        // Case 6: the IDSIG is not properly signed
        else if (result == OASPStatus.CORRUPTED) {
            msg += "OASP: This app has IDSIG but not properly signed!";
        }
        // Case 7: the app has no IDSIG at all
        else if (result == OASPStatus.UNSUPPORTED) {
            msg += "OASP: This app does not support OASP";
        }
        // Case 8: fail to contact the server
        else if (result == OASPStatus.COMM_ERROR) {
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
    }

    // Verify the IDSIG signature, similar to how Android verifies apk signatures
    private boolean idsigVerify(String file) {
        try {
            JarFile.setMetaDir("IDSIG/");
            JarFile jarFile = new JarFile(file, true, true);
            // JarFile(String filename, boolean verify, boolean chainCheck)

            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith("META-INF/") && !entryName.startsWith("IDSIG/")) {
                    InputStream is = jarFile.getInputStream(entry);
                    // Skip bytes because we have to read the entire file for it to read signatures
                    long toSkip = entry.getSize();
                    while (toSkip > 0) {
                        long skipped = is.skip(toSkip);
                        toSkip -= skipped;
                    }
                    is.close();
                    Certificate[] certs = entry.getCertificates();
                    if (certs == null || certs.length == 0) {
                        Log.e("OASP", "Failed to verify IDSIG for " + entryName);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("OASP", "Exception in verifying IDSIG for " + file);
            e.printStackTrace();
            return false;
        }
    }

    // Verify the IDSIG_OLD signatures, similar to idsigVerify
    private boolean idsigOldVerify(String file) {
        try {
            JarFile.setMetaDir("IDSIG_OLD/");
            JarFile jarFile = new JarFile(file, true, true);
            // JarFile(String filename, boolean verify, boolean chainCheck)

            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String entryName = entry.getName();
                if (!entryName.startsWith("META-INF/") && !entryName.startsWith("IDSIG/")
                        && !entryName.startsWith("IDSIG_OLD/")) {
                    InputStream is = jarFile.getInputStream(entry);
                    // Skip bytes because we have to read the entire file for it to read signatures
                    long toSkip = entry.getSize();
                    while (toSkip > 0) {
                        long skipped = is.skip(toSkip);
                        toSkip -= skipped;
                    }
                    is.close();
                    Certificate[] certs = entry.getCertificates();
                    if (certs == null || certs.length == 0) {
                        Log.e("OASP", "Failed to verify IDSIG_OLD for " + entryName);
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("OASP", "Exception in verifying IDSIG_OLD for " + file);
            e.printStackTrace();
            return false;
        }
    }
}
