This setup is intended to be used with Eclipse Neon or better only. Do not use older versions.

Download the Eclipse Neon installer from eclipse.org. It depends on your current operating system, therefore we cannot give a direct link.
Start it and switch to the advanced mode using the menu on the top right.
Confirm the question about placing the installer in a permanent location, otherwise your settings cannot be stored.

After the installer has restarted, select the "Eclipse platform" on the first wizard page and use Next.
On the second page, use the green plus button at the top, enter the following URL in the text field:
https://raw.githubusercontent.com/cgeo/cgeo/master/main/project/eclipse%20installation/cgeo.setup

When using next, you are asked to customize your cgeo Eclipse installation.
All values have sensible defaults, you only need to add your github username and password.

When you now finish the wizard, it will install a minimal Eclipse and start installing additional plugins.
After a while that wizard is done and you can close it, Eclipse will restart.

After the restart the workspace is initialized, i.e. sources are cloned, projects imported etc.
You can see the progress by clicking the spinning arrows in the bottom bar of the Eclipse window.

The automated setup will be checked (and updated) every time you restart Eclipse.