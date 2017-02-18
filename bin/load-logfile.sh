#!/bin/bash
if ! ( docker images | grep forecast >/dev/null ); then
  echo "build forecast docker image"
  ./build.sh
fi

if [[ ! -z "$1" ]]; then
    cp "$1" data/dockerlog || exit 1
    dockerlogfile=dockerlog
fi

docker run -it --rm -v $PWD/data:/usr/src/app/data --link forecast-aero:aero -e WEATHER_API=$WEATHER_API -e AEROSPIKE_HOST=aero --name load-log forecast java -jar app-standalone.jar $dockerlogfile --aero --load $@

