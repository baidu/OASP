#!/bin/sh

mkdir -p keystores

# This step is to generate the original apk signing keystore. 
# If you already have one, simply copy it as keystores/orig.p12
openssl genrsa -out orig_key.pem 4096
openssl req -new -x509 -days 1024 -key orig_key.pem -out orig_cert.pem \
	-subj "/CN=SampleApp"
openssl pkcs12 -export -in orig_cert.pem -inkey orig_key.pem \
	-password pass:passwd -out keystores/orig.p12 -name cert
rm -f orig_key.pem orig_cert.pem

# This step is to generate the IDSIG keystore (keystores/idsig.p12)
openssl genrsa -out key.pem 4096
openssl req -new -x509 -sha256 -days 1024 \
	-key key.pem -out cert.pem \
	-config oasp.cnf -extensions oasp
openssl pkcs12 -export -in cert.pem -inkey key.pem \
	-password pass:passwd -out keystores/idsig.p12 -name id
rm -f key.pem cert.pem

# Generate OASP server key/cert
openssl genrsa -out server/key.pem 4096
openssl req -new -x509 -days 1024 -key server/key.pem -out server/cert.pem \
	-subj "/C=US/ST=Sunnyvale/O=Demo/OU=Demo/CN=oasp.yulongzhang.com"