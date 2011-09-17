[c:geo](http://www.cgeo.org) is a simple yet powerful unofficial geocaching client for Android devices. In contrast to other similar applications, c:geo doesn't require a web browser nor file exports. You can just go geocaching with your phone and without any home preparation or worries. Of course, you can go without paying - it's free.

Get the source
--------------

Fork the project source code on [github](https://github.com/cgeo/c-geo-opensource):

	git clone git://github.com/cgeo/c-geo-opensource.git

Set up Eclipse
--------------

After forking the project you should import the two Eclipse projects in your workspace with File | Import | Projects from Git (requires the EGit Plugin)
Build
-----

### Prerequisites ###

- [Android SDK](http://developer.android.com/sdk) 2.2 including Google APIs V8
- [Google USB Driver](http://developer.android.com/sdk/win-usb.html) to install the application on the smartphone
- [Ant](http://ant.apache.org) 1.6.0+ for building c:geo on the command line (not necessary when using only Eclipse)

### Structure ###

c:geo sources and executables are located in the `main` directory. Tests are located in the `tests` directory.

### Known limitations ###

If the workspace directory name contains a space and leads to errors in the -dex Ant target, then you need to set the property "basedir" in your `local.properties` to the 8.3 name of the directory where this script is located on your disk.

### Configuration ###

1. copy `./main/templates/private.properties` to `./main/`
2. edit `private.properties` (see comments in the file)
3. copy `./main/templates/local.properties` to `./main/`
4. edit `local.properties` (see comments in the file)
5. copy `./main/templates/mapsapikey.xml` to `./main/res/values/`
6. edit `./main/res/values/mapsapikey.xml` and insert your Maps API key (see comments in the file)

### Building with Ant ###

Run one of the following commands in `./`

    ant help
    ant clean
    ant debug
    ant release

or use the Ant view of Eclipse

### Debugging ###

1. Add android:debuggable="true" in the application settings of `AndroidManifest.xml`
2. In Eclipse, create a Debug Configuration for an Android Application using the menu Run->Debug Configurations

### Testing ###

The Test classes can be found in the package cgeo.geocaching.test (within the cgeo-os-test project)
Every class can be "Run As" (or "Debug As") an "Android JUnit Test" from Eclipse.
Further information can be found at http://developer.android.com/guide/topics/testing/testing_android.html

License
-------

c:geo is distributed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Contact
-------

- original author: Radovan Paska aka carnero
- questions: support@cgeo.org
- website: http://www.cgeo.org/
- support: support@cgeo.org
- twitter: http://twitter.com/android_gc
- facebook: http://www.facebook.com/android.geocaching
