### Generale
- Cambio: Introduzione della funzione di navigazione nella parte inferiore per l'accesso diretto alle schermate più utilizzate di c:geo, sostituendo il vecchio schermo principale

### Mappa
- Corretto: Caricando file GPX contenenti più tracce, queste vengono visualizzate come tracce separate non collegate tra loro
- Cambiato: Visualizzazione automatica della traccia al caricamento di un file GPX
- Nuovo: Possibilità di visualizzare diverse tracce contemporaneamente
- Nuovo: simboli D/T per le icone della cache (opzionale)
- Nuovo: Opzione per controllare i dati di routing mancanti per la visualizzazione corrente
- Nuovo: Leggenda del tema per i temi Elevate, Elements e Freizeitkarte
- Fix: Reenable routing with external BRouter app in version 1.6.3
- Fix: Avoid map duplication by map downloader in certain conditions

### Elenco dei cache
- Nuovo: Opzione per selezionare i 20 cache succesivi
- Novità: Panoramica degli attributi (vedi Gestisci i Cache => Panoramica degli attributi)
- Nuovo: Aggiunta importazione da liste di segnalibri (solo utenti premium GC)
- Nuovo: Inverti ordinamento con un tocco prolungato sulla barra di ordinamento
- Cambio: Effettua anche l'ordinamento automatico in base alla distanza, per le liste contenenti serie di cache con più di 50 cache (fino a 500)
- Fix: Use a shorter timeout for fast scrolling mechanism for less interference with other layout elements

### Dettagli del cache
- Nuovo: Passa le coordinate correnti del cache a geochecker (se supportato da geochecker)
- Nuovo: icone di attributi colorate (rispecchia gruppi di attributi)
- Corretto: Problema durante l'apertura di immagini dalla scheda galleria in applicazioni esterne su alcuni dispositivi Samsung
- Fix: Missing log count (website change)

### Altro
- Novità: Carica rapidamente i geocode dal testo degli appunti nella ricerca nella schermata principale
- New: Added support for user-defined log templates
- Novità: Rendi le Impostazioni => Visualizza Impostazioni filtrabile
- Nuovo: abilita la ricerca nelle preferenze
- Novità: Aggiunto GC Wizard all'elenco delle app utili
- Nuovo: Filtro attributi: Consente di selezionare gli attributi per cui vengono visualizzati
- Nuovo: Opzione per limitare la distanza nella ricerca nelle vicinanze (vedere Impostazioni => Servizi)
- Modifica: Rimosso lo scanner di codici a barre dall'elenco delle app utili e dalla schermata principale
- Modifica: Rimosso BRouter dall'elenco delle app utili (puoi ancora usare sia la navigazione esterna che interna)
- Corretto: Evita i controlli ripetuti degli aggiornamenti delle mappe/routing con intervallo=0
- Corretto: Ottimizzato il supporto per la compilazione automatica delle password dalle app di archiviazione di password esterne, nelle impostazioni
- Corretto: Abilita suggerimenti per sistemi in esecuzione su Android in versioni precedenti alla 8
- Fix: Crash on long-tap on trackable code in trackable details
- Fix: Fieldnotes upload (website change)
- Refactored settings to meet current Android specifications
- Updated MapsWithMe API

