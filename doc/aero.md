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

You'll see both "id" and "\_id" data in the sets (tables), both of which contain the key of the row. Aerospike doesn't seem to return the key on queries, so my first pass was to add it as a bin named "ip". However, I wanted aerospike queries to function identitically with memory queries, the latter of which does provide the key as well as the row. To accomodate this (and to make the tests pass!), I add "\_id" behind the scenes to the row in the repository code, and remove it when passing it back to the client. "ip" may now be removed.

I wanted to allow arbitrary types for the bin userKeys and values, but didn't go quite that far as it wasn't needed for this exercise.
