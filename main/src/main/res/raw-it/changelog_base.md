### Mappa unificata
Benvenuto nella nostra nuova implementazione della mappa, internamente chiamata "Mappa Unificata". Questo è il risultato di quasi due anni di lavoro del team c:geo per una implementazione della mappa completamente rinnovata. Il motivo per cui abbiamo iniziato questo, era che le vecchie implementazioni della mappa erano diventate sempre più difficili da mantenere (più o meno) in sintonia con le funzionalità, con alcune parti di codice di dieci anni (o più).

Con la Mappa Unificata abbiamo cercato di ottenere la stessa esperienza utente su tutti i diversi tipi di mappe (ove possibile), modernizzando e unificando l'architettura interna.

La Mappa Unificata offre (quasi) tutte le caratteristiche che le nostre vecchie implementazioni di mappa hanno, ma offrono un paio di funzionalità aggiuntive:

- Rotazione della mappa basata su OpenStreetMap (online e offline)
- Scala frazionaria per mappe basate su OpenStreetMap
- Cluster popup per Google Maps
- Le fonti delle mappe che non servono possono essere nascoste
- Grafico di elevazione per percorsi e tracce (con un tocco sul percorso)
- Switch between lists directly from map (or by long-tapping on map icon)

La Mappa Unificata ha raggiunto lo stato beta adesso, così abbiamo deciso di renderla la nostra mappa predefinita per tutti gli utenti delle versioni notturne.

Tutto dovrebbe funzionare, ma ci possono ancora essere (e ce ne saranno) alcuni bug. In caso di necessità è possibile passare dalle nuove alle vecchie implementazioni della mappa (vedi impostazioni - sorgenti della mappa), ma vorremmo davvero che si provasse quella nuova. Si prega di segnalare qualsiasi bug trovato al supporto ([support@cgeo.org](mailto:support@cgeo.org)) o [c:geo su GitHub](github.com/cgeo/cgeo/issues). Ogni feedback è benvenuto!

---

Altre modifiche:

### Mappa
- Nuovo: Evidenzia i download in corso nel gestore download
- Nuovo: Mostra lo stato trovato sulle icone dei cache
- Nuovo: Con tocco prolungato è possibile tracciare una linea che unisca un cache con i suoi waypoint
- Cambio: Mostra dettagli cache/waypoint in modo non bloccante
- New: Optionally keep temporary OAM files (map downloader, useful when using POI files with other apps)

### Dettagli del cache
- Cambio: "Attiva/disattiva parlato" è diventato un interruttore
- Cambio: aumento della lunghezza massima del log per geocaching.com
- Corretto: non è possibile caricare note personali più lunghe sui siti opencaching

### Generale
- Toccando la notifica sul download apre "download in attesa"
- Cambio: l'utilizzo come sfondo non richiede più l'autorizzazione READ_EXTERNAL_STORAGE
- Nuovo: layout a due colonne per le impostazioni in modalità orizzontale
- Fix: Restore of backups without valid date/time info
