# Introduction

App signature is the fundation of the Android ecosystem. App developers must never lose the signing certificate/key and keep them secretly. However, we can always see misuses/abuses/leakages of the signing certs. Therefore, a new signing scheme is needed to allow developers to migrate to new signing certs, or at least allow them to "tag" apps signed by lost certificates.

So we proposed to add another layer of signature above the existing App signature. It's a nested signature that helps to identify/differentiate apps signed by the same installation signature. The new scheme is absolutely compatible with the existing App verification/installation process, and solves the headache of developers who have to switch to new signing certificate but can't.

Based on the extra signature layer, we introduced OASP (Online App Status Protocol). App developers should carry OASP URLs (must be HTTPS) together with the OASP signature. Through this URL, everyone else in the ecosystem can query app information to the developers and verify whether the app is trustworthy or not. This makes possible to build an open collaborative platform where developers and device/security vendors are no longer isolated -- there will be no gap/barrier between them. 

For more details, one can directly refer to [our talk](mosec17.pdf) at [MOSEC 2017](http://mosec.org).

Also, please note that OASP is part of the OASES (Open AI System Security Alliance) projects. For more information about OASES, please visit its [website](https://oases.io).

# Changelog

## v0.2

- Avoid the duplication of the MF/SF digest files introduced by the OASP signature. Now OASP only put its certificate and url under APK signing; it then signs the MANIFEST.MF (shared with APK signing) and inserts the OASP signature into META-INF. This significantly reduces file size increase, simplifies the signing/verification process, and speeds up the performance.

- Split the OASP verification logic out of the demo app as a standalone module, which makes it easier for other apps to integrate OASP.

- Further polish the tool scripts and test cases.

- Unify the term "IDSIG" and "OASP" into "OASP" alone to avoid confusion.

## v0.1

- The initial implementation of the nested signing scheme described in [our MOSEC 2017 talk](mosec17.pdf)


# Source Code Description

This repo contains a reference/demo implementation of OASP. Both client and server are provided. 

## client

This an Android app that scans over all installed apps. If an app supports OASP, it will collects necessary info (package name, version code, hash, App certificate, and OASP certificate, etc.) and consult OASP server for the app's status (whether it can be trusted or not). Details about building and running the client can be found inside the client directory.

We deliberately split the core OASP logic into a standalone module/library called "oasp". This makes it easy for other apps to integrate it.

## server

An HTTPS server that serves OASP info. On GET, it replies with supported OASP protocol version; and on POST, it replies with apps' status. The specific policy to determine whether an app is good or bad is up to the app developers. And how to consume the OASP response is up to the device/security vendors. 

## prepare.sh

A bootsrap script to generate all the needed certificates/keys to play with the client and server. It generates three certificates/keys, one for the normal apk signing, one for the OASP signing, and the third for the OASP HTTPS server.

## sign.sh

This is the tool to sign the app using both the normal apk signature and the OASP signature.

## verify.sh

This is the commandline tool to quickly verify the OASP signature.

## tests

This directory contains test cases of the client and server. Please refer to the README inside the directory for details.


# How to Run

1. Execute prepare.sh to generate the needed certificates/keys. It by default generates:
  * keys/orig.p12: the cert/key used for the original apk signing
  * keys/oasp.cert/.key: the cert/key used for OASP signing
  * keys/old1/2.cert/.key: old OASP certs/keys that can be upgraded from (optional)
  * server/\*.pem: the cert/key used by the HTTPS server
If you have your own cets/keys, please modify the script and place your certs/keys in the corresponding directories.
2. Assume your apk is placed in this directory as "sample.apk", run ./sign.sh sample.apk to sign it.
3. Build the client app (or implement your own app using the "oasp" module in the provided client app), and install both sample.apk and client app on your device.
4. Launch the OASP server by executing server.py. Make sure that the HTTPS certificate can be trusted by your device, then enjoy!

# Frequently Asked Questions

**Q: Is OASP simply using multiple cert/key to sign the APK?**

A: No, OASP utilizes nested signing (normal APK signature -> OASP signature -> OASP server's HTTPS signature) to introduce extra layers of attestation and trust. Also, OASP brings in flexibility and ontime information exchange through the introduction of the OASP url.

**Q: How can the verifier know whom the OASP cert belongs to?**

A: The verifier can attest the OASP cert through the OASP url.

**Q: Then how can the verifier know if the OASP url has been maliciously replaced?**

A: That's why we enforce the OASP url to be HTTPS. The verifier can log the HTTPS certificate for decision making (if the majority of the APKs sharing the same package name and version lead to the same HTTPS site, the verifier can safely treat the url as authentic) and forensic purpose (to track the minority that lead to different HTTPS sites).

**Q: Who can benefit from OASP?**

A: OASP is an open and collaborative security mechanism. Developers, security vendors, app stores, and device vendors -- all roles in the Android ecosystem can benefit from it. With OASP, developers can timely provide the app status information; security vendors can obtain a new repulation based malware detection factor; app stores can take down apps based on the ontime OASP status updates, and act as the OASP information hub; device vendors also gain an additional layer to verify app status, providing stronger protection for end users. In general, OASP aims to solve the private key leakage, product resale, certificate upgrade etc. problems, yet without introducing a new ecosystem central dependency.

# License

The code is under the BSD license. Please refer to the [LICENSE](LICENCE) file for details.


# Contacts

[Yulong Zhang](ylzhang@baidu.com)

[Lenx (Tao) Wei](lenx@baidu.com)


