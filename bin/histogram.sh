#!/bin/bash
if ! ( docker images | grep forecast >/dev/null ); then
  echo "build forecast docker image"
  ./build.sh
fi

docker run -it --rm -v $PWD/data:/usr/src/app/data -e AEROSPIKE_HOST=aero --link forecast-aero:aero --name hist forecast java -jar app-standalone.jar $1 --aero --hist

