#!/bin/sh
docker run -it --rm --link forecast-aero:aerospike --name aql aerospike/aerospike-tools aql
