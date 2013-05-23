[c:geo](http://www.cgeo.org) is a simple yet powerful unofficial geocaching client for Android devices. In contrast to other similar applications, c:geo doesn't require a web browser nor file exports. You can just go geocaching with your phone and without any home preparation or worries. Of course, you can go without paying - it's free.

You want to contribute?
-----------------------
Perfect! Please **tell us in the [issue tracker](https://github.com/cgeo/cgeo/issues) before hacking** on your great new feature. It would be bad if you have implemented something great, but we can't include it because it doesn't fit the remaining architecture and code. You might also want to chat with the developers on channel #cgeo on the freenode IRC network.

Get the source
--------------

Fork the [project source code](https://github.com/cgeo/cgeo), make changes to your clone and [create a pull request](https://help.github.com/articles/using-pull-requests) afterwards.

### Branches ###

- **master** is for development of new features. Nightly builds are created from this branch.
- **release** is for all bug fixes of already existing features. So if a bug is reported in released version, it should be fixed on this branch (and merged to master afterwards).

A more complex bugfix can be first tested against the `master` branch and integrated in the nightly builds, while kept compatible with the `release` branch for a later integration.
Such a procedure is [described in the wiki](https://github.com/cgeo/cgeo/wiki/How-to-get-a-bug-fix-into-the-release).

Set up Eclipse
--------------

- Install an Eclipse distribution for your OS from http://eclipse.org/downloads/ (you may choose the Java developers distribution).
- Start Eclipse, choose any directory as workspace. Close the welcome screen, if it appears.
- After the workbench has started, select File | Import | Install | Install Software Items From File and select a locally downloaded copy of https://github.com/cgeo/cgeo/tree/master/main/project/eclipse%20installation/cgeo%20eclipse%20components.p2f. This way you can easily install all necessary plugins.
- After forking the project you should import the Eclipse projects in your workspace with File | Import | Projects from Git.

Build
-----

### Prerequisites ###

- [Android SDK](http://developer.android.com/sdk) (latest version) including Google APIs V11 (although we target API 4)
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
3. copy `./main/templates/local.properties` to `./tests/`
4. edit `local.properties` (see comments in the file)
5. copy `./main/templates/mapsapikey.xml` to `./main/res/values/`
6. edit `./main/res/values/mapsapikey.xml` and insert your Maps API key (see comments in the file)

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

License
-------

c:geo is distributed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Contact
-------

- Website: http://www.cgeo.org/
- Support: support@cgeo.org
- Twitter: http://twitter.com/android_gc
- Facebook: http://www.facebook.com/android.geocaching
- Google+: https://plus.google.com/105588163414303246956
- Google Play: https://play.google.com/store/apps/details?id=cgeo.geocaching
- Live status: http://status.cgeo.org/
