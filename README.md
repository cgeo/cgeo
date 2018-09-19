[c:geo](http://www.cgeo.org) is an open source, full-featured, always ready-to-go client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.
It does not require a web browser or exports - just download and start right away.

## You want to contribute?

Perfect! Please **tell us in the [issue tracker](https://github.com/cgeo/cgeo/issues) before hacking** on your great new feature. It would be bad if you have implemented something great, but we can't include it because it doesn't fit the remaining architecture and code. You might also want to chat with the developers on channel #cgeo on the freenode IRC network.

### Starting points for contribution

You can also take a look onto the [project page](https://github.com/cgeo/cgeo/projects) of our repository.
For example we have a collection of [urgent issues](https://github.com/cgeo/cgeo/projects/6) and a list of [beginner topics](https://github.com/cgeo/cgeo/projects/7) which collects issues, which might be suitable for your first contribution.

## Project status

[![Build Status](http://ci.cgeo.org/job/cgeo%20continuous%20integration/badge/icon)](http://ci.cgeo.org/job/cgeo%20continuous%20integration/)<br>
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
We removed the support for development in Eclipse, once we switched to a gradle based build.
The cause for this is that Google dropped the ADT for eclipse support
(https://android-developers.blogspot.de/2016/11/support-ended-for-eclipse-android.html).

Still, there are developers in the project that uses both eclipse and Android Studio (or IntelliJ IDEA).
They use eclipse for writing code/testing and Android Studio (or IntelliJ IDEA) for building.

Here are instructions on how to setup the eclipse environment and clone the repositories as part of that setup.

- Install the Eclipse installer for your OS from http://eclipse.org/downloads/. Do **not** choose any of the pre-made distributions like "Eclipse IDE for Java developers".
- Start the installer, switch to advanced mode. On the first page of the wizard choose "Eclipse IDE for Java developers" and use "Next".
- On the second wizard page use the "Plus" icon (you will have to search for it for a while), select the github catalog, and add the URI https://github.com/cgeo/cgeo/raw/master/main/project/eclipse%20installation/cgeo.setup. Now select the newly create tree node "cgeo" and use next.
- On the third wizard page add your github user name and password. Adapt the "root installation folder", the folder cgeo will be created in there and everything will be downloaded and copied to the cgeo folder. That means that if you have a projects directory where you store many projects, it is a good candidate for root installation folder. The cgeo folder will ultimately consist of two folders, eclipse (that contains a whole eclipse installation) and git (that contains some git repository clones). Use Next and Finish to start the download of necessary Eclipse plugins and the cloning of the repositories.
- In eclipse, the project will be uncompilable until you have copied the files keys.xml from main/templates to main/res/values (you need to do this because eclipse fails to create the file automatically from private.properties as Android Studio do). You have to change all values starting with @ and ending with @ (inclusive) with respective keys. If a key is missing, remove the respective value (together with the leading and trailing @).

### Android Studio (or IntelliJ IDEA)
- Install Android Studio from https://developer.android.com/sdk/index.html
- On first start, choose to clone a project from version control, and choose github afterwards. Supply your credentials.
- Android Studio should detect that gradle is used for building cgeo. If it complains that this is not a gradle project, then close the project. Choose "Import project" and select the `build.gradle` or `settings.gradle` in the root directory of the git repository.
- c:geo uses [Checkstyle](http://checkstyle.sourceforge.net/) to verify that coding standards are followed throughout the project. To see violations directly in Android Studio you need to install the `CheckStyle-IDEA` Plugin and configure the `checkstyle.xml` file available in the project root directory.

## Build

### Prerequisites

- [Android SDK](http://developer.android.com/sdk) (latest version) including Google APIs V23, Google repository and Android support repository.
- If you use Microsoft Windows, [Google USB Driver](http://developer.android.com/sdk/win-usb.html) to install the application on the smartphone.
- You need to provide several API keys for compiling the app. See next section for details.

### API keys
Copy [`main/templates/keys.xml`](https://github.com/cgeo/cgeo/blob/master/main/templates/keys.xml) to `main/res/values/`. Then edit `main/res/values/keys.xml` and insert several keys (see comments in the file). Most important is the Google Maps API v1 key. You can leave it empty, but then Google Maps doesn't work. Google doesn't hand out new keys for Google Maps v1, you have to use an existing one.

Request your personal API key for the various [OpenCaching](http://www.opencaching.eu/) sites we support. If you leave these blank, then those networks will remain disabled.
* [opencaching.de OKAPI signup](http://www.opencaching.de/okapi/signup.html)
* [opencaching.pl OKAPI signup](http://www.opencaching.pl/okapi/signup.html)
* [opencaching.ro OKAPI signup](http://www.opencaching.ro/okapi/signup.html)
* [opencaching.nl OKAPI signup](http://www.opencaching.nl/okapi/signup.html)
* [opencaching.us OKAPI signup](http://www.opencaching.us/okapi/signup.html)
* [opencache.uk OKAPI signup](http://www.opencache.uk/okapi/signup.html)

### Building with gradle

Run `gradlew` from the root directory of the git repository. That will install the necessary build framework and display how to build cgeo. `gradlew assembleBasicDebug` might be a good start.

### Debugging

In Eclipse, create a Debug Configuration by selecting the cgeo application (inside the cgeo) folder and press F11. Then choose to run as Android Application.

### Testing

The Test classes can be found in the project test. Test classes should be located in the same package as
the class under test.
Every class can be run with `Run '<class name>'` or debugged with `Debug '<class name>'`) as an [Android JUnit Test](https://developer.android.com/training/testing/fundamentals.html).
To run all tests use the same `Run 'Tests in <package name>'` menu item from the context menu of a package in the test project.

For tests to run successfully you need to configure c:geo on the emulator that runs the test with a valid geocaching.com account. In order for all tests to be successfull the account needs to be a premium member.

Tests may also be launched from command line. Use `gradlew assembleBasicDebug` from the root directory of the git repository.

## License

c:geo is distributed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Contact

- Website: http://www.cgeo.org/
- Support: support@cgeo.org
- Twitter: http://twitter.com/android_gc
- Facebook: http://www.facebook.com/android.geocaching
- Google+: https://plus.google.com/105588163414303246956
- Google Play: https://play.google.com/store/apps/details?id=cgeo.geocaching
- Live status: http://status.cgeo.org/
- Developer chat: [#cgeo on freenode.net](https://webchat.freenode.net/?channels=%23cgeo)
