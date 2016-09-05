[c:geo](http://www.cgeo.org) is an open source, full-featured, always ready-to-go client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.
It does not require a web browser or exports - just download and start right away.

## You want to contribute?

Perfect! Please **tell us in the [issue tracker](https://github.com/cgeo/cgeo/issues) before hacking** on your great new feature. It would be bad if you have implemented something great, but we can't include it because it doesn't fit the remaining architecture and code. You might also want to chat with the developers on channel #cgeo on the freenode IRC network.

## Project status

[![Build Status](http://ci.cgeo.org/job/cgeo%20continuous%20integration/badge/icon)](http://ci.cgeo.org/job/cgeo%20continuous%20integration/)<br>
[![Issue Stats](http://issuestats.com/github/cgeo/cgeo/badge/pr)](http://issuestats.com/github/cgeo/cgeo)<br>
[![Issue Stats](http://issuestats.com/github/cgeo/cgeo/badge/issue)](http://issuestats.com/github/cgeo/cgeo)<br>
[![Codacy Badge](https://api.codacy.com/project/badge/grade/3256314c8ba8457b9639bd2d4f4e7c91)](https://www.codacy.com/app/cgeo/cgeo)<br>

## Get the source

Fork the [project source code](https://github.com/cgeo/cgeo), make changes to your clone and [create a pull request](https://help.github.com/articles/using-pull-requests) afterwards.

### Branches

- **master** is for development of new features. Nightly builds are created from this branch.
- **release** is for all bug fixes of already existing features. So if a bug is reported in released version, it should be fixed on this branch (and merged to master afterwards).

A more complex bugfix can be first tested against the `master` branch and integrated in the nightly builds, while kept compatible with the `release` branch for a later integration.
Such a procedure is [described in the wiki](https://github.com/cgeo/cgeo/wiki/How-to-get-a-bug-fix-into-the-release).

## Setting up an IDE

Make sure to use Java 8 for your IDE and build process. Some of the involved tools require it, even though the source code is Java 7 only.

### Eclipse
- Install an Eclipse distribution for your OS from http://eclipse.org/downloads/ (you may choose the Java developers distribution).
- Start Eclipse, choose any directory as workspace. Close the welcome screen, if it appears.
- After the workbench has started, select File | Import | Install | Install Software Items From File and select a locally downloaded copy of https://github.com/cgeo/cgeo/tree/master/main/project/eclipse%20installation/cgeo%20eclipse%20components.p2f. This way you can easily install all necessary plugins.
- After forking the project you should import the Eclipse projects in your workspace with File | Import | Projects from Git.

Please be warned, we might remove the support for development in Eclipse, once we switch to a gradle based build.

### Android Studio (or IntelliJ IDEA)
- Install Android Studio from https://developer.android.com/sdk/index.html
- On first start, choose to clone a project from version control, and choose github afterwards. Supply your credentials.
- Android Studio should detect that gradle is used for building cgeo. If it complains that this is not a gradle project, then close the project. Choose "Import project" and select the `build.gradle` or `settings.gradle` in the root directory of the git repository.

## Build

### Prerequisites

- [Android SDK](http://developer.android.com/sdk) (latest version) including Google APIs V19, Google APIs V22, Google repository and Android support repository
- If you use Microsoft Windows, [Google USB Driver](http://developer.android.com/sdk/win-usb.html) to install the application on the smartphone
- You need to provide several API keys for compiling the app. See next section for details.

### API keys
Copy [`main/templates/keys.xml`](https://github.com/cgeo/cgeo/blob/master/main/templates/keys.xml) to `main/res/values/`. Then edit `main/res/values/keys.xml` and insert several keys (see comments in the file). Most important is the Google Maps API v1 key. You can leave it empty, but then Google Maps doesn't work. Google doesn't hand out new keys for Google Maps v1, you have to use an existing one.

Request your personal API key for the various [OpenCaching](http://www.opencaching.eu/) sites we support. If you leave these blank, then those networks will remain disabled.
* [opencaching.de OKAPI signup](http://www.opencaching.de/okapi/signup.html)
* [opencaching.pl OKAPI signup](http://www.opencaching.pl/okapi/signup.html)
* [opencaching.ro OKAPI signup](http://www.opencaching.ro/okapi/signup.html)
* [opencaching.nl OKAPI signup](http://www.opencaching.nl/okapi/signup.html)
* [opencaching.us OKAPI signup](http://www.opencaching.us/okapi/signup.html)
* [opencaching.org.uk OKAPI signup](http://www.opencaching.org.uk/okapi/signup.html)

### Building with gradle

Run `gradlew` from the root directory of the git repository. That will install the necessary build framework and display how to build cgeo.

### Building with Ant (deprecated)

1. copy `./templates/private.properties` to `./`
2. edit `private.properties` (see comments in the file)
3. copy `./main/templates/local.properties` to `./main/`
4. copy `./main/templates/local.properties` to `./tests/`
5. edit `local.properties` (see comments in the file)
6. copy `local.properties` to all other projects (currently android-support-v7-appcompat, google-play-services_lib, mapswithme-api, showcaseview)
7. copy `local.properties` to cgeo-calendar and cgeo-contacts if you plan to hack on the plugins

Run one of the following commands in `./main`

    ant help
    ant clean
    ant debug
    ant release

or use the Ant view of Eclipse

### Debugging

In Eclipse, create a Debug Configuration for an Android Application using the menu Run | Debug Configurations

### Testing

The Test classes can be found in the project test. Test classes should be located in the same package as
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
- Developer chat: [#cgeo on freenode.net](https://webchat.freenode.net/?channels=%23cgeo)
