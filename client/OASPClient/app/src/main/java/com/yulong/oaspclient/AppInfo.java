package com.yulong.oaspclient;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERTaggedObject;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.x509.extension.X509ExtensionUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class AppInfo {
    String name;
    String pkg;
    String path;
    Drawable icon;
    String version;
    int verCode;
    String hash;
    String cert;

    boolean hasIdsigOld;
    String idsig;
    String oaspUrl;

    private PackageInfo pkgInfo;

    /* Only collect basic info for all apps, to speed up launch */
    public AppInfo(PackageManager pm, ApplicationInfo app) {
        try {
            this.name = app.loadLabel(pm).toString();
            this.pkg = app.packageName;
            this.icon = app.loadIcon(pm);
            this.path = app.sourceDir;
            pkgInfo = pm.getPackageInfo(this.pkg, PackageManager.GET_SIGNATURES);
            this.version = pkgInfo.versionName;
            this.verCode = pkgInfo.versionCode;
            this.hasIdsigOld = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Collect detailed info if a certain app is selected */
    public void getDetails() {
        try {
            this.hash = getSHA256(this.path);

            Signature sig = pkgInfo.signatures[0];
            byte[] rawCert = sig.toByteArray();
            InputStream certStream = new ByteArrayInputStream(rawCert);
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(x509Cert.getEncoded());
            byte[] hash = messageDigest.digest();
            this.cert = convertToHex(hash);
            this.getIDSIG();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Convert byte array to hex string representation
    private String convertToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            hex.append(String.format("%02X", hash[i]));
        }
        return hex.toString();
    }

    // Calculate the SHA256 digest of a file
    private String getSHA256(String path) {
        try {
            File file = new File(path);
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(buffer);
            fis.close();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(buffer);
            byte[] hash = messageDigest.digest();
            return convertToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Obtain the OCSP/OASP URI from the certificate
    private String getOcspUrlFromCertificate(X509Certificate cert) {
        byte[] extensionValue = cert.getExtensionValue("1.3.6.1.5.5.7.1.1"); // Authority Information Access

        try {
            ASN1Sequence asn1Seq = (ASN1Sequence) X509ExtensionUtil.fromExtensionValue(extensionValue);
            Enumeration<?> objects = asn1Seq.getObjects();

            while (objects.hasMoreElements()) {
                ASN1Sequence obj = (ASN1Sequence) objects.nextElement(); // AccessDescription
                ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) obj.getObjectAt(0); // AccessMethod
                DERTaggedObject location = (DERTaggedObject) obj.getObjectAt(1); // AccessLocation

                if (location.getTagNo() == GeneralName.uniformResourceIdentifier) {
                    DEROctetString uri = (DEROctetString) location.getObject();
                    String str = new String(uri.getOctets());
                    if (oid.equals(X509ObjectIdentifiers.id_ad_ocsp)) {
                        return str;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Obtain the IDSIG fingerprint
    public void getIDSIG() {
        try {
            ZipFile zip = new ZipFile(path);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.getName().equals("IDSIG/ID.RSA")) {
                    InputStream is = zip.getInputStream(ze);
                    CertificateFactory cf = CertificateFactory.getInstance("X509");
                    X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(is);

                    this.oaspUrl = getOcspUrlFromCertificate(x509Cert);
                    MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
                    messageDigest.update(x509Cert.getEncoded());
                    byte[] hash = messageDigest.digest();
                    this.idsig = convertToHex(hash);
                    Log.i("OASP", this.oaspUrl);
                    break;
                } else if (ze.getName().startsWith("IDSIG_OLD/")) {
                    this.hasIdsigOld = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
