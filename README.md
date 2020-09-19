[c:geo](https://www.cgeo.org) is an open source, full-featured, always ready-to-go client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.
It does not require a web browser or exports - just download and start right away.

## You want to contribute?

Perfect! Please **tell us in the [issue tracker](https://github.com/cgeo/cgeo/issues) before hacking** on your great new feature. It would be bad if you have implemented something great, but we can't include it because it doesn't fit the remaining architecture and code. You might also want to chat with the developers on channel #cgeo on the freenode IRC network.

### Starting points for contribution

You can also take a look onto the [project page](https://github.com/cgeo/cgeo/projects) of our repository.
For example we have a collection of [urgent issues](https://github.com/cgeo/cgeo/projects/6) and a list of [beginner topics](https://github.com/cgeo/cgeo/projects/7) which collects issues, which might be suitable for your first contribution.

## Project status

[![Build Status](https://ci.cgeo.org/job/cgeo%20continuous%20integration/badge/icon)](https://ci.cgeo.org/job/cgeo%20continuous%20integration/)<br>
[![Codacy Badge](https://api.codacy.com/project/badge/grade/3256314c8ba8457b9639bd2d4f4e7c91)](https://www.codacy.com/app/cgeo/cgeo)<br>
[![Crowdin](https://badges.crowdin.net/cgeo/localized.svg)](https://crowdin.com/project/cgeo)

## Get the source

Fork the [project source code](https://github.com/cgeo/cgeo), make changes to your clone and [create a pull request](https://help.github.com/articles/using-pull-requests) afterwards.

### Branches

- **master** is for development of new features. Nightly builds are created from this branch.
- **release** is for all bug fixes of already existing features. So if a bug is reported in released version, it should be fixed on this branch (and merged to master afterwards).

Note: Regular merging of **release** to **master** (after changes have been done on **release**) is highly recommended to avoid unnecessary merge conflicts later on.

A more complex bugfix can be first tested against the `master` branch and integrated in the nightly builds, while kept compatible with the `release` branch for a later integration.
Such a procedure is [described in the wiki](https://github.com/cgeo/cgeo/wiki/How-to-get-a-bug-fix-into-the-release).

## Setting up an IDE

The standard IDE for Android projects is Android Studio, which is based on IntelliJ IDEA.
We use it for the development of c:geo.
It is also possible to use eclipse for writing code/testing and Android Studio for building. This is not supported officially anymore.

Details for setting up the IDE is described in the wiki (https://github.com/cgeo/cgeo/wiki/IDE).

## Build

### Prerequisites

- [Android SDK](https://developer.android.com/sdk) (latest version) including Google APIs (at least) V26, Google repository and Android support repository. (File => Settings, Appearance & Behaviour => System Settings => Android SDK, Check "Show Package Details" on "SDK Platforms" tab and check subpackages as needed.)
- If you use Microsoft Windows, [Google USB Driver](https://developer.android.com/sdk/win-usb.html) to install the application on the smartphone.
- You need to provide several API keys for compiling the app. See next sections for details.

### API keys

For the full usability of c:geo you need some API keys - for Google Maps and the opencaching sites.
You can leave all entries in the configuration empty, but then Google Maps and the Opencaching sites don't work.

For using the Google Maps function it is necessary to have a Google Maps API v2 key. For this follow
* [Maps SDK for Android: Get an API Key](https://developers.google.com/maps/documentation/android-sdk/get-api-key)

The key itself is free, and you don't have to enter any credit card info, although the web form seems to force you to.

Also to be able to use Google Maps you need to use a Google API enabled image, so make sure to select the right image for your emulator/device, otherwise Google Maps won't be offered as map provider in c:geo.

Request your personal API key for the various [OpenCaching](https://www.opencaching.eu/) sites we support. If you leave these blank, then those networks will remain disabled.
* [opencaching.de OKAPI signup](https://www.opencaching.de/okapi/signup.html)
* [opencaching.pl OKAPI signup](https://www.opencaching.pl/okapi/signup.html)
* [opencaching.ro OKAPI signup](https://www.opencaching.ro/okapi/signup.html)
* [opencaching.nl OKAPI signup](https://www.opencaching.nl/okapi/signup.html)
* [opencaching.us OKAPI signup](https://www.opencaching.us/okapi/signup.html)
* [opencache.uk OKAPI signup](https://www.opencache.uk/okapi/signup.html)

To obtain API key for [geocaching.su](https://geocaching.su) you need to request access from [administration](https://geocaching.su/?pn=1), keys are generated manually on request.

### API keys installation

In c:geo we have a semi-automatic configuration
1. copy `./templates/private.properties` to `./`
2. edit `private.properties` with your keys
3. on the gradle build the `./main/res/values/keys.xml` is created and filled with the data from `private.properties`

The third point works only if the file `keys.xml` does not exist.
When changing your API keys you have to delete the `keys.xml` file

If you want to fill the `keys.xml` by hand copy `./main/templates/keys.xml` to `./main/res/values/`.
Then edit the copied `keys.xml`. For each key, replace the value starting with @ and ending with @ (inclusive) with the key. If a key is missing, remove the value and the leading and trailing @).

### Building with gradle

Run `gradlew` from the root directory of the git repository. That will install the necessary build framework and display how to build cgeo. `gradlew assembleBasicDebug` might be a good start.
Alternatively you can use "make" in Android Studio ("Build" => "Make Project")

To be able to create an installable Android package (APK) you need to create a signing key first. In Android Studio go to "Build" => "Generate Signed Bundle & APK", select "APK" and follow the instructions. You will create a key storage and an project specific key. Enter path and access information to those in file cgeo/private.properties.

### Testing

The Test classes can be found in the project test. Test classes should be located in the same package as
the class under test.
Every class can be run with `Run '<class name>'` or debugged with `Debug '<class name>'`) as an [Android JUnit Test](https://developer.android.com/training/testing/fundamentals.html).
To run all tests use the same `Run 'Tests in <package name>'` menu item from the context menu of a package in the test project.

For tests to run successfully you need to configure c:geo on the emulator that runs the test with a valid geocaching.com account. In order for all tests to be successfull the account needs to be a premium member.

Tests may also be launched from command line. Use `gradlew assembleBasicDebug` from the root directory of the git repository.

## Deploying the app locally for testing purposes

Android Studio needs to be configured for which device(s) c:geo to deploy to. Use "run" => "run" (2nd entry with this heading).
You can create several profiles for either a physical device attached via USB as well as virtual devices run in an emulator. (If the emulator is not installed yet do so via File => Settings, Appearance & Behaviour => System Settings => Android SDK, tab "SDK Tools", check "Android Emulator" and apply.)

## License

c:geo is distributed under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Contact

- Website: https://www.cgeo.org/
- Support: support@cgeo.org
- Twitter: https://twitter.com/android_gc
- Facebook: https://www.facebook.com/android.geocaching
- Google Play: https://play.google.com/store/apps/details?id=cgeo.geocaching
- Live status: https://status.cgeo.org/
- Developer chat: [#cgeo on freenode.net](https://webchat.freenode.net/?channels=%23cgeo)
