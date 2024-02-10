#!/bin/bash

cd files
openssl s_server -no_tls1_3 -state -msg -trace -security_debug_verbose -debug -accept 4443 -cert ../server$1.crt -key ../server$1.key -Verify 4 -CAfile ../com_clover_config_default_cert_ca_device.crt -WWW -cipher "DEFAULT:@SECLEVEL=0" -client_sigalgs rsa_pkcs1_sha256 -no_check_time

# rsa_pkcs1_sha256 rsa_pss_rsae_sha256
