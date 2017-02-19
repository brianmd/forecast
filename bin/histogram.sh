#!/bin/bash
. ./version.sh

if ! ( docker images | grep "forecast:$FORECAST_VERSION" >/dev/null ); then
  echo "build forecast docker image"
  ./build.sh
fi

docker run -it --rm -v $PWD/data:/usr/src/app/data -e AEROSPIKE_HOST=aero --link forecast-aero:aero --name hist forecast:$FORECAST_VERSION java -jar app-standalone.jar $1 --aero --hist

