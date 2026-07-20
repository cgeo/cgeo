### Notes générales de publication

**Bord à bord**

En raison des politiques du Play Store, nous avons mis à jour le niveau de l'API Android cette version de c:geo cibles + nous avons modifié certaines routines de mise en page de l'écran. Cela peut provoquer avec quelques effets secondaires indésirables, en particulier sur les nouvelles versions d'Android. Si vous rencontrez des problèmes avec cette version de c:geo, veuillez signaler soit sur [GitHub](https://github.com/cgeo/cgeo) ou par e-mail à [support@cgeo.org](mailto:support@cgeo.org)

**Anciennes cartes**

Comme annoncé avec les versions 2025.07.17 et 2025.12.01, nous avons finalement supprimé les implémentations héritées de nos cartes. Vous passerez automatiquement à notre nouvelle UnifiedMap et ne remarquerez aucune différence, sauf quelques nouvelles fonctionnalités, dont certaines sont
- Rotation de la carte pour les cartes basées sur OpenStreetMap (en ligne et hors ligne)
- Pop-up de cluster pour Google Maps
- Cacher les sources de carte dont vous n'avez pas besoin
- Graphique d'altitude pour les routes et les pistes
- Basculer entre les listes directement depuis la carte
- Mode "Conduite" pour les cartes basées sur OpenStreetMap
- Appui long sur la piste / route individuelle pour plus d'options

### Carte
- Nouveau: L'optimisation des itinéraires met en cache les données calculées
- Nouveau : L'activation du mode live garde les waypoints de la cible actuellement définie visibles
- Nouveau : Un appui long sur la ligne de navigation ouvre le graphique d'altitude (UnifiedMap)
- Nouveau : Afficher les waypoints générés sur la carte
- Nouveau : Téléchargement des caches triées par distance
- Correction : Doublage des éléments de route individuels
- Nouveau : Prise en charge du thème Motorider (VTM uniquement)
- Nouveau : Fournisseur de tuiles qui n'affiche pas de carte (ne pas afficher de carte, juste des caches, etc.)
- Changement: Distance maximale des points de connexion sur la piste historique abaissée à 500m (configurable)
- Nouveau : Permettre l'importation de fichiers KML en tant que pistes (ex: itinéraire trackable)
- Nouveau : Offre de définir l'icône de cache même si la cache n'est pas encore stockée
- Nouveau : Boite d'information pour le graphique d'altitude montrant la distance restante, montée, descente
- Nouveau : Afficher les coordonnées des waypoints dans la popup des waypoints
- Correction : Les paramètres rapides de la carte peuvent afficher les boutons "1"/"2" pour les profils de routage vides après changement de langue
- Nouveau : Calcul des données d'altitude manquantes lors de l'importation de pistes (si les données d'altitude sont téléchargées)
- Correction : Arrêt du téléchargeur de tuiles dans certaines conditions (cartes en ligne OpenStreetMap uniquement)
- Nouveau : marqueurs de cache conditionnels
- New: Show navigation hint (arrow + distance)

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
- Nouveau : Visualiser le débordement des coordonnées calculées dans la liste des waypoints
- Nouveau : Entrée de menu dans la liste des waypoints pour marquer certains types de waypoints comme visités
- Nouveau : Variables pour le log des objets voyageurs (nom de la géocache, code de la géocache, utilisateur)
- Changement : Suppression du lien vers l'application WhereYouGo obsolète. Le lecteur intégré de cartouches Wherigo est maintenant pris par défaut pour les cartouches Wherigos.
- Correction : Il manque un raccourci dans le mode guidé de la calculatrice de waypoint
- Nouveau: fonctions agrégées avec support de l'intervalle : add/somme, min/minimum, max/maximum, cnt/nombre, avg/moyenne, multiply/produit/prod
- Correction : gestion incorrecte du statut DNF pour les plates-formes opencaching
- Nouveau : Supprimer le log hors ligne après la fusion avec le log en ligne
- Nouveau : Afficher la confirmation lors de la suppression des caches avec des logs hors ligne
- Nouveau : Afficher la confirmation lors de la suppression de toutes les caches de la liste "Toutes"
- Nouveau : Permettre le formatage Markdown pour le texte des caches définies par l'utilisateur
- Changement: Stocker le cache avant d'ajouter une image utilisateur
- Correction : Crash lors du chargement des images intégrées directement dans le texte de la liste
- Nouveau : Afficher ses propres favoris dans la vue des logs (Geocaching.com + logs hors-ligne)

### Wherigo Player
- Nouveau : Traduction hors ligne pour les caches Wherigo
- Nouveau : Amélioration de la gestion des boutons
- Nouveau : Enregistrement automatique du statut
- Nouveau : Possibilité de créer un raccourci vers Wherigo sur l'écran d'accueil de votre mobile

### Général
- Nouveau : Option de partage après avoir logué une cache
- Changement : Ne pas afficher les options "besoin de maintenance" ou "a besoin d'être archivé" pour ses propres caches
- Correction : La restauration d'une sauvegarde peut dupliquer les fichiers de suivi dans le stockage interne et les sauvegardes suivantes
- Changement : Références supprimées à Twitter
- Nouveau : Supprimer les fichiers de suivi orphelins lors du nettoyage et de la restauration de la sauvegarde
- Nouveau: Avertissement en essayant d'ajouter trop de caches à une liste de favoris
- Nouveau: Fonctions de surveillance des listes
- Nouveau : Possibilité de traduction hors ligne avec les applications Google Translate ou DeepL (si installé)
- Nouveau : Supprimer les éléments de l'historique de recherche
- Changement : Suppression de GCVote (service arrêté)
- Nouveau : Barre d'outils colorée sur les pages de détails de la cache
- Nouveau : Sélection de plusieurs listes de bookmarks / pocket queries à télécharger
- Nouveau: Aperçu des listes de favoris
- Changement: Augmentation de la version minimale requise d'Android vers Android 8
- Nouveau : Boutons rapides par défaut pour les nouvelles installations
- Corriger : Les titres dans les boîtes de dialogue d'entrée de la plage sont coupées
- Correction : Notification de mise à jour de la version nightly vers l'APK régulier, même pour la variante FOSS
- Nouveau: option "Ignorer l'année" pour les filtres de date
- Nouveau : Rendre l'URI distant cliquable dans les téléchargements en attente
- Changement : Utiliser les paramètres système comme thème par défaut pour les nouvelles installations
- Nouveau : exportation GPX : Écrire les annotations GSAK Lat/LonBeforeCorrect lors de l'exportation des waypoints d'origine
- Nouveau : Afficher la barre d'annulation lors de la suppression des caches de la liste de la carte
- Correction : Crahs dans le filtre favori en pourcentage
- Nouveau : Faciliter l'utilisation de listes simples comme listes parentes
- Changement : Utiliser le fuseau horaire local (de l'appareil, pas de l'événement) pour les entrées du calendrier (au lieu de l'UTC)
- Correction : Certains textes ne tiennent pas compte du changement de langue
- Correction : « Utiliser les unités impériales » pas correctement initialisé lors d'une nouvelle installation
- Changement: module de traduction hors ligne Bergamot open source remplaçant le traducteur Google ML Kit à source fermée
- Changement : Nouveau sélecteur d'émoji
