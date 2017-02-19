# forecast

Parse specialized log file. Show histogram of tomorrow's maximum temperature
for the included ip addresses.

By default, random locations and forecasts are returned soas to not hit the ip-to-location and location-to-weather services too hard. Append '--live' to command to get real-time data.

A weather api will be needed, which may be acquired by signing up on https://openweathermap.com/

## Usage

There are two ways to run this:

- [memory version](doc/memory.md), uses a memory repository for holding the ip and location information,
- [aerospike version](doc/aero.md), uses, well, an aerospike repository.

## Logfiles

Three example logfiles are provided in the data directory:

- __logfile__, one invalid ip address, ten good addresses, and three duplicate ip addresses. Histogram should have temperatures for only the ten valid, unique addresses.
- __logfile2__, has the ten valid addresses from _logfile_ plus ten more. Processing _logfile_ and _logfile2_ multiple times should leave a histogram with 20 entries.
- __logfile-big__, has 4082 entries.

The logfile for this program (how meta) is data/log.log.


