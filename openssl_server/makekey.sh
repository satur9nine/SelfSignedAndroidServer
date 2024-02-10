#!/bin/sh

# Generate self signed root CA cert
#openssl req -nodes -x509 -newkey rsa:3072 -keyout ca.key -out ca.crt -subj "/C=US/ST=CA/L=San Mateo/O=Jacob/OU=TLS Test CA/CN=localhost/emailAddress=jacob@clover.com"

# Generate server cert to be signed
openssl req -nodes -newkey rsa:512 -keyout server512.key -out server512.csr -subj "/C=US/ST=CA/L=San Mateo/O=Jacob/OU=TLS Test 512/CN=localhost/emailAddress=jacob@clover.com"
openssl req -nodes -newkey rsa:1024 -keyout server1024.key -out server1024.csr -subj "/C=US/ST=CA/L=San Mateo/O=Jacob/OU=TLS Test 1024/CN=localhost/emailAddress=jacob@clover.com"
openssl req -nodes -newkey rsa:2048 -keyout server2048.key -out server2048.csr -subj "/C=US/ST=CA/L=San Mateo/O=Jacob/OU=TLS Test 2048/CN=localhost/emailAddress=jacob@clover.com"

# Sign the server cert
openssl x509 -req -days 7300 -in server512.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server512.crt
openssl x509 -req -days 7300 -in server1024.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server1024.crt
openssl x509 -req -days 7300 -in server2048.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server2048.crt

