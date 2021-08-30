## Bugfix Release

### Design
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### Détails de la cache
- Fix missing cache title if cache opened via geocode or link (website change)
- Fix missing cache description on some caches

### Divers
- Show premium caches again in search results of basic members
- Fix further creation of user defined caches if some user defines caches have been loaded via GPX
- Use more common English abbreviation for traditional cache in cache type filter

## Fonctionnalités de la version 2021.08.15 :

### Système de filtrage avancé
- Introduire un nouveau système de filtrage à c:geo, prenant en charge des filtres flexibles, combinables et stockables
- Disponible dans les listes de caches et dans la vue de la carte
- Nouvelle fonction "Recherche par filtre"

### Carte
- Nouveau : Lors de la création d'une cache définie par l'utilisateur lors de l'affichage d'une carte à partir d'une liste : proposer à l'utilisateur de stocker une nouvelle cache dans la liste actuelle (au lieu de la liste par défaut pour les caches définies par l'utilisateur)
- Nouveau : Séparer les filtres "cachées" et "trouvées" dans les paramètres rapides de la carte
- Changement : Afficher en plus le nom de la cache dans les détails de la popup

### Détails de la cache
- Nouveau: Utiliser la popup de traduction Google Translate dans l'application
- Nouveau : Permettre de modifier l'icône assignée dans les détails de la cache par un clic long (caches stockées uniquement)

### Téléchargement
- Changement : Les téléchargements se produiront désormais complètement en arrière-plan, une notification s'affiche
- Changement : Les fichiers téléchargés avec succès écraseront automatiquement les fichiers existants ayant le même nom
- Changement: Si une carte nécessite un thème qui n'est pas encore installé, c:geo téléchargera et installera automatiquement ce thème

### Divers
- Changement: Nous avons complètement retravaillé les aspects techniques internes du thème c:geo pour pouvoir utiliser certains composants plus modernes fournis par Android. Cela aura quelques effets secondaires, dont certains non intentionnels. Veuillez signaler toute erreur ou problème sur notre page [GitHub](https://www.github.com/cgeo/cgeo/issues) ou en contactant le support.
- Nouveau : Prise en charge du mode jour/nuit depuis le système (optionnel)
- Nouveau : Télécharger les listes de favoris depuis geocaching.com - voir “Listes / Pocket queries” dans le menu principal
- Nouveau : Ignorer le support de geocaching.su
- Changement : L'application de navigation RMAPS n'est plus maintenue
- Correction : Extraire un point de passage portant le même nom mais ayant des coordonnées différentes de la note personnelle
- Correction : Bug lors de l'extraction de la note utilisateur pour les waypoints avec formules
- Correction : Exporter les formules vers les notes personnelles au lieu des coordonnées pour la formule complétée
- Correction : Le dossier de la carte hors-ligne et des thèmes est incorrect après réinstallation et restauration de la sauvegarde
- Correction : La trace / l'itinéraire ne peut pas être mise à jour
- Correction : Erreur de thème pour lors du téléchargement dans le thème clair
