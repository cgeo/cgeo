### Tabella di marcia per UnifiedMap & "vecchia" mappa
c:geo, da qualche tempo, ha una nuova implementazione della mappa chiamata "UnifiedMap", che alla fine sostituirà le vecchie implementazioni di Google Maps e Mapsforge (OpenStreetMap). Questo è un avviso per informarvi sulla prossima roadmap.

UnifiedMap è stato pubblicato circa un anno fa. Supporta ancora Google Maps e OpenStreetMap (online + offline), ma in modo completamente rielaborato e con un sacco di eccitanti nuove caratteristiche che le "vecchie" mappe non supportano, alcune delle quali sono
- Rotazione delle mappe basate su OpenStreetMap (online e offline)
- Cluster popup per Google Maps
- Nascondi le fonti delle mappe che non ti servono
- Grafico di elevazione per percorsi e tracce
- Passaggio tra le liste direttamente dalla mappa
- "Modalità di guida" per mappe basate su OpenStreetMap

UnfiedMap ha dimostrato di essere stabile da un bel po' di tempo, quindi rimuoveremo le vecchie implementazioni della mappa per ridurre l'impegno nella mantenzione di c:geo.

Tabella di marcia:
- Le "vecchie" mappe saranno abbandonate - non risolveremo più i bug.
- UnifiedMap (mappa unificata) sarà resa predefinita per tutti gli utenti nell'autunno del 2025.
- Le implementazioni della "vecchia" mappa saranno rimosse nella primavera 2026.

Fino ad allora, sarà possibile passare tra le diverse implementazioni nelle impostazioni => Sorgenti mappa.

### Mappa
- Nuovo: Mostra l'area (geofence) intorno ai vari step degli Adventure Lab (UnifiedMap) - abilita "Cerchi" nelle impostazioni rapide della mappa per mostrarla
- Nuovo: Opzione per impostare cerchi con raggio personalizzato per i waypoint (opzione del menu contestuale "geofence")
- Corretto: vista mappa non aggiornata quando si rimuove un cache dalla lista attualmente visualizzata
- Corretto: numero di cache nella lista non aggiornato modificando i contenuti della lista
- Cambia: Mantieni la vista corrente nella mappatura di un elenco, se tutte le cache sono compatibili con la vista corrente
- Nuovo: Segui la mia posizione nel grafico di elevazione (UnifiedMap)
- Nuovo: abilita le azioni "vai a" / "copia in" per "mostra come elenco"
- Nuovo: Supporto per il tema Elevate Winter nel downloader di mappe
- Nuovo: Ombreggiamento Adattivo, modalità alta qualità opzionale (UnifiedMap Mapsforge)
- Nuovo: Riprogettata la finestra di dialogo delle impostazioni rapide per percorsi/tracce
- Nuovo: Tocco prolungato sull'icona della selezione della mappa per selezionare la sorgente di tasselli della mappa precedente (UnifiedMap)
- Nuovo: Consenti l'impostazione del nome visualizzato per le mappe offline (UnifiedMap)
- Nuovo: Tocco prolungato su "mappa live" per caricare i cache offline
- Nuovo: Offline hillshading per UnifiedMap (variante VTM)
- Nuovo: Supporto per mappe in background (UnifiedMap)
- Corretto: icone compatte che non tornano a grandi icone sullo zoom in modalità automatica (UnifiedMap)
- Nuovo: Azioni a lungo tocco nel foglio informativo cache: Codice GC, titolo cache, coordinate, note personali/indizio
- Cambia: cambia il foglio infosheet della cache a lungo tocco per il selettore emoji per un tocco corto per risolvere la collisione

### Dettagli del cache
- Nuovo: Traduzione offline del testo della descrizione e dei log (sperimentale)
- Nuovo: Opzione per condividere il cache con i dati utente (coordinate, nota personale)
- Corretto: Servizio vocale interrotto sulla rotazione dello schermo
- Corretto: Dettagli della cache: Elenca per la cache non aggiornata dopo aver toccato il nome della lista una rimozione della cache da quella lista
- Corretto: La nota utente si perde durante l'aggiornamento di un'adventure lab
- Modifica: i segnaposto relativi alla data di log utilizzeranno la data scelta invece della data corrente
- Nuovo: Comprimi le voci di log lunghe per impostazione predefinita

### Wherigo player
- Nuovo: Controllo lettore Wherigo integrato per le credenziali mancanti
- Modifica: Rimosso la segnalazione di bug Wherigo (poiché gli errori sono per lo più correlati alla cartuccia, devono essere risolti dal proprietario della cartuccia)
- Nuovo: Capacità di navigare in una zona usando la bussola
- Nuovo: possibilità di copiare negli appunti le coordinate del centro di un'area
- Nuovo: Imposta il centro della zona come obiettivo quando si apre la mappa (per ottenere informazioni sul percorso e sulla distanza per esso)
- Nuovo: Supporto all'apertura dei file Wherigo locali
- Cambio: il tocco prolungato su una zona sulla mappa non è più riconosciuto. Questo permette agli utenti di fare altre cose nell'area della mappa disponibile con un tocco prolungato, es.: creare un cache definito dall'utente
- Nuovo: Avviso se wherigo.com segnala la mancanza di EULA (che porta a un errore nel download della cartuccia)

### Generale
- Nuovo: pagina di ricerca ridisegnata
- Nuovo: filtro conteggio inventario
- Nuovo: Supporto delle coordinate in formato DD, DDDDDDD
- Nuovo: mostra l'ultimo nome del filtro usato nella finestra del filtro
- Nuovo: Calcolatore di coordinate: Funzione per sostituire "x" con simbolo di moltiplicazione
- Corretto: altitudine errata (non con media sul livello del mare)
- Corretto: impostazione limite di distanza nelle vicinanze non funziona correttamente per valori piccoli
- Corretto: Ordinamento di elenchi di cache per distanza discendente non funziona correttamente
- Corretto: Cache di laboratorio escluse dal filtro D/T anche con "include incerto" attivo
- Corretto: Problemi di colore con le icone del menu in modalità light
- Nuovo: Aggiungi "Rimuovi gli eventi passati" per elencare "tutti"
- Nuovo: Mostra connettore per le "cache definite dall'utente" come attive nel filtro sorgente
- Nuovo: Esportazione GPX: esportazione di registri / trackables resi opzionali
- Nuovo: Aggiunto il pulsante per eliminare i modelli di log
- Corretto: l'importazione del file di mappa locale ottiene il nome casuale della mappa
- Corretto: Download mappa con file corrotti (0 byte) per il download
- Nuovo: Aggiunte mappature per alcuni tipi di cache OC mancanti
- Nuovo: Sposta gli elenchi "usati di recente" nella finestra di selezione degli elenchi in alto premendo il pulsante "usato di recente"
- Nuovo: Condividi l'elenco dei geocodici dalla lista cache
- Modifica: "Navigazione (car)" ecc. usa il parametro "q=" invece del parametro "ll=" obsoleto
