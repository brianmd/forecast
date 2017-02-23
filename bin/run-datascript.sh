#!/bin/bash
. ./ensure-version.sh

if [[ -z "$WEATHER_API" ]]; then
  echo "Must set WEATHER_API environment variable."
  exit 1
fi

if [[ ! -z "$1" ]]; then
  cp "$1" data/dockerlog || exit 1
  dockerlogfile=dockerlog
fi

docker run -it --rm -v $PWD/data:/usr/src/app/data -e "WEATHER_API=$WEATHER_API" --name fore forecast:$FORECAST_VERSION java -jar app-standalone.jar $dockerlogfile $2 $3 --process --hist --datascript

