### Advanced filtering system (experimental)
- Introducing a new filtering system to c:geo, supporting flexible, combinable and storable filters
- Available in both cache lists and map view
- This is work in progress, stay tuned!
- The old filter systems are still available (for the time being), but will be superseeded by the new system at some point in time.

### Map
- New: On creating a user-defined cache while displaying a map from a list: Offer user to store new cache in current list (instead of default list for user-defined caches)
- New: Separate "own" and "found" filters in map quick settings
- Change: Additionally show cache name in poup details

### Cache details
- New: Make use of google translate in-app translation popup
- New: Allow changing the assigned icon in cache details popup via long click (stored caches only)

### Downloader
- Change: Downloads will now completely happen in background, a notification is shown
- Change: Files downloaded successfully will automatically overwrite existing files having the same name
- Change: If a map requires a certain theme which is not installed yet, c:geo will automatically download and install that theme as well

### Other
- Change: We've completely reworked the internal technical aspects c:geo theming to be able to make use of some more modern components provided by Android. This will have a couple of side-effects, some of them unintended. Please report any errors or glitches either on our [GitHub page](https://www.github.com/cgeo/cgeo/issues) or by contacting support.
- New: Support day / night mode from system (optional)
- New: Download bookmark lists from geocaching.com - see "Lists / pocket queries" in main menu
- New: Ignore capability for geocaching.su
- Change: Removed no longer maintained RMAPS navigation app
