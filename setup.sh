#!/bin/sh
. ./version.sh

if ! ( docker images | grep "forecast.*$FORECAST_VERSION" >/dev/null ); then
    echo "build forecast docker image $FORECAST_VERSION"
    ./build.sh
fi

if ! ( docker ps | grep forecast-aero >/dev/null ); then
  echo "please run 'bin/aero.sh' before running this command"
  exit 1
fi

