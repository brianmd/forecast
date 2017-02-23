#!/bin/bash
. bin/aero/setup.sh

docker run -it --rm -v $PWD/data:/usr/src/app/data -e AEROSPIKE_HOST=aero --link forecast-aero:aero --name hist forecast:$FORECAST_VERSION java -jar app-standalone.jar $1 --aero --hist

