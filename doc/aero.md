# Usage Instructions

Four terminals are needed. Start aerospike in the first terminal:
```sh
$ bin/aero/run-storage.sh
```
_Note: you may stop and re-start aero.sh to clear existing data._

You'll need an api key, which may be obtained by signing up on https://openweathermap.com/.
Start the ip and location processing daemon in the second terminal:
```sh
$ WEATHER_API=apikey bin/aero/daemon.sh [--live]
```
_Note: by default random data is used for the locations and forecasts. Add the __--live__ flag to use ipinfo.io and openweathermap.com for data._

Push a logfile into aerospike in the third terminal:
```sh
$ bin/aero/load-logfile.sh data/logfile
```
_Note: the parameter is the path to the logfile._

And finally request the histogram in the fourth terminal:
```sh
$ bin/aero/histogram.sh 10
```
_Note: the parameter is the number of bins for the histogram._

Aerospike's aql program may be run to view the data:
```sh
$ bin/aero/cli.sh
```
Two sets are used, ip and location. Here are three interesting aql commands:
```sh
aql> select * from test.ip
aql> select * from test.location
aql> show indexes
```

# About
The daemon program awakens two threads once a second, one to process new ip addresses,
and another to process new locations. By running these simultaneously, partial histogram
results are able to be obtained earlier (without waiting for all ip addresses to be
processed prior to starting locations.)

The histogram will show all the logfiles that have been processed so far.
You may request histograms while logfiles are still being loaded and/or processed -- results
will be shown for all data that has been processed to that time.

# Implementation Limitations
Ip locations don't change often, so the associated location doesn't need to be looked up very often.
(A `retrieved` timestamp has been added, to allow for expiring after some amount of time, should
this be desired.)
However, tomorrow's forecast does change and should be recalculated at least daily, and perhaps more often.
This implementation doesn't account for time.

There is no indication of when the processing has completed. When `histogram.sh` is run, results
are reported on all temperatures that have been calculated, but it would be appropriate to also
report (roughly) how many unprocessed ip addresses/locations exist.

Additionally, only one `daemon.sh` may run. There are two threads in the daemon, but they are coordinated
such that they won't both write to the same record. However, a second daemon would occasionally
make the same ip-location and location-forecast calls. Would need to add generation checks to prevent this.
