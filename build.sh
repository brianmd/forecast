#!/bin/sh
. ./version.sh
docker build -t forecast:$FORECAST_VERSION .
