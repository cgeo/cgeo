### Mappa
- Nuovo: "Modifica nota personale" dalla pagina info del cache
- Corretto: Waypoint non filtrati sulla mappatura di una singola cache (UnifiedMap)
- Nuovo: Supporto a fornitore di tasselli definiti dall'utente
- Corretto: aggiorna i dati della mappa dopo l'apertura / chiusura della finestra di dialogo delle impostazioni (UnifiedMap)
- Nuovo: Mostra/nascondi edifici 2D/3D (UnifiedMap OSM)
- Nuovo: Cache store/refresh dal popup spostato in background
- Modifica: cerca le coordinate: mostra la direzione e la distanza dall'obiettivo e non dalla posizione corrente
- Nuovo: Indicatore grafico D/T nel foglio informazioni cache
- Corretto: Bussola nascosta quando la barra dei filtri è visibile (UnifiedMap)

### Dettagli del cache
- Nuovo: Mostra le immagini collegate in "note personali" nella scheda Immagini
- Cambio: semplificato il tocco lungo nei dettagli dei cache e dei tracciabili
- Novità: ridimensionamento delle immagini del log
- Cambia: Cambio dell1'icona "modifica liste" da matita a lista + matita
- Fix: vanity function failing on long strings
- Corretto: errata priorità nel parsing della formula backup
- Change: Allow larger integer ranges in formulas (disallow usage of negation)
- New: Allow user-stored cache images on creating/editing log
- Fix: Spoiler images no longer being loaded (website change)

### Generale
- Nuovo: Impostazione manuale o automatica per lo stato 'trovato' di Lab Adventures
- Nuovo: Finestra di selezione elenchi: Gruppi automatici di elenchi di cache con un ":" nel loro nome
- Cambiamento: utilizzo di OSM Nominatum come geocoder fallback, in sostituzione di MapQuest geocoder (che non funziona più per noi)
- Cambiamento: aggiornato BRouter intergrato alla versione 1.7.4
- Nuovo: Lettura delle informazioni di elevazione dalla traccia durante l'importazione
- Nuovo: API a Locus che supporta ora la dimensione del cache "virtuale"
- Corretto: risultati della ricerca per una posizione non più ordinata per distanza dalla posizione di destinazione
- Nuovo: filtro "Coordinate corrette"
- Modifica: Aggiornato targetSDK a 34 per soddisfare i requisiti imminenti del Play Store
- New: Added "none"-entry to selection of routing profiles
- Modifica: Migliorata la descrizione della funzione "manutenzione" (rimuove i dati orfani)
- Nuovo: Mostra avvisi quando si verifica l'errore HTTP 429 (troppe richieste)
- Corretto: Flickering in aggiornamento della lista cache
- New: Allow display of passwords in connector configuration
- Fix: Search for geokretys no longer working when using trackingcodes
