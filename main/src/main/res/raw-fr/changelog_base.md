Due to Play Store policies we have updated the Android API level this version of c:geo targets + we have changed some of the screen layout routines. Cela peut provoquer avec quelques effets secondaires indésirables, en particulier sur les nouvelles versions d'Android. Si vous rencontrez des problèmes avec cette version de c:geo, veuillez signaler soit sur [GitHub](https://github.com/cgeo/cgeo) ou par e-mail à [support@cgeo.org](mailto:support@cgeo.org)

### Carte
- Nouveau: L'optimisation des itinéraires met en cache les données calculées
- Nouveau : L'activation du mode live garde les waypoints de la cible actuellement définie visibles
- Nouveau : Un appui long sur la ligne de navigation ouvre le graphique d'altitude (UnifiedMap)
- Nouveau : Afficher les waypoints générés sur la carte

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

### Wherigo Player
- Nouveau : Traduction hors ligne pour les caches Wherigo
- Nouveau : Amélioration de la gestion des boutons

### Général
- Nouveau : Option de partage après avoir logué une cache
- Changement : Ne pas afficher les options "besoin de maintenance" ou "a besoin d'être archivé" pour ses propres caches
- Correction : La restauration d'une sauvegarde peut dupliquer les fichiers de suivi dans le stockage interne et les sauvegardes suivantes
- Changement : Références supprimées à Twitter
- Nouveau : Supprimer les fichiers de suivi orphelins lors du nettoyage et de la restauration de la sauvegarde
- Nouveau: Avertissement en essayant d'ajouter trop de caches à une liste de favoris
- Nouveau: Fonctions de surveillance des listes
- New: Offer offline translation with Google Translate or DeepL apps (if installed)
