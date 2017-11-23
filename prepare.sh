#!/bin/sh

mkdir -p keys

# This step is to generate the original apk signing keystore. 
# If you already have one, simply copy it as keys/orig.p12
openssl genrsa -out orig_key.pem 4096
openssl req -new -x509 -days 1024 -key orig_key.pem \
	-out orig_cert.pem -subj "/CN=SampleAppCert"
openssl pkcs12 -export -in orig_cert.pem -inkey orig_key.pem \
	-password pass:passwd -out keys/orig.p12 -name cert
rm -f orig_key.pem orig_cert.pem

# This step is to generate the OASP key and cert
openssl genrsa -out keys/oasp.key 4096
openssl req -new -x509 -sha256 -days 1024 -key keys/oasp.key \
	-out keys/oasp.cert -subj "/CN=SampleOASPCert"

# Prepare the OASP url
echo "https://oasp.baidu.com" > url.txt

# Generate OASP server key/cert
# You don't need it if you have your own HTTPS server
openssl genrsa -out server/key.pem 4096
openssl req -new -x509 -days 1024 -key server/key.pem -out server/cert.pem \
    -subj "/C=US/ST=Sunnyvale/O=Demo/OU=Demo/CN=oasp.yourdomain.com"
