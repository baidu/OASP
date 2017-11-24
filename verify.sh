#!/bin/sh

if [ $# -ne 1 ]
then
    echo "Please specify the apk to be signed"
    echo "Example: ./sign.sh sample.apk"
    exit
fi

unzip -q $1 -d tmp

# Step 0, assuming that the apk has been installed to Android, 
#   so META-INF/*.SF should have already been verified by Android

# Step 1, verify OASP cert
openssl verify tmp/META-INF-OASP/oasp.cert

# Step 2, if Step 1 succeeded, verify OASP signature
openssl x509 -pubkey -noout -in tmp/META-INF-OASP/oasp.cert > tmp/META-INF-OASP/oasp.pub
openssl dgst -sha256 -verify tmp/META-INF-OASP/oasp.pub -signature tmp/META-INF/oasp.sig tmp/META-INF/MANIFEST.MF

# Step 3, query OASP url (in tmp/OASP/url.txt) for attestation


# Clean up
rm -rf tmp