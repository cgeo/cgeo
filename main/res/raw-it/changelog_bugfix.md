## 2021.12.24 Bugfix release

- Fix: Enable upgrading from OpenAndroMaps v4 to v5
- Fix: Enable checks for map theme updates
- Fix: Skip Mapsforge cache cleanup due to problems under certain conditions

## 2021.12.13 Bugfix release

- Corretto: File cache di Mapsforge: Pulizia e nuova posizione (cartella separata)
- Corretto: File mappa Freizeitkarte: Usa la cartella 'latest' nel downloader come soluzione per errori temporanei del server
- Risolto: Evita l'eccezione del puntatore nullo nelle pagine di informazioni
- Risolto: Abilita l'avvolgimento per le stelle nel popup del cache
- Corretto: visualizza il messaggio di errore se non è stato trovato un cache durante il tentativo di aggiornarne le informazioni
- Risolto: Mostra browser predefinito di sistema nella selezione delle app usando 'Apri nel browser' per una cache
- Risolto: Adatta il downloader per usare la pagina del nuovo tema per le nuove OpenAndroMaps v5
- Fix: On changing a path setting don't ask user for copy or move if old path has no files
