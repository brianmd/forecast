#!/bin/sh
. ./version.sh

if ! ( docker images | grep "forecast\\s*${FORECAST_VERSION}\\s" >/dev/null ); then
    echo "build forecast docker image $FORECAST_VERSION"
    bin/common/build.sh
fi
