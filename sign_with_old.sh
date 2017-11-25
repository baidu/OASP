#!/bin/sh

if [ $# -ne 1 ]
then 
	echo "Please specify the apk to be signed"
	echo "Example: ./sign_with_old.sh sample.apk"
	exit
fi

# Remove old signatures
if unzip -l $1 | grep -q "META-INF/"
then
	zip -dq $1 META-INF/*
fi
if unzip -l $1 | grep -q "META-INF-OASP"
then
	zip -dq $1 META-INF-OASP/*
fi

# Adding OASP certificate and url to the apk
echo "Adding OASP certificate and url..."
unzip -q $1 -d tmp
mkdir -p tmp/META-INF-OASP
mkdir -p tmp/META-INF-OASP/OLD/
cp keys/oasp.cert url.txt tmp/META-INF-OASP/
cp keys/old*.cert tmp/META-INF-OASP/OLD/
cd tmp
zip -uq ../$1 META-INF-OASP/*
zip -uq ../$1 META-INF-OASP/OLD/*
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
mkdir -p META-INF/OASP-OLD
for f in `ls ../keys/old*.key`
do
	k=${f##*/} # remove the prefix path before "/"
	openssl dgst -sha256 -sign $f -out META-INF/OASP-OLD/${k%.*}.sig META-INF/MANIFEST.MF
	zip -uq ../$1 META-INF/OASP-OLD/${k%.*}.sig
done
cd ..
echo "OASP signed"

# Clean up
rm -rf tmp