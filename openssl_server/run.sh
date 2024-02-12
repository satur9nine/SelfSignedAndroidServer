#!/bin/bash

cd files
openssl s_server -accept 4433 -cert ../server$1.crt -key ../server$1.key -WWW -cipher "DEFAULT:@SECLEVEL=0"
