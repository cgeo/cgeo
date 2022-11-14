
### Avviso: modifica a livello API
A causa delle restrizioni imminenti nel Play Store abbiamo aggiornato il livello di API Android. Questo non dovrebbe influenzare l'utilizzo di c:geo, e dovrebbe ancora essere eseguibile da Android 5 in poi, ma se noti eventuali irregolarità, contattaci su support@cgeo.org

### Mappa
- Nuovo: Consentito il cambio del nome visualizzato della traccia

### Dettagli del cache
- Fix: Log image labelled "Image 1" even if only a single image added
- Nuovo: segnaposto modello di log GEOCODE
- Nuovo: Supporto di base alla formattazione HTML per le liste di definizione (dl/dt/dd)
- New: Open zoomable image view when tapping on listing images
- Fix: Open links in listings in integrated web view (if enabled)
- Modifica: Render della descrizione del cache in background e limitazione della lunghezza a 50.000 caratteri per impostazione predefinita
- Cambiato: Disabilitata connessione al servizio GCVote a causa di gravi problemi di prestazione - È possibile riattivarlo manualmente andando su Impostazioni - Servizi - GCVote
- Nuovo: Log dei cache: viene mantenuta l'ultima azione per quanto riguarda i tracciabili

### Generale
- Nuovo: Mostra i download in sospeso
- Nuovo: Viene aggiunto il nome del cache/elenco a quello del file nell'esportazione GPX
- Modifica: Rimossa l'impostazione "Identifica come browser Android"
- Nuovo: Controllo dei download in sospeso (mappe/dati di routing) all'avvio
- Nuovo: Consente la selezione dei file da scaricare
- Nuovo: filtro di stato per DNF
- Nuovo: Mostra la quota nella schermata home (se disponibile)
- Nuovo: Permesso l'inserimento manuale di valori nei filtri usando i cursori
- New: Enable upload of modified coordinates for caches imported from GPX file, when cache has waypoint of type "ORIGINAL"
- Change: Improve filter status line text
- Cambio: Usato un colore meglio leggibile nei titoli per i nomi di cache archiviati e rimossa la colorazione dalla pagina dettagli cache
