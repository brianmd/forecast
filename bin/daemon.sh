#!/bin/bash
if [[ -z "$WEATHER_API" ]]; then
    echo "Must set WEATHER_API environment variable."
    exit 1
fi

if ! ( docker ps | grep forecast-aero >/dev/null ); then
    echo "running aerospike in docker"
    bin/aero.sh
fi

if ! ( docker images | grep forecast >/dev/null ); then
    echo "build forecast docker image"
    ./build.sh
fi

docker run -it --rm -v $PWD/data:/usr/src/app/data -e "WEATHER_API=$WEATHER_API" -e AEROSPIKE_HOST=aero --link forecast-aero:aero --name fore-daemon forecast java -jar app-standalone.jar --daemon --aero $@
