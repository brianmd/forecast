#!/bin/sh
export FORECAST_VERSION=1.03

if ! ( docker images | grep "forecast\\s*${FORECAST_VERSION}\\s" >/dev/null ); then
    echo "build forecast docker image $FORECAST_VERSION"
    ./build.sh
fi

