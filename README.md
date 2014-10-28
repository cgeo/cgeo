[c:geo](http://www.cgeo.org) is an open source, full-featured, always ready-to-go client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.
It does not require a web browser or exports - just download and start right away.

## You want to contribute?

Perfect! Please **tell us in the [issue tracker](https://github.com/cgeo/cgeo/issues) before hacking** on your great new feature. It would be bad if you have implemented something great, but we can't include it because it doesn't fit the remaining architecture and code. You might also want to chat with the developers on channel #cgeo on the freenode IRC network.

## Project status

[![Build Status](http://ci.cgeo.org/job/c-geo/badge/icon)](http://ci.cgeo.org/job/c-geo/)<br>
[![Issue Stats](http://issuestats.com/github/cgeo/cgeo/badge/pr)](http://issuestats.com/github/cgeo/cgeo)<br>
[![Issue Stats](http://issuestats.com/github/cgeo/cgeo/badge/issue)](http://issuestats.com/github/cgeo/cgeo)

## Get the source

Fork the [project source code](https://github.com/cgeo/cgeo), make changes to your clone and [create a pull request](https://help.github.com/articles/using-pull-requests) afterwards.

### Branches ###

- **master** is for development of new features. Nightly builds are created from this branch.
- **release** is for all bug fixes of already existing features. So if a bug is reported in released version, it should be fixed on this branch (and merged to master afterwards).

A more complex bugfix can be first tested against the `master` branch and integrated in the nightly builds, while kept compatible with the `release` branch for a later integration.
Such a procedure is [described in the wiki](https://github.com/cgeo/cgeo/wiki/How-to-get-a-bug-fix-into-the-release).

## Set up Eclipse

- Install an Eclipse distribution for your OS from http://eclipse.org/downloads/ (you may choose the Java developers distribution).
- Start Eclipse, choose any directory as workspace. Close the welcome screen, if it appears.
- After the workbench has started, select File | Import | Install | Install Software Items From File and select a locally downloaded copy of https://github.com/cgeo/cgeo/tree/master/main/project/eclipse%20installation/cgeo%20eclipse%20components.p2f. This way you can easily install all necessary plugins.
- After forking the project you should import the Eclipse projects in your workspace with File | Import | Projects from Git.

## Build

### Prerequisites ###

- [Android SDK](http://developer.android.com/sdk) (latest version) including Google APIs V19
- [Ant](http://ant.apache.org) 1.6.0+ for building c:geo on the command line (not necessary when using only Eclipse)
- If you use Microsoft Windows, [Google USB Driver](http://developer.android.com/sdk/win-usb.html) to install the application on the smartphone

### Structure ###

c:geo sources and executables are located in the `main` directory. Tests are located in the `tests` directory.

### Known limitations ###

If the workspace directory name contains a space and leads to errors in the -dex Ant target, then you need to set the property "basedir" in your `local.properties` to the 8.3 name of the directory where this script is located on your disk.

### Configuration ###

1. copy `./main/templates/private.properties` to `./main/`
2. edit `private.properties` (see comments in the file)
3. copy `./main/templates/local.properties` to `./main/`
4. copy `./main/templates/local.properties` to `./tests/`
5. edit `local.properties` (see comments in the file)
6. copy `local.properties` to all other projects (currently android-support-v7-appcompat, google-play-services_lib, mapswithme-api, showcaseview)
7. copy `local.properties` to cgeo-calender and cgeo-contatcs if you plan to hack on the plugins
6. copy `./main/templates/keys.xml` to `./main/res/values/`
7. edit `./main/res/values/keys.xml` and insert several keys (see comments in the file)
  * Google Maps API v1 key (you can leave it empty, but then Google Maps don't work - Google doesn't hand out new keys for Google Maps v1, you have to use an existing one)
  * request your personal consumer key and secret for the various opencaching nodes we support:
    * [opencaching.de OKAPI signup](http://www.opencaching.de/okapi/signup.html)
    * [opencaching.pl OKAPI signup](http://www.opencaching.pl/okapi/signup.html)
    * [opencaching.ro OKAPI signup](http://www.opencaching.ro/okapi/signup.html)
    * [opencaching.nl OKAPI signup](http://www.opencaching.nl/okapi/signup.html)
    * [opencaching.us OKAPI signup](http://www.opencaching.us/okapi/signup.html)
    * [opencaching.org.uk OKAPI signup](http://www.opencaching.org.uk/okapi/signup.html)

### Building with Ant ###

Run one of the following commands in `./main`

    ant help
    ant clean
    ant debug
    ant release

or use the Ant view of Eclipse

### Debugging ###

In Eclipse, create a Debug Configuration for an Android Application using the menu Run | Debug Configurations

### Testing ###

The Test classes can be found in the project cgeo-os-test. Test classes should be located in the same package as
the class under test.
Every class can be "Run As" (or "Debug As") an [Android JUnit Test](http://developer.android.com/guide/topics/testing/testing_android.html) from Eclipse.
To run all tests use the same "Run As" menu item from the context menu of the test project.

For tests to run successfully you need to configure c:geo on the emulator that runs the test with a valid geocaching.com account. In order for all tests to be successfull the account needs to be premium.

## License

c:geo is distributed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Contact

- Website: http://www.cgeo.org/
- Support: support@cgeo.org
- Twitter: http://twitter.com/android_gc
- Facebook: http://www.facebook.com/android.geocaching
- Google+: https://plus.google.com/105588163414303246956
- Google Play: https://play.google.com/store/apps/details?id=cgeo.geocaching
- Live status: http://status.cgeo.org/
