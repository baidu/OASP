# Introduction

App signature is the fundation of the Android ecosystem. App developers must never lose the signing certificate/key and keep them secretly. However, we can always see misuses/abuses/leakages of the signing certs. Therefore, a new signing scheme is needed to allow developers to migrate to new signing certs, or at least allow them to "tag" apps signed by lost certificates.

So we proposed to add another layer of signature above the existing App signature. It's a nested signature that helps to identify/differentiate apps signed by the same installation signature. The new scheme is absolutely compatible with the existing App verification/installation process, and solves the headache of developers who have to switch to new signing certificate but can't.

Based on the extra signature layer, we introduced OASP (Online App Status Protocol). App developers should carry OASP URLs (must be HTTPS) together with the OASP signature. Through this URL, everyone else in the ecosystem can query app information to the developers and verify whether the app is trustworthy or not. This makes possible to build an open collaborative platform where developers and device/security vendors are no longer isolated -- there will be no gap/barrier between them. 

For more details, one can directly refer to [our talk](mosec17.pdf) at [MOSEC 2017](http://mosec.org).


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

# License

The code is under the BSD license. Please refer to the [LICENSE](LICENCE) file for details.


# Contacts

[Yulong Zhang](ylzhang@baidu.com)

[Lenx (Tao) Wei](lenx@baidu.com)


