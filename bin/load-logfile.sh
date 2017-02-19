#!/bin/bash
. ./setup.sh

if [[ ! -z "$1" ]]; then
    cp "$1" data/dockerlog || exit 1
    dockerlogfile=dockerlog
else
    echo "please specify the filename to load"
    exit 1
fi

docker run -it --rm -v $PWD/data:/usr/src/app/data --link forecast-aero:aero -e WEATHER_API=$WEATHER_API -e AEROSPIKE_HOST=aero --name load-log forecast:$FORECAST_VERSION java -jar app-standalone.jar $dockerlogfile --aero --load $@

