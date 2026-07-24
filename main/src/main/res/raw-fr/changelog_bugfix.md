##
Il est temps de mettre à jour ! Si vous utilisez toujours Android 7 ou plus, ce sera peut-être la dernière mise à jour c:geo pour vous ! Avec notre prochaine version de c:geo, nous allons supprimer le support d'Android 5 à 7 pour réduire notre charge de maintenance et pour être en mesure de mettre à jour certains composants externes utilisés par c:geo qui sont toujours actuellement en attente. Nous supporterons donc toujours les version d'Android 8 à 16 (et les nouvelles versions quand elles seront publiées), une période de plus de huit ans d'histoire Android.

- Correction : La popup d'ouverture de la cache/du waypoint met du temps à souvrir sur certains appareils
- Correction : Modifier la description de la cache ne prend pas en charge la copie & coller
- Correction : Quelques plantages et « l'application ne répond pas»
- Correction : La suppression du journal de l'objet voyageur échoue (modification du site)

##
- Correction : Suppression des images de log cassée (modification du site)
- Changement : Unifier les boutons de chargement des parcours et des itinéraires individuels
- Correction : Les attributs de cache ne sont pas détectés correctement dans certaines conditions
- Corriger : Loguer les caches (changement de site)
- Correction : Loguer les objets voyageurs (changement de site)

##
- Correction : L'importation de Pocket query est cassée (changement de site web)

##
- Correction : Crash lors de l'accès aux routes
- Correction : Crash sur la page du waypoint
- Changement : La recherche de ses "propres caches" commence par de nouveaux filtres
- Correction : Les étapes d'adventure lab non sauvegardées perde les informations "visitées" lors de la mise à jour
- Correction : invite récurrente pour les mises à jour des sources des tuiles
- Correction : emplacement aléatoire lors du placement d'un liste sur la carte (Google Maps)

##
- Correction : Crash dans la fiche de cache
- Correction : Les cartouches Wherigo ne peuvent plus être téléchargées (modification du site)

##
 - Changement : Les fichiers Wherigo ne peuvent pas être téléchargés actuellement, afficher les instructions alternatives
 - Correction : La raison de suppression du log n'impose pas la limite de longueur
 - Nouveau : journalisation étendue des plantages dans le gestionnaire de téléchargement
 - Correction : La fenêtre de point de passage peut devenir trop longue, les boutons ne sont pas accessibles
 - Correction : Certaines informations de localisation sont tronquées
 - Correction : Le routage interne ne fonctionne plus, seule la ligne droite est affichée
 - Correction : Quelques problèmes de création de dossier

Note : Si vous utilisez un routage interne, vous devrez exécuter l'étape suivante une seule fois après l'installation de cette version : aller à l'écran d'accueil c:geo ouvrir "Gérer les données hors ligne" - "Mettre à jour les données de routage", et laisser c:geo installer les fichiers mis à jour. (Raison : la structure du fichier de données de routage BRouter a été modifiée et tous les fichiers de données de routage doivent être conformes à la même version.)

##
- Correction : L'analyse de la chaîne d'emplacement de la cache échoue pour certaines langues du site web
- Correction : L'ouverture de l'objet voyageur à partir de la liste de suivi échoue
- Correction : Le clavier peut bloquer la sélection de la liste
- Correction : Le fournisseur de tuiles défini par l'utilisateur ne prend pas en charge les paramètres d'URL supplémentaires
- Correction : Inventaire / les objets voyageurs d'une cache ne sont plus chargés
- Changement : Mise à jour de l'agent utilisateur interne pour résoudre certains problèmes de téléchargement
- Correction : Regarder les détails d'un objet voyageur le supprime de l'inventaire de la cache

##
- Correction : Boîte de dialogue de téléchargement de la traduction hors-ligne affichée dans les installations sans support de traduction hors ligne
- Correction : Changement du format des coordonnées dans la fiche info cache/waypoint
- Correction : la date de fermeture dans la liste des logs (en fonction du format de la date et de la taille de la police)
- Correction : Horaires d'event non détectés dans certaines conditions
- Correction : lien dans la liste non cliquable sous certaines conditions
- Correction : Les actions de log des objets voyageurs sont parfois mélangées

##
- Changement: Le nombre maximum d'objets voyageurs par log visitant une cache a été réduit à 100 (à la demande de géocaching.com pour réduire la charge de leur serveur causée par des amateurs extrêmes d'objets voyageurs)
- Correction : Certaines exceptions de sécurité possibles lorsque l'utilisateur n'a pas accordé certains droits (ex: notifications)
- Correction : Les cercles de cache sont incomplets sur les niveaux de zoom bas (VTM seulement)
- Correction : Crash lors du rechargement des waypoints dans certaines conditions de charge
- Correction : Le filtre de date de l'événement ne fonctionne pas sous certaines conditions
- Correction : La limite maximale de ligne de log ne fonctionne pas de manière fiable dans les paramètres « illimitée »
- Correction : Crash sur l'ouverture de la carte sous certaines conditions
- Correction : Aucune carte affichée si wherigo n'a pas de zones visibles
- Correction : Crash sur l'onglet image des détails de la cache sous certaines conditions
- Correction : Recherche sur la carte avec des coordonnées invalides
- Correction : Certaines traductions ne respectent pas le paramètre c:geo-internal language

##
- Changement : La carte UnifiedMap définie comme carte par défaut pour tout le monde (dans le cadre de notre roadmap vers UnifiedMap) Vous pouvez revenir en arrière dans "paramètres" - "sources de cartes" pour le moment. Le retrait des cartes historiques est prévu pour le printemps 2026 dans nos versions régulières.
- Correction : La case à cocher Favoris est réinitialisée à la réentrée de l'écran de journal hors ligne
- Correction : La zone d'entrée du rayon de géorepérage montre un nombre décimal
- Correction : La synchronisation des notes personnelles ne fonctionne pas
- Changement : Nouvelle icône pour l'importation de traces/route GPX dans les paramètres rapides de la carte

##
- Correction : Les valeurs négatives dans le graphique d'altitude ne sont pas mises à l'échelle
- Correction : Coordonnées près de 0 cassées dans les exportations GPX
- Correction : Quelques plantages
- Essayer de corriger : ANR au démarrage
- Essayer de réparer : les données de géocache manquantes sur la carte live

##
- Correction : Crash dans la recherche de mots clés
- Correction : Crash dans la carte
- Correction : Le texte d'indice n'est plus sélectionnable
- Correction : plusieurs problèmes Wherigo

##
- Correction : Chiffrer/déchiffrer un indice nécessite d'abord un appui supplémentaire
- Correction : crash de Wherigo lors de la lecture des anciens jeux sauvegardés
- Correction : Le log à partir de c:geo n'est pas mémorisée
- Correction : Il manque une mise à jour des données en direct pour les caches trouvées & archivées
- Correction : Les waypoints dans la carte hors ligne ne sont pas affichés parfois

##
- Correction: indices des caches non encryptés (changement sur le site web)
- Correction : Lab Aventures ne se chargent pas dans l'application (changement sur le site web, vous devrez mettre à jour les adventure labs stockées pour pouvoir les appeler à nouveau de c:geo)
- Correction : UnifiedMap VTM : basculer les bâtiments 3D ne fonctionne pas pour les cartes combinées
- Correction: Traduction hors ligne: Liste des langages parfois détectés comme --

##
- Correction : Crash dans le module de traduction
- Correction : Échec de la détection de la connexion (changement du site)
- Correction : Crash lors de la récupération de la cartouche Wherigo
- Correction : « Charger plus» ne respecte pas les filtres hors ligne

##
- Correction : L'inventaire des objets voyageurs n'est pas chargé lors du log d'une cache

##
- Correctif : La migration des caches définies par l'utilisateur au démarrage de c:geo échoue => retiré pour le moment
- Correctif : Les tâches Wherigo terminées ne sont pas marquées comme terminées ou échouées































