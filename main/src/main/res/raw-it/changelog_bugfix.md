##
- Fix: Log length check counting some characters twice
- Fix: Adapt to hylly website change
- New: Additional theming options for Google Maps

##
- Corretto: cache che non si caricano dopo aver abilitato la mappa live (UnifiedMap)
- Corretto: Manca l'opzione 'usa la lista corrente' per creare un cache definito dall'utente (UnifiedMap)
- Corretto: Bussola nascosta dietro le viste a distanza (UnifiedMap)
- Corretto: i dettagli della cache scorrono nell'intestazione della pagina dopo aver modificato la nota personale
- Nuovo: Mostra la data dell'evento al selettore della cache
- Corretto: Login alla piattaforma OC non riconosciuto dalla procedura guidata di installazione
- Corretto: Routing non funziona di default dopo nuova installazione
- Corretto: Info foglio barra degli strumenti nascosta in modalità orizzontale anche su dispositivi di grandi dimensioni
- Corretto: "segui la mia posizione" ancora attiva dopo lo zoom con pan (UnifiedMap)
- Corretto: i singoli percorsi esportati come traccia non possono essere letti dai dispositivi Garmin
- Corretto: Caricamento dei trackables dal database interno che fallisce in determinate condizioni
- Fix: Route to navigation target not recalculated on routing mode change
- Fix: Error while reading available trackable log types

##
- Corretto: link dei tracciabili con il parametro TB non funzionante
- Nuovo: Aggiunto suggerimento per la ricerca disabilitata per parole chiave per gli utenti base
- Corretto: la registrazione dei tracciabili non funziona di nuovo (modifica del sito web)
- Corretto: informazioni di elevazione ruotano con il marcatore di posizione
- Corretto: Nome utente non rilevato durante il login quando contiene determinati caratteri speciali

##
- Corretto: Mostra/nascondi i waypoint non funziona correttamente se attraversano i limiti dei waypoint (UnifiedMap)
- Corretto: i log di cache o tracciabili non funzionano più (modifica del sito)
- Corretto: L'eliminazione dei propri log non funziona

##
- Corretto: contatore dei "trovati" non rilevato in determinate situazioni a causa di cambiamenti del sito
- Corretto: Errore durante l'apertura della mappa con nomi dei file della traccia vuoti
- Corretto: la rotazione automatica della mappa è ancora attiva dopo aver reimpostato la bussola rosa (UnifiedMap)
- Risolto: Bussola mancante nella modalità di autorotazione su Google Maps (UnifiedMap)
- Corretto: i log dei tracciabili non possono essere caricati a causa di modifiche del sito
- Cambio: combina l'elevazione + informazioni sulle coordinate nel menu a lungo tocco della mappa in una singola "posizione selezionata" + mostra la distanza dalla posizione corrente

##
- Nuovo: Eliminazione dei log offline usando il menu contestuale
- Corretto: L'eliminazione del log offline non funzionava in determinate condizioni
- Corretto: il nome del filtro veniva perso nel cambio rapido del filtro
- Cambio: Ordina i trackfiles per nome
- Modifica: Salva azione tracciabile anche per i log offline
- Corretto: Passaggio della mappa alle coordinate 0,0 con il cambiamento del tipo di mappa (UnifiedMap)
- Corretto: Waypoint target che torna alla cache come destinazione (UnifiedMap)
- Corretto: "Archiviare" un cache senza selezionare un elenco
- Corretto: Errore di accesso su geocaching.com a causa del cambiamento del sito web
- Cambio: Mostra le informazioni di elevazione sotto l'indicatore di posizione (se attivato)
- NOTA: Ci sono ulteriori problemi, dovuti alle recenti modifiche del sito web geocaching.com, che non sono ancora stati risolti. Ci stiamo lavorando. Vedi la nostra [ pagina di stato](https://github.com/cgeo/cgeo/issues/15555) per i progressi attuali.
