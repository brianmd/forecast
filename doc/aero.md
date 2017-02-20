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


# How It Works

![how it works](how-it-works.jpg)

Aerospike's `test` namespace contains two sets: `ip` and `location`.

1. `load-logfile.sh` extracts ip addresses from the log file.
For each discovered ip address, it checks to see if that ip address
has already been entered in the `ip` set, and inserts it if not
found with a `state` of "new".

2. `daemon.sh` has two threads which operate once a second,
one for processing new ips and another
for processing new locations. The former performs a query for all
`ip`'s with a state of "new". It then requests a geo-location for each
of these, using either the default randomize or, if `--live` was specified,
by an http call to ip_info.io. The returned locations are stored in the
`location` set with a `state` of "new", and updates the `ip`'s state
to "done". If an error occurs, the `state` is instead updated to "error".

3. The other thread of `daemon.sh` performs similarly, except against
the `location` set, and uses the openweathermap.com service to determine
the location's high temperature for tomorrow. The temperature is stored
back in the `location` set. Another option would have been to store the
results in a separate `temperature` set. This was the approach I used
in https://github.com/brianmd/scraper, but opted for the simplier approach
here.

Aside: the process for these two threads are similar enough that were I
to continue working on this, I'd make a framework of sorts which would
share the commonalities (requesting "new" states, error handling) and
accept a hash of the differences: which set to query, how to process, and
what to do with the results.

Aside 2: there is a third, insignificant thread which reports metrics
every fifteen seconds.


4. `histogram.sh` queries for all `state`="done" in the `location` set
and prints a histogram of the returned results.


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
