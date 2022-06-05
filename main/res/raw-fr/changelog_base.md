### Détails de la cache

- Nouveauté : Refonte de la calculatrice de coordonnées (prise en charge des fonctions)
- Modification : les variables pour le calcul des étapes sont désormais globales à la cache
- Nouveau : Onglet Variables dans les détails de la cache
- Nouveauté : Génération de points de passage basés sur des formules et des variables
- Nouveauté : Modèles de journal pour utilisation hors ligne
- Nouveauté : Ajout de la\[position\] dans le menu du modèle de journal
- Nouveauté : Autoriser la sélection du texte des journaux
- Correction : lien du vérificateur GC générant une boucle dans certaines conditions sur Android 12
- Nouveau : ajouté un bouton geochecker à la fin du texte de description (si approprié)
- Nouveau : ajouté l'option "loguer dans le navigateur" au menu des caches

### Liste des caches

- New: Added option for "has user defined waypoints" to advanced status filter
- Nouveau : permet l'inclusion de caches sans D/T dans le filtre
- Correction : Retri de la liste des caches à chaque changement de localisation sur l'ordre de tri des distances

### Carte

- Nouveauté : Thème de carte pour Google Maps
- Nouveau : options de mise à l'échelle de la carte pour OpenStreetMap (voir les options du thème)
- Modification : Paramètres => Carte => Appui long sur la carte active/désactive aussi l'appui long sur la carte de cache (utile pour la création de nouveaux points de passage)
- Modification : Ne plus afficher des cercles de distance pour les caches archivées
- Correction : Plantage des cartes OpenStreetMap dans certaines conditions
- Correction : le routage se fige lorsque de nombreuses tuiles de routage sont installées

### Général

- Nouveauté : Sauvegardes automatiques (optionnel)
- Correction : reprendre l'importation des téléchargements terminés
- Nouveau : ajout de boutons de lancement rapide configurables à l'écran d'accueil, voir Paramètres => Apparence
- Nouveauté : Mise à jour du routage interne avec BRouter v1.6.3
- Nouveau : limite l'utilisation répétée de la touche retour en lançant une nouvelle pile d'activité lors du passage à une autre partie de l'application
- Nouveau : ajoute d'un paramètre pour décoder l'indice de la cache par défaut (au lieu de le faire uniquement en appuyant dessus)
- Nouveau : accepte de marquer des caches provenant de sources inconnues comme étant trouvées localement
- Supprimé : service d'objets voyageurs Geolutin (car il n'existe plus)
