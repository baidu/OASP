#!/bin/sh

if [ $# -ne 1 ]
then 
	echo "Please specify the apk to be signed"
	echo "Example: ./sign.sh sample.apk"
	exit
fi

# Remove old signatures
if unzip -l $1 | grep -q "META-INF/"
then
	zip -dq $1 META-INF/*
fi

# Adding OASP certificate and url to the apk
echo "Adding OASP certificate and url..."
unzip -q $1 -d tmp
mkdir -p tmp/META-INF-OASP
cp keys/oasp.cert url.txt tmp/META-INF-OASP/
cd tmp
zip -uq ../$1 META-INF-OASP/*
cd ..

# Perform the normal Android APK signing
echo "Normal APK signing..."
jarsigner -tsa http://timestamp.digicert.com \
	-keystore keys/orig.p12 -storepass passwd $1 cert

# OASP Signing
echo "OASP signing..."
unzip -q $1 META-INF/* -d tmp/
openssl dgst -sha256 -sign keys/oasp.key -out tmp/META-INF/oasp.sig tmp/META-INF/MANIFEST.MF
cd tmp
zip -uq ../$1 META-INF/oasp.sig
cd ..
echo "OASP signed"

# Clean up
rm -rf tmp