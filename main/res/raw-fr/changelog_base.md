### Général
- Changement : Introduction de la navigation en bas pour un accès direct aux écrans les plus utilisés de c:géo, en remplacement de l'ancien écran principal

### Carte
- Correction : Lors du chargement de fichiers GPX contenant plusieurs traces, les afficher en tant que traces indépendantes
- Modification : Activer automatiquement l'affichage du parcours lors du chargement d'un fichier de suivi GPX
- New: Allow displaying several tracks at once
- New: D/T symbols for cache icons (optional)
- New: Option to check for missing routing data for current viewport
- New: Theme legend for Elevate, Elements and Freizeitkarte themes
- Fix: Reenable routing with external BRouter app in version 1.6.3
- Fix: Avoid map duplication by map downloader in certain conditions

### Liste des caches
- Nouveau : Option pour sélectionner les 20 caches suivantes
- Nouveau: Aperçu des attributs (voir Gérer les caches => Aperçu des attributs)
- Nouveauté : Ajout de l'import des signets (requiert un compte GC premium)
- Nouveauté : Inverser le tri lors d'un clic long sur la barre de tri
- Changement: Effectuer également un tri automatique par distance pour les listes contenant des séries de caches avec plus de 50 caches (jusqu'à 500)
- Fix: Use a shorter timeout for fast scrolling mechanism for less interference with other layout elements

### Détails de la cache
- Nouveauté : Envoyer les coordonnées de la cache à geochecker
- Nouveau : Icônes d'attributs colorés (groupes d'attributs suivants)
- Correction : Problème lors de l'ouverture des images depuis l'onglet Galerie dans les applications externes sur certains appareils Samsung
- Fix: Missing log count (website change)

### Divers
- Nouveau : Charger rapidement les géocodes à partir du texte du presse-papiers dans la recherche de l'écran principal
- New: Added support for user-defined log templates
- Nouveau : Rendre les paramètres => Voir les paramètres filtrables
- Nouveau : Activer la recherche dans les préférences
- Nouveauté : Ajout de l'assistant GC Wizard à la liste des applications utiles
- New: Attributes filter: Allow selecting from which connectors attributes are shown
- New: Option to limit distance in nearby search (see Settings => Services)
- Change: Removed barcode scanner from useful apps list and from mainscreen
- Change: Removed BRouter from useful apps list (you can still use both external and internal navigation)
- Fix: Avoid repeated update checks for maps/routing tiles with interval=0
- Fix: Optimize support to autofill passwords from external password store apps in settings
- Fix: Enable tooltips for systems running on Android below version 8
- Fix: Crash on long-tap on trackable code in trackable details
- Fix: Fieldnotes upload (website change)
- Refactored settings to meet current Android specifications
- Updated MapsWithMe API

