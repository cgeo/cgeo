### Annonce de dépréciation des cartes UnifiedMap & "ancienne" cartes
c:geo a une toute nouvelle implémentation de cartes appelée "UnifiedMap" depuis quelque temps, qui finira par remplacer les anciennes implémentations de Google Maps et Mapsforge (OpenStreetMap). Ceci est un avis de dépréciation pour vous informer de la future feuille de route.

UnifiedMap a été publié il y a environ un an. Il supporte toujours Google Maps et OpenStreetMap (en ligne + hors ligne), mais de manière technique entièrement retravaillée, et avec beaucoup de nouvelles fonctionnalités excitantes que les "anciennes" cartes ne prennent pas en charge, dont certaines sont
- Rotation de la carte pour les cartes basées sur OpenStreetMap (en ligne et hors ligne)
- Pop-up de cluster pour Google Maps
- Cacher les sources de carte dont vous n'avez pas besoin
- Graphique d'altitude pour les routes et les pistes
- Basculer entre les listes directement depuis la carte
- Mode "Conduite" pour les cartes basées sur OpenStreetMap

UnfiedMap s'est avéré stable depuis un certain temps, donc nous allons supprimer les anciennes implémentations de cartes pour réduire les efforts pour maintenir c:geo.

Feuille de route :
- Les cartes "anciennes" sont actuellement en mode dépréciation - nous ne corrigerons plus de bugs pour elles.
- UnifiedMap sera la carte par défaut pour tous les utilisateurs à l'automne 2025.
- Les implémentations de cartes "anciennes" seront supprimées au printemps 2026.

D'ici là, vous pouvez basculer entre les différentes implémentations dans settings => sources de cartes.

### Carte
- Nouveau : Afficher les géorepérages pour les étapes de lab (UnifiedMap) - activer "Cercles" dans les paramètres rapides de la carte pour les afficher
- Nouveau : Option permettant de définir des cercles avec un rayon individuel aux waypoints (option de menu contextuel "geofence")
- Correction : la vue de la carte n'a pas été mise à jour lors de la suppression d'une cache de la liste affichée
- Correction : Le nombre de caches dans le sélecteur de liste n'est pas mis à jour lors du changement du contenu de la liste
- Changement: Garder la vue actuelle lors du mappage d'une liste, si toutes les caches entrent dans la fenêtre d'affichage actuelle
- Nouveau : Suivre mon emplacement dans le diagramme d'altitude (UnifiedMap)
- Nouveau : Activer les actions "déplacer vers" / "copier vers" pour "afficher comme liste"
- Nouveau : Prise en charge du thème Elevate Winter dans le téléchargeur de cartes
- Nouveau : ombrage de colline adaptatif, mode optionnel de haute qualité (UnifiedMap Mapsforge)
- Nouveau : Boîte de dialogue de configuration rapide des routes/pistes relancées
- Nouveau : Appuyez longuement sur l'icône de sélection de carte pour sélectionner le fournisseur de tuiles précédent (UnifiedMap)
- Nouveau : Permettre de définir le nom d'affichage pour les cartes hors ligne dans un fichier compagnon (UnifiedMap)
- Nouveau : Appuyez longuement sur "activer le bouton live" pour charger les caches hors ligne
- Nouveau : ombrage hors-ligne pour UnifiedMap (VTM variant)
- Nouveau : prise en charge des cartes d'arrière plan (UnifiedMap VTM uniquement)
- Correction : Les icônes compactes ne redeviennent pas grandes en zoomant en mode automatique (UnifiedMap)
- Nouveau: Actions de appui long dans la fiche de cache : code GC, titre de la cache, coordonnées, note personnelle/indice
- Changement : Bascule la fiche de cache pour un sélecteur d'émoticônes sur appui court pour résoudre la collision

### Détails de la cache
- Nouveau : Traduction hors ligne du texte de la liste et des logs (expérimental)
- Nouveau : Possibilité de partager une cache avec ses données utilisateur (coordonnées, note personnelle)
- Correction : Service vocal interrompu lors de la rotation de l'écran
- Correction : Détails de la cache : La liste des caches n'est pas mise à jour après avoir tapé sur le nom de la liste et supprimé cette cache de cette liste
- Correction : La note de l'utilisateur est perdue en rafraîchissant une adventure lab
- Changement : Les espaces réservés liés à la date de log utiliseront la date choisie au lieu de la date courante
- Nouveau : Réduire les entrées de log longues par défaut

### Wherigo Player
- Nouveau : Le lecteur Wherigo intégré vérifie les identifiants manquants
- Changement : Retrait du rapport de bogue Wherigo (les erreurs sont principalement liées aux cartouches, doivent être corrigées par le propriétaire de la cartouche)
- Nouveau : Possibilité de naviguer vers une zone en utilisant la boussole
- Nouveau : Possibilité de copier les coordonnées du centre de zone dans le presse-papiers
- Nouveau : Définir le centre de la zone comme cible lors de l'ouverture de la carte (pour obtenir les informations sur le routage et la distance pour elle)
- Nouveau : Support de l'ouverture de fichiers Wherigo locaux
- Changement : Un appui long sur une zone sur la carte n'est plus reconnu. Cela permet aux utilisateurs de faire d'autres choses dans la zone de la carte disponible sur appui long, par exemple: créer un cache défini par l'utilisateur
- Nouveau : Afficher l'avertissement si wherigo.com signale que le CLUF est manquant (ce qui entraîne une défaillance du téléchargement des cartouches)

### Général
- Nouveau: page de recherche réorganisée
- Nouveau : Filtre de nombre d'inventaire
- Nouveau : Prise en charge des coordonnées au foramt DD,DDDDDDD
- Nouveau : Afficher le nom du dernier filtre utilisé dans la boîte de dialogue de filtre
- Nouveau : Calculatrice de coordonnées : Fonction pour remplacer "x" par le symbole de multiplication
- Correction : Altitude incorrecte (pas avec une moyenne au-dessus du niveau de la mer)
- Correction : Le réglage de la limite de distance à proximité ne fonctionne pas correctement pour les petites valeurs
- Correction : Le tri des listes de caches par distance décroissante ne fonctionne pas correctement
- Corriger : Lab caches exclues par le filtre D/T même avec les « incertitudes inclus» actifs
- Correction : Problèmes de couleur avec les icônes du menu en mode clair
- Nouveau : Ajouter "Supprimer les événements passés" à la liste "all"
- Nouveau : Afficher le connecteur pour les "caches définies par l'utilisateur" comme actif dans le filtre source
- Nouveau : export GPX : exportation des logs / objets voyageurs rendus facultatifs
- Nouveau : bouton ajouté pour supprimer les modèles de log
- Correction : L'importation du fichier de carte local obtient un nom de carte aléatoire
- Correction : Téléchargement de cartes proposant des fichiers cassés (0 octets) pour le téléchargement
- Nouveau: Ajout de mappings pour certains types de cache OC manquants
- Nouveau : Déplacer les listes "récemment utilisées" en haut dans la boîte de dialogue de sélection de liste lors de l'appui sur le bouton "récemment utilisé"
- Nouveau : Partager la liste des géocodes de la liste des caches
- Changement: "Navigation (voiture)" etc. utilisez le paramètre "q=" au lieu du paramètre obsolète "ll="
