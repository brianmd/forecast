#!/bin/sh
docker run -it --rm --link aerospike:forecast-aero --name aql aerospike/aerospike-tools aql
