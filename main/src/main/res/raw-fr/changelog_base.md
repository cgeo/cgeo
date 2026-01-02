Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Cela peut provoquer avec quelques effets secondaires indésirables, en particulier sur les nouvelles versions d'Android. Si vous rencontrez des problèmes avec cette version de c:geo, veuillez signaler soit sur [GitHub](https://github.com/cgeo/cgeo) ou par e-mail à [support@cgeo.org](mailto:support@cgeo.org)

### Carte
- Nouveau: L'optimisation des itinéraires met en cache les données calculées
- Nouveau : L'activation du mode live garde les waypoints de la cible actuellement définie visibles
- Nouveau : Un appui long sur la ligne de navigation ouvre le graphique d'altitude (UnifiedMap)
- Nouveau : Afficher les waypoints générés sur la carte
- New: Download caches ordered by distance
- Fix: Doubling of individual route items
- New: Support for Motorider theme (VTM only)
- New: Support for transparent background display of offline maps (VTM only)
- New: NoMap tile provider (don't show map, just caches etc.)
- Change: Max distance to connect points on history track lowered to 500m (configurable)

### Détails de la cache
- Nouveau : Détecter les caractères supplémentaires dans les formules : –,  , ×
- Nouveau : Conserver l'horodatage de ses propres logs lors de l'actualisation d'une cache
- Nouveau : Vue mini boussole facultative (voir paramètres => détails de la cache => Afficher la direction dans la vue détaillée de la cache)
- Nouveau : Afficher les logs des propriétaires dans l'onglet "vous / amis"
- Changement: l'onglet "Vous / amis" affiche le nombre de logs pour cet onglet au lieu de compteurs globaux
- Changement : Amélioration de l'en-tête dans les onglets variables et waypoint
- Correction : Deux éléments « supprimer le journal» affichés
- Correction : c:geo plantait dans les détails de la cache lors de la rotation de l'écran
- Changement : Mise en page plus compacte pour "ajouter un nouveau waypoint"
- Nouveau: Possibilité de charger les images des caches de geocaching.com en taille "inchangée"
- Nouveau : la vue des variables peut être filtrée
- New: Visualize calculated coordinates overflow in waypoint list
- New: Menu entry in waypoint list to mark certain waypoint types as visited
- New: Placeholders for trackable logging (geocache name, geocache code, user)
- Change: Removed the link to outdated WhereYouGo player. Integrated Wherigo player is now default for Wherigos.
- Fix: Missing quick toggle in guided mode of waypoint calculator

### Wherigo Player
- Nouveau : Traduction hors ligne pour les caches Wherigo
- Nouveau : Amélioration de la gestion des boutons
- New: Status auto-save
- New: Option to create shortcout to Wherigo player on your mobile's home screen

### Général
- Nouveau : Option de partage après avoir logué une cache
- Changement : Ne pas afficher les options "besoin de maintenance" ou "a besoin d'être archivé" pour ses propres caches
- Correction : La restauration d'une sauvegarde peut dupliquer les fichiers de suivi dans le stockage interne et les sauvegardes suivantes
- Changement : Références supprimées à Twitter
- Nouveau : Supprimer les fichiers de suivi orphelins lors du nettoyage et de la restauration de la sauvegarde
- Nouveau: Avertissement en essayant d'ajouter trop de caches à une liste de favoris
- Nouveau: Fonctions de surveillance des listes
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
- New: Delete items from search history
- Change: Remove GCVote (service discontinued)
- New: Colored toolbar on cache details pages
- New: Select multiple bookmark lists / pocket queries to download
- New: Preview bookmark lists
- Change: Increase minimum required Android version to Android 8
- New: Default quick buttons for new installations
- Fix: Titles in range input dialogs cut off
- Fix: Notification for nightly update points to regular APK even for FOSS variant
- New: "Ignore year" option for date filters
