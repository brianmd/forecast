#!/bin/bash
if [[ -z "$WEATHER_API" ]]; then
    echo "Must set WEATHER_API environment variable."
    exit 1
fi

. ./setup.sh

docker run -it --rm -v $PWD/data:/usr/src/app/data -e "WEATHER_API=$WEATHER_API" -e AEROSPIKE_HOST=aero --link forecast-aero:aero --name fore-daemon forecast:$FORECAST_VERSION java -jar app-standalone.jar --daemon --aero $@
