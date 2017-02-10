# forecast

Parse specialized log file. Show histogram of tomorrow's maximum temperature
for the included ip addresses.

By default, random locations and forecasts are returned soas to not hit the ip-to-location and location-to-weather services too hard. Append '--live' to command to get real-time data.

## Usage

```sh
$ ./run log-filename-to-parse [num-bins [--live]]
```

For example, this command will make 10 location and weather calls to api servers (there are 11 lines, one of which does not have a valid ip address):
```sh
./run data/logfile 5 --live
```

while this command will generate 4080 locations and forecasts (not live, 4082 lines, 2 of which are not valid):
```sh
./run data/logfile-big 10 --live
```

The above log files are supplied, but you may use any file path.


