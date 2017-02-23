# forecast

Parse specialized log file. Show histogram of tomorrow's maximum temperature
for the included ip addresses.

By default, random locations and forecasts are returned soas to not hit the ip-to-location and location-to-weather services too hard. Append '--live' to command to get real-time data.

A weather api will be needed, which may be acquired by signing up on https://openweathermap.com/

## Usage

There are two ways to run this:

- [memory version](doc/memory.md), uses a memory repository for holding the ip and location information,
- [aerospike version](doc/aero.md), uses an aerospike repository.
- `datascript version`, uses, well, a datascript repository.
Usage is identical to the memory version, except use `bin/datascript/run.sh` in place of `bin/memory/run.sh`.

## Logfiles

Three example logfiles are provided in the data directory:

- __logfile__, one invalid ip address, ten good addresses, and three duplicate ip addresses. Histogram should have temperatures for only the ten valid, unique addresses.
- __logfile2__, has the ten valid addresses from _logfile_ plus ten more. Processing _logfile_ and _logfile2_ multiple times should leave a histogram with 20 entries.
- __logfile-big__, has 4082 entries.

The logfile for this program (how meta) is data/log.log.

# How It Works

![how it works](doc/how-it-works.jpg)

_Note: the following description explains the aerospike storage version,
but the memory and datascript repositories operate similarly._

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


