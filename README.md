# forecast

Parse specialized log file. Show histogram of tomorrow's maximum temperature
for the included ip addresses.

By default, random locations and forecasts are returned soas to not hit the ip-to-location and location-to-weather services too hard. Append '--live' to command to get real-time data.

A weather api will be needed, which may be acquired by signing up on https://openweathermap.com/

## Usage

There are two ways to run this:

- [memory version](doc/memory.md), uses a memory repository for holding the ip and location information,
- [aerospike version](doc/aero.md), uses, well, an aerospike repository.

