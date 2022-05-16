### Détails de la cache

- Nouveauté : Refonte de la calculatrice de coordonnées (prise en charge des fonctions)
- Change: Variables for waypoint calculation are now cache-global
- Nouveau : Onglet Variables dans les détails de la cache
- Nouveauté : Génération de points de passage basés sur des formules et des variables
- Nouveauté : Modèles de journal pour utilisation hors ligne
- Nouveauté : Ajout de la\[position\] dans le menu du modèle de journal
- Nouveauté : Autoriser la sélection du texte des journaux
- Fix: GC checker link leading to loop in certain conditions on Android 12
- New: Added geochecker button at end of description text (when appropriate)
- New: Added 'log in browser' option to cache menu

### Liste des caches

- New: Added option for "has user defined waypoints" to advanced status filter
- New: Allow inclusion of caches without D/T in filter
- Correction : Retri de la liste des caches à chaque changement de localisation sur l'ordre de tri des distances

### Carte

- Nouveauté : Thème de carte pour Google Maps
- New: Map scaling options for OpenStreetMap (see theme options)
- Modification : Paramètres => Carte => Appui long sur la carte active/désactive aussi l'appui long sur la carte de cache (utile pour la création de nouveaux points de passage)
- Modification : Ne plus afficher des cercles de distance pour les caches archivées
- Correction : Plantage des cartes OpenStreetMap dans certaines conditions
- Fix: Routing becoming unresponsive when many routing tiles are installed

### Général

- Nouveauté : Sauvegardes automatiques (optionnel)
- Fix: Resume importing finished downloads
- New: Added configurable quick launch buttons to home screen, see Settings => Appearance
- Nouveauté : Mise à jour du routage interne avec BRouter v1.6.3
- New: Limit the need of repetitive back key usage by starting a new activity stack when changing to another part of the app
- New: Add setting to decrypt the cache hint by default (instead of only when tapping on it)