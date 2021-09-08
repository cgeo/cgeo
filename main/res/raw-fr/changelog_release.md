### Correctifs
- "Ajouter à la liste de suivi" / "Supprimer de la liste de suivi" en erreur (modification du site)
- Les boutons "Ajouter aux favoris" / "Supprimer des favoris" n'apparaissaient plus après l'envoi d'un log "Trouvé"
- La date dans le carnet de visites était tronquée pour les polices de grande taille
- Filtrage sur la carte temps réel pour les types de caches rares retournant peu de résultats

## Correction de bugs, version 2021.08.28

### Apparence
- Augmenter la taille de la police pour les champs de saisie de texte
- Augmenter la taille de la police pour certains éléments de la boussole
- Utiliser une couleur de police avec contraste élevé dans l'onglet Points de passage
- Rendre à nouveau visible l'indicateur de log rapide hors-ligne
- Augmenter la taille de la police pour les champs de saisie de coordonnées
- Respecter les paramètres systèmes de la taille de police aussi pour les anciennes versions d'Android (5, 6 et 7)

### Détails de la cache
- Correction du titre de cache manquant si la cache est ouverte via le géocode ou le lien (changement du site)
- Correction de la description de cache manquante sur certaines caches

### Divers
- Afficher à nouveau les caches premium dans les résultats de recherche des membres basiques
- Correction de la création des caches définies par l'utilisateur si certaines caches définies par l'utilisateur ont été chargées via GPX
- Utilisation de plus d'abréviations anglaises courantes pour les caches traditionnelles dans le filtre de type de cache

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
