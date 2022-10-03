
### Mappa
- Nuovo: Aggiunto il supporto per ombreggiatura su OSM
- Correzione: i waypoint di un cache copiati manualmente, non sono tutti visualizzati

### Dettagli del cache
- Change: Show loading indicator in log activity while retrieving required data is ongoing
- Fix: Log image labelled "Image 1" even if only a single image added

### Generale
- Nuovo: Supporto ai download generici per i link con prefisso "mf-v4-map" e "mf-theme" (aggiornamento automatico non supportato)
- New: Maintenance function reindexes database
- New: Automatic performance optimization of database every 90 days (reindex)
- Fix: Handle missing fine location permission
- Change: Website language switch removed
- Fix: Deleted user-defined caches are not deleted on c:geo restart
- New: View for pending downloads
- New: Append cache name / list name to file name on GPX export
- Change: Removed "Identify as Android browser" setting
- New: Check pending downloads (maps / routing data) on startup
- Fix: Filter bar not readable in light mode
