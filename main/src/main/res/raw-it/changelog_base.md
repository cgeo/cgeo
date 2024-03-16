### Mappa
- Nuovo: [Mappa Unificata](https://github.com/cgeo/cgeo/wiki/UnifiedMap) (beta), vedi [Impostazioni => Sorgenti Mappa => Mappa Unificata](cgeo-setting://featureSwitch_useUnifiedMap)
- Nuovo: Evidenzia i download in corso nel gestore download
- Nuovo: Mostra lo stato trovato sulle icone dei cache
- Nuovo: Con tocco prolungato è possibile tracciare una linea che unisca un cache con i suoi waypoint
- Cambio: Mostra dettagli cache/waypoint in modo non bloccante
- Nuovo: Facoltativamente mantiene file OAM temporanei (scaricatore di mappe, utile quando si utilizzano i file POI con altre applicazioni)
- Corretto: Errore download tema Hylly 404
- Corretto: le informazioni di elevazione non rispettano l'impostazione "usa unità imperiali"

### Dettagli del cache
- Cambio: "Attiva/disattiva parlato" è diventato un interruttore
- Cambio: aumento della lunghezza massima del log per geocaching.com
- Corretto: non è possibile caricare note personali più lunghe sui siti opencaching
- Nuovo: Modifica/elimina i propri log
- Nuovo: Proiezione Waypoint con variabili
- Cambio: Limita la selezione di immagini ai tipi jpg, png, gif
- Nuovo: Nuova formula CHARS per selezionare più caratteri singoli
- Corretto: risultato errato per la funzione TRUNC con numeri negativi
- Corretto: le formule che iniziano con una variabile simile a un marcatore emisfero non possono essere calcolate

### Generale
- Toccando la notifica sul download apre "download in attesa"
- Cambio: l'utilizzo come sfondo non richiede più l'autorizzazione READ_EXTERNAL_STORAGE
- Nuovo: layout a due colonne per le impostazioni in modalità orizzontale
- Corretto: Ripristino dei backup senza informazioni valide di data/ora
- Nuovo: Includi i trackfiles attivi nel backup
- Nuovo: Integrato addon dei contatti c:geo (addon esterno non più richiesto)
- Corretto: Tipo di log predefinito per tracciabile rielaborato
- Corretto: Tracciabile mancante nell'inventario del cache (modifica del sito)
- Corretto: Simbolo stella mancante per i filtri memorizzati modificati
- Corretto: Il campo di ricerca per parola chiave visualizza "GC" dopo aver eseguito una ricerca
- Fix: Internal crash in routing calculation
- Fix: Downloading bookmark lists returns empty list (website change)

### Changes since current beta version
- New: Show images linked in "personal note" in Images tab
- Change: Simplify long-tap action in cache details and trackable details
- New: Switch to set found state of Lab Adventures either manually or automatically
- New: List selection dialog: Auto-group cache lists having a ":" in their name
- New: Smoother scaling of log images
- New: "Edit Personal Note" from cache info sheet
