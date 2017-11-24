package com.baidu.oasp;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class OASPVerify {
    private static final String OASP_CERT = "META-INF-OASP/oasp.cert";
    private static final String OASP_OLDCERT_DIR = "META-INF-OASP/OLD/";
    private static final String OASP_URL = "META-INF-OASP/url.txt";

    private static final String OASP_SIGNATURE = "META-INF/oasp.sig";
    private static final String OASP_OLDSIG_DIR = "META-INF/OASP-OLD/";
    private static final String APK_MANIFEST = "META-INF/MANIFEST.MF";

    public OASPStatus status;   // OASP verification result

    public String name;         // App name
    public String pkg;          // App package name
    public String path;         // APK path
    public Drawable icon;       // App icon
    public String version;      // App version
    public int verCode;         // App version code
    public String apk_cert;         // APK signing certificates
    public String hash;         // APK whole file digest
    public String mf_hash;      // MANIFEST.MF file digest
    // Note: the reason we collect mf_hash is because APK's META-INF directory can have files
    //   modifed/added/deleted without breaking the APK signing verification; the ZIP
    //   archive structure can be re-arranged or re-aligned without breaking the APK signing
    //   verification as well. So once the APK is signed, the whole file digest may be varied.
    //   The META-INF/MANIFEST.MF file, however, is under signing thus cannot be changed.
    //   We can use the whole file digest to locate the unique sample in the wild, but use the
    //   MF digest to match the exact signed version. When we do post request to OASP server
    //   to lookup the app status, we use MF digest.

    public String oasp_cert;    // OASP certificate
    public String oasp_url;     // URL of OASP remote server (must be HTTPS)
    public ArrayList<String> oasp_url_certs;    // HTTPS certificates of the OASP server
    public ArrayList<String> oasp_old_certs;    // Old OASP certificates
    // Note: OASP supports (cross-version) certificate upgrade. Old OASP certificates should be
    //   included as OASP/OLD/xxx.cert, and the corresponding signatures should be included as
    //   META-INF/OASP-OLD/xxx.sig. Here xxx.cert and xxx.sig have the same prefix.


    public OASPVerify(PackageManager pm, ApplicationInfo app) {
        status = OASPStatus.UNSUPPORTED; // If sth unexpected happened, default to UNSUPPORTED
        try {
            //****************  Obtain basic app info  ****************
            name = app.loadLabel(pm).toString();
            pkg = app.packageName;
            icon = app.loadIcon(pm);
            path = app.sourceDir;
            PackageInfo pkgInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
            version = pkgInfo.versionName;
            verCode = pkgInfo.versionCode;


            //****************  Obtain the signing certificates  ****************
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            byte[] rawCert = pkgInfo.signatures[0].toByteArray();
            InputStream certStream = new ByteArrayInputStream(rawCert);
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);
            messageDigest.reset();
            messageDigest.update(cert.getEncoded());
            apk_cert = convertToHex(messageDigest.digest());


            //****************  Collect MF digest   ****************
            byte[] mf_bytes = readFile(APK_MANIFEST);
            messageDigest.reset();
            messageDigest.update(mf_bytes);
            mf_hash = convertToHex(messageDigest.digest());


            //****************  Collect and verify OASP certificate   ****************
            Signature sig = Signature.getInstance("SHA256withRSA");
            JarFile jarFile = new JarFile(path);
            JarEntry jarEntry = jarFile.getJarEntry(OASP_CERT);
            if (jarEntry == null) {
                Log.e("OASP", pkg + ": OASP certificate not found");
                status = OASPStatus.UNSUPPORTED;
                return;
            }
            cert = (X509Certificate) cf.generateCertificate(jarFile.getInputStream(jarEntry));
            try {
                cert.checkValidity();
                cert.verify(cert.getPublicKey());   // Assuming a self-signed certificate
            } catch (Exception e) {
                Log.e("OASP", pkg + ": OASP certificate invalid");
                e.printStackTrace();
                status = OASPStatus.CORRUPTED;
                return;
            }
            byte[] oasp_sig_bytes = readFile(OASP_SIGNATURE);
            if (oasp_sig_bytes == null) {
                Log.e("OASP", pkg + ": OASP signature not found");
                status = OASPStatus.UNSUPPORTED;
                return;
            }
            sig.initVerify(cert);
            sig.update(mf_bytes);
            if (!sig.verify(oasp_sig_bytes)) {
                Log.e("OASP", pkg + ": OASP signature cannot be verified");
                status = OASPStatus.CORRUPTED;
                return;
            }
            messageDigest.reset();
            messageDigest.update(cert.getEncoded());
            oasp_cert = convertToHex(messageDigest.digest());


            //****************  Collect and verify OASP URL   ****************
            jarEntry = jarFile.getJarEntry(OASP_URL);
            if (jarEntry == null) {
                Log.e("OASP", pkg + ": OASP URL not found");
                status = OASPStatus.UNSUPPORTED;
                return;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry)));
            String url_str = r.readLine();
            if (!url_str.startsWith("https://")) {
                Log.e("OASP", pkg + ": OASP URL is not in HTTPS");
                status = OASPStatus.UNSUPPORTED;
                return;
            }
            oasp_url = url_str;


            //****************  Collect and verify OASP old certificates   ****************
            oasp_old_certs = new ArrayList<>();
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                String f = entry.getName();
                if (f.startsWith(OASP_OLDSIG_DIR) && (f.split("/").length == 3) && f.endsWith(".sig")) {
                    String sigName = f.split("/")[2];
                    sigName = sigName.substring(0, sigName.length() - 4);

                    JarEntry certEntry = jarFile.getJarEntry(OASP_OLDCERT_DIR + sigName + ".cert");
                    if (certEntry == null)
                        throw new Exception("Failed to read " + OASP_OLDCERT_DIR + sigName + ".cert" + " from " + path);

                    cert = (X509Certificate) cf.generateCertificate(jarFile.getInputStream(certEntry));
                    try {
                        cert.checkValidity();
                        cert.verify(cert.getPublicKey());   // Assuming a self-signed certificate
                    } catch (Exception e) {
                        Log.e("OASP", pkg + ": OASP OLD certificate " + sigName + " invalid");
                        e.printStackTrace();
                        status = OASPStatus.CORRUPTED;
                        return;
                    }
                    sig.initVerify(cert);
                    sig.update(mf_bytes);
                    if (!sig.verify(readFile(f))) {
                        Log.e("OASP", pkg + ": OASP OLD signature " + sigName + " cannot be verified");
                        status = OASPStatus.CORRUPTED;
                        return;
                    }
                    messageDigest.reset();
                    messageDigest.update(cert.getEncoded());
                    oasp_old_certs.add(convertToHex(messageDigest.digest()));
                }
            }


            //****************  Collect OASP URL's HTTPS certificates   ****************
            oasp_url_certs = new ArrayList<>();
            if (!new GetHTTPSCerts().execute().get()) {
                Log.e("OASP", pkg + ": Failed to obtain HTTPS certs from URL " + oasp_url);
                status = OASPStatus.COMM_ERROR;
                return;
            }

            status = new OASPRemoteCheck().execute().get();

            //****************  APK whole file hash  ****************
            // We leave this in the very end because it's time consuming and we don't really
            //   include it in the request sent to the remote server. Antivirus vendors might be
            //   interested in it though.
            File file = new File(path);
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(buffer);
            fis.close();
            messageDigest.reset();
            messageDigest.update(buffer);
            hash = convertToHex(messageDigest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Convert byte array to hex string representation
    private String convertToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    // Obtain file content from an APK
    private byte[] readFile(String internalPath) throws Exception {
        JarFile jarFile = new JarFile(path);
        JarEntry jarEntry = jarFile.getJarEntry(internalPath);
        if (jarEntry == null)
            return null;

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buf = new byte[100 * 1024];
        int len;
        InputStream is = jarFile.getInputStream(jarEntry);
        while ((len = is.read(buf)) != -1) {
            byteBuffer.write(buf, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // Collect URL's HTTPS certificates
    private class GetHTTPSCerts extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // TODO: support self-defined ports instead of 443 only
                SocketFactory factory = SSLSocketFactory.getDefault();
                SSLSocket socket = (SSLSocket) factory.createSocket(oasp_url.substring(8), 443);
                socket.startHandshake();
                Certificate[] certs = socket.getSession().getPeerCertificates();
                if (certs == null)
                    return false;

                for (Certificate cert : certs) {
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
                    messageDigest.update(cert.getEncoded());
                    oasp_url_certs.add(convertToHex(messageDigest.digest()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

    }

	// Query OASP server for app status
    private class OASPRemoteCheck extends AsyncTask<Void, Void, OASPStatus> {
        @Override
        protected OASPStatus doInBackground(Void... params) {
            try {
                // Now post the required info to the OASP server
                JSONObject data = new JSONObject();
                data.put("pkg", pkg);
                data.put("ver", verCode);
                data.put("mf_hash", mf_hash);
                data.put("apk_cert", apk_cert);
                data.put("oasp_cert", oasp_cert);

                URL url = new URL(oasp_url);
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
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                JSONObject rst = new JSONObject(result.toString());
                in.close();
                conn.disconnect();

                int status = (int) rst.get("oasp_result");
                if (OASPStatus.contains(status)) {
                    return OASPStatus.fromInt(status);
                } else {
                    return OASPStatus.COMM_ERROR;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return OASPStatus.COMM_ERROR;
            }
        }

    }


}
