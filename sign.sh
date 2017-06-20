#!/bin/sh

if [ $# -ne 1 ]
then 
	echo "Please specify the apk to be signed"
	echo "Example: ./sign.sh sample.apk"
	exit
fi

# Remove existing signatures if exist
if unzip -l $1 | grep -q "META-INF"
then
	zip -dq $1 META-INF/*
fi
if unzip -l $1 | grep -q "IDSIG"
then
	zip -dq $1 IDSIG/*
fi

# Generate IDSIG
echo "IDSIG signing..."
jarsigner -tsa http://timestamp.digicert.com \
	-keystore keystores/idsig.p12 -storepass passwd $1 id
unzip -q $1 -d tmp
cd tmp
mv META-INF IDSIG
zip -uq ../$1 IDSIG/*
cd ..
zip -dq $1 META-INF/*
rm -rf tmp

# And the normal Android APK signature
echo "Normal APK signing..."
jarsigner -tsa http://timestamp.digicert.com \
	-keystore keystores/orig.p12 -storepass passwd $1 cert