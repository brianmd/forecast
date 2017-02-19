# Usage Instructions

Four terminals are needed. Start aerospike in the first terminal:
```sh
$ bin/aero.sh
```
_Note: you may stop and re-start aero.sh to clear existing data._

You'll need an api key, which may be obtained by signing up on https://openweathermap.com/.
Start the ip and location processing daemon in the second terminal:
```sh
$ WEATHER_API=apikey bin/daemon.sh [--live]
```
_Note: by default random data is used for the locations and forecasts. Add the __--live__ flag to use ipinfo.io and openweathermap.com for data._

Push a logfile into aerospike in the third terminal:
```sh
$ bin/load-logfile.sh data/logfile
```
_Note: the parameter is the path to the logfile._

And finally request the histogram in the fourth terminal:
```sh
$ bin/histogram.sh 10
```
_Note: the parameter is the number of bins for the histogram._

Aerospike's aql program may be run to view the data:
```sh
$ bin/aql.sh
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
results are able to be obtained earlier (don't have to wait for all ip addresses to be
processed prior to starting locations.)

The histogram will show all the logfiles that have been processed so far.
You may request histograms while logfiles are still being loaded and/or processed -- results
will be shown for all data that has been processed to that time.

# Implementation Limitations
Ip locations don't change often, so the associated location doesn't need to be looked up very often. However, tomorrow's forecast does change and should be recalculated at least daily, and perhaps more often. This implementation doesn't account for time.

Lastly, only one daemon may run. There are two threads in the daemon, but they are coordinated such that they won't both write to the same record. However, a second daemon would occasionally make the same ip-location and location-forecast calls. Would need to add generation checks to prevent this.
