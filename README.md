OMERO Process Container Steward
===============================

Tools for stewardship of the OMERO server ProcessContainer.

Requirements
============

* OMERO 5.6.x+
* Java 8+

Installation
============

The latest release, built by GitHub Actions, can be found here:

* https://github.com/glencoesoftware/omero-pc-steward/releases

It can also be build by hand:

        git clone git@github.com:glencoesoftware/omero-pc-steward.git
        cd omero-pc-steward
        ./gradlew jar

1. Copy the downloaded `.jar` or the built one from `build/libs` into the `$OMERODIR/lib/server` directory of your OMERO installation

2. Configure logging by adding `<logger name="com.glencoesoftware" level="INFO"/>` to the "ROOT logger" section of `$OMERODIR/etc/logback.xml`

3. Optionally, configure the frequency of the cleanup checks by setting the config `omero.managed.steward.cron` to a valid cron string (see https://www.quartz-scheduler.org/api/1.8.6/org/quartz/CronExpression.html). The default is set to check every minute for completed import processes to clean up. If you wanted to check every 30 seconds, you could set

        omero config set omero.managed.steward.cron '*/30 * * * * ?'

5. Restart OMERO server with

        omero admin restart

6. In `$OMERODIR/var/log/Blitz-0.log`, you should see logging from the ProcessContainerSteward once per configured period (default every minute) reporting how many `ImportProcesses` there are in the `ProcessContainer`

		2023-07-25 10:02:30,001 INFO  [       c.g.omero.ProcessContainerSteward] (2-thread-2) Number of processes in the container: 0

Reference
=========

* https://www.openmicroscopy.org/
