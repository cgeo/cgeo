
### Mapa
- New: Add support for hillshading on OSM
- Fix: Manually copied waypoints of a cache not all being displayed

### Szczegóły skrytki
- Change: Show loading indicator in log activity while retrieving required data is ongoing
- Fix: nbsp; not considered as space in formula parsing
- Fix: Log image labelled "Image 1" even if only a single image added
- Fix: Extra waypoints created on personal note formula parsing
- Fix: Variables not created on copying formula to different cache
- Fix: (Experimental gallery) Use image cache for log/spoiler images
- New: GEOCODE log template placeholder

### Ogólne
- New: Support generic downloads for "mf-v4-map"- and "mf-theme"-prefixed links (no automatic updates supported)
- New: Maintenance function reindexes database
- New: Automatic performance optimization of database every 90 days (reindex)
- Fix: Handle missing fine location permission
- Change: Website language switch removed
- Poprawka: Usunięte skrytki zdefiniowane przez użytkownika nie były usuwane przy ponownym uruchomieniu c:geo
- New: View for pending downloads
- New: Append cache name / list name to file name on GPX export
- Change: Removed "Identify as Android browser" setting
- New: Check pending downloads (maps / routing data) on startup
- Fix: Filter bar not readable in light mode
- New: Allow selection of files to download
- New: Status filter for DNF
- New: Display elevation on home screen (if available)
