### Carte
- Nouveau: "Modifier la note personnelle" depuis la fiche d'information de la cache
- Correction : Les waypoints ne sont pas filtrés sur le mapping d'une seule cache (UnifiedMap)
- Nouveau : Support des fournisseurs de tuiles définis par l'utilisateur
- Correction : Rafraîchir les données de la carte après ouverture / fermeture de la boîte de dialogue des paramètres (UnifiedMap)
- Nouveau : Activation/Désactivation de l'affichage en 2D/3D des bâtiments (cartes OSM UnifiedMaps)
- Nouveau : Enregistrement de la cache/actualisation depuis une popup déplacée en arrière-plan
- Changement : Recherche de coordonnées : Afficher la direction et la distance vers la position cible et non vers la position actuelle
- Nouveau : Indicateur graphique D/T dans la fiche d'information de la cache
- Correction : La boussole est cachée quand la barre de filtres est visible (UnifiedMap)
- Changement : Suppression des légendes du thème de la carte
- Correction : plusieurs popups de sélection de navigation lors d'un appui long

### Détails de la cache
- Nouveau: Afficher les images liées dans "note personnelle" dans l'onglet Images
- Changement: Simplifiez l'action de appui long dans les détails de la cache et les détails de l'objet voyageur
- Nouveau : Mise à l'échelle plus douce des images de log
- Changement : Changer l'icône "Editer les listes" du crayon vers la liste + crayon
- Correction : La fonction vanity échoue sur les chaînes longues
- Correction : mauvaise priorité d'analyse dans la sauvegarde des formules
- Changement : Autoriser les plages d'entiers plus larges dans les formules (interdire l'utilisation de la négation)
- Nouveau : Autoriser les images stockées en cache par les utilisateurs lors de la création/modification du log
- Correction : Les images de Spoiler ne sont plus chargées (modification du site)

### Général
- Nouveau : Changer pour définir l'état de l'Adventure Lab manuellement ou automatiquement
- Nouveau : Boîte de dialogue de sélection de la liste : Groupes automatiques de caches ayant un ":" dans leur nom
- Changement : Utiliser OSM Nominatum comme géocodage de secours en remplaçant le géocodeur MapQuest (qui ne fonctionne plus pour nous)
- Changement : Mise à jour du BRouter intégré en version 1.7.5
- Nouveau : Lire les informations d'altitude à partir de la piste lors de l'importation
- Nouveau : API Locus prends désormais en charge la taille de la cache
- Correction : Les résultats de recherche d'un emplacement ne sont plus triés par distance vers l'emplacement cible
- Nouveau : filtre "Coordonnées corrigées"
- Changement: Mise à jour de targetSDK en version 34 pour se conformer aux prochaines exigences du Play Store
- Nouveau: Ajout de "aucun" à la sélection des profils de routage
- Changement : Améliorer la description de la fonction "maintenance" (supprimer les données orphelines)
- Nouveau : Afficher les avertissements en cas d'erreur HTTP 429 (Trop de requêtes)
- Correction : clignotement lors de l'actualisation de la liste des caches
- Nouveau : Autoriser l'affichage des mots de passe dans la configuration du connecteur
- Correction : La recherche de GeoKrety ne fonctionne plus avec les codes de suivi
- Fix: Missing "copy to clipboard" option on some devices (see settings => system)
