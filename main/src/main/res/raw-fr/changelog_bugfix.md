##
- Correction : Les caches ne se chargent pas après l'activation de la carte en temps réel (UnifiedMap)
- Correction : Il manque l'option 'utiliser la liste courante' lors de la création d'une cache définie par l'utilisateur (UnifiedMap)
- Fix: Compass rose hidden behind distance views (UnifiedMap)
- Fix: Cache details scroll to page header after editing personal note
- New: Show event date to cache selector
- Fix: Login to OC platform not recognized by installation wizard
- Fix: Routing not working by default after fresh installation
- Fix: Info sheet toolbar hidden in landscape mode even on large devices
- Fix: "follow my location" still active after zoom with pan (UnifiedMap)
- Fix: Individual routes exported as track cannot be read by Garmin devices
- Fix: Loading trackables from internal database fails under certain conditions
- Fix: Route to navigation target not recalculated on routing mode change

##
- Correction : les liens des objets voyageurs avec le paramètre TB ne fonctionnent pas
- Nouveau : Ajouter un indice à la recherche de mots clés désactivés pour les membres basic
- Corriger : la journalisation des objets voyageurs ne fonctionne pas à nouveau (modifications du site web)
- Correction : Les informations d'altitude tournent avec le marqueur de position
- Correction : Le nom d'utilisateur n'est pas détecté lors de la connexion s'il contient certains caractères spéciaux

##
- Fix: Show/hide waypoints not working correctly if crossing waypoint limits (UnifiedMap)
- Correction : Loguer les caches ou les objets voyageurs ne fonctionnent plus (modifications du site web)
- Correction : La suppression de ses propres logs ne fonctionne pas

##
- Correction : Le compteur de caches trouvées n'est pas détecté dans certaines situations en raison de modifications du site
- Correction : Crash lors de l'ouverture de la carte avec des noms de fichiers de piste vides
- Correction : Rotation automatique de la carte toujours active après réinitialisation en utilisant la rose des vents (UnifiedMap)
- Fix: Missing compass rose in autorotation modes on Google Maps (UnifiedMap)
- Fix: Trackable logs cannot be loaded due to website changes
- Change: Combine elevation + coordinate info in map long-tap menu into single "selected position" + show distance to current position

##
- New: Delete offline logs using context menu
- Fix: Deleting offline log not working under certain condition
- Fix: Filter name lost on filter quickchange
- Change: Sort trackfiles by name
- Change: Save trackable action also for offline logs
- Fix: Map switching to 0,0 coordinates on map type change (UnifiedMap)
- Fix: Waypoint target switching back to cache as target (UnifiedMap)
- Fix: "Storing" a cache without selecting a list
- Fix: Login failure due to website change on geocaching.com
- Change: Show elevation info below position marker (if activated)
- NOTE: There are more issues due to recent website changes on geocaching.com, which have not been fixed yet. We are working on it. See our [status page](https://github.com/cgeo/cgeo/issues/15555) for current progress.
