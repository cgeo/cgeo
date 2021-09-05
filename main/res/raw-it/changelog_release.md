### Fixes
- "Add to watchlist" / "Remove from watchlist" failing (Website change)
- "Add to favorite" / "Remove from favorite" buttons not shown after "found" log
- Date in logbook cut off on larger fonts
- Filtering in live map for more rare cache types returning only few results

## Bugfix Release 2021.08.28

### Design
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### Dettagli del cache
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### Altro
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

## Feature Release 2021.08.15:

### Sistema di filtri avanzato
- Introduzione di un nuovo sistema di filtri in c:geo, supporto di filtri flessibili, combinabili e memorizzabili
- Disponibile sia negli elenchi dei cache che nella visualizzazione a mappa
- Nuova funzione "Ricerca per filtro"

### Mappa
- Nuovo: Alla creazione di un cache definito dall'utente, visualizzando una mappa da un elenco: propone all'utente di memorizzare il nuovo cache nell'elenco corrente (invece che nell'elenco predefinito per i cache definiti dall'utente)
- Nuovo: separati i filtri "propri" e "trovati" nelle impostazioni rapide della mappa
- Cambiato: nel poup dettagli mostra anche il nome del cache

### Dettagli del cache
- Nuovo: Utilizzo del Traduttore di Google nel popup di traduzione integrato
- Nuovo: Possibilità di modifica l'icona associata al cqche, nel popup dei dettagli del cache, tramite tocco proluganto (solo cache memorizzati)

### Downloader
- Cambiato: i download ora avverranno completamente in background, viene visualizzata una notifica
- Cambiato: I file scaricati correttamente sovrascriveranno automaticamente i file esistenti aventi lo stesso nome
- Cambiato: Se una mappa richiede un certo tema che non è installato, c:geo scaricherà e installerà automaticamente quel tema

### Altro
- Cambiato: Abbiamo completamente rielaborato gli aspetti tecnici interni nella gestione dei temi in c:geo, per essere in grado di utilizzare alcuni componenti più moderni forniti da Android. Questo avrà alcuni effetti collaterali, alcuni di loro non intenzionali. Si prega di segnalare eventuali errori o problemi sulla nostra pagina [GitHub](https://www.github.com/cgeo/cgeo/issues) o contattando il supporto.
- Nuovo: Supportata la modalità giorno / notte da sistema (opzionale)
- Nuovo: Scarica gli elenchi di segnalibri da geocaching.com - vedi "Elenchi / Pocket query" nel menu principale
- Nuovo: Ignora la capacità per geocaching.su
- Cambiato: Rimossa l'app di navigazione RMAPS non più mantenuta
- Corretto: Estrazione di waypoint dalle note personali, con lo stesso nome ma coordinate diverse
- Corretto: Errore nell'estrazione della nota utente per waypoint con un formula
- Corretto: Esporta la formula in PN invece delle coordinate per la formula completata
- Corretto: Mappa offline e cartelli temi non corretta dopo la reinstallazione e il ripristino del backup
- Corretto: La traccia/rotta non può essere aggiornata
- Corretto: Tema errato per il downloader nel tema chiaro
