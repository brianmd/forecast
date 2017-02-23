#!/bin/sh
. ./ensure-version.sh

if ! ( docker ps | grep forecast-aero >/dev/null ); then
  echo "please run 'bin/aero.sh' before running this command"
  exit 1
fi

