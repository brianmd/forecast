#!/bin/sh
# docker run -it --rm --name forecast-aero -p 3000:3000 -p 3001:3001 -p 3002:3002 aerospike/aerospike-server
docker run -it --rm --name forecast-aero aerospike/aerospike-server
