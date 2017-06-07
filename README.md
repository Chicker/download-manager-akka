## Description

This is a console utility that downloads files over HTTP protocol.

The utility can download several hyperlinks simultaneously. For this purpose it has a command-line 
argument named `-n`, which sets a count of simultaneous execution threads. By default, this value was set to 2 threads.

There is a possibility to limit download speed overall. For this purpose the utility has a 
command-line argument named `-l`. The limit speed will be splitted between execution threads. By default, this value was set to 64Kb/sec.

A list of hyperlinks should be saved in a simple plain text file that should has a specific 
structure. The path to this file sets by command-line argument named `-f`.

To set the path to the output folder where downloaded files will be saved,
you can use the command-line argument named `-o`.

### Hyperlinks file structure

In the links file every hyperlink should be written in individual line/record. Such record should
consist of the following two parts separated by whitespace character:

- HTTP URL (for example, `http://example.com/archive.zip`)
- the filename to save that include file extension. In this file the content of HTTP link
will be saved (for example, `my_archive.zip`).

### Command line arguments

The program have a several required command-line arguments:

* `-f` - a full path to the file that consists a list of hyperlinks.
* `-l` - an overall bandwidth limit for downloading (bytes/seconds) (you can use mnemonic
symbols, e.g. 100k, 1M).
* `-n` - a count of simultaneous downloading threads.
* `-o` - a full path to the folder where downloaded files will be saved

## Libs thar are used

### Akka

Parallel process of downloading files has been implemented with using Actors model.

Each file that should be downloaded is processed by one actor called `DownloaderActor`.

Whole downloading process is controlled by supervisor actor called `DownloadManager`. This actor owns a queue of the tasks. Each task from the queue is the file which should be downloaded. The download manager pushes a next task to the downloader actor when he will report about finishing own task.
When the queue will be empty the download manager instead the send a new task it stops this downloader actor (the actor which done his work and ready to get a new task).

When all of the downloader actors has stopped, the download manager shutdowns actorsystem.

### Http client

For the sending Http requests is using `httpclient` library which is a part of `Apache HttpComponents`. It is battle-tested Java library that commonly used in the Java world.

### Command line interface

For the building command line interface for our program is using `scopt` library, which is also well known.

## Configuration

There are two confiuration file:

* application.conf (default configuration)
* dev.conf (configuration which is using for development purpose)

So, for example, in "dev" configuration file the logging level has been set to "debug" value
for the akka system.

## Build & Run

To create a fat (standalone) jar file, is using sbt `assembly` plugin. Just execute the command `sbt assembly`. In the result you will get an `all-in-one` jar file in the `target/scala-2.11/` folder. Run this file as a regular jar file.

### Run with dev configuration

Just add the parameter `-Dconfig.resource=/dev.conf`
