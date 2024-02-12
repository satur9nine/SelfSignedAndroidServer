#!/bin/bash

# Generate self signed root CA cert
openssl req -days 1000 -nodes -x509 -newkey rsa:3072 -keyout ca.key -out ca.crt -subj "/C=US/ST=CA/L=San Mateo/O=Jacob/OU=TLS Test CA/CN=Jacob Root/emailAddress=jacob@example.com"

# Generate server cert to be signed
openssl req -nodes -newkey rsa:2048 -keyout server2048_key.key -out server2048_csr.csr -subj "/C=US/ST=CA/L=San Mateo/O=Jacob/OU=TLS Test 2048/CN=localhost/emailAddress=jacob@example.com"

# Sign the server cert
openssl x509 -req -extfile <(printf "extendedKeyUsage=serverAuth\nsubjectAltName=DNS:localhost,DNS:127.0.0.1") -days 365 -in server2048_csr.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server2048_crt.crt

openssl x509 -in ca.crt -text -noout
openssl x509 -in server2048_crt.crt -text -noout