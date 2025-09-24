A causa de les polítiques de Play Store, hem actualitzat el nivell d'API d'Android per a aquesta versió dels objectius c:geo i hem canviat algunes de les rutines de disseny de la pantalla. Això pot tenir alguns efectes secundaris no desitjats, especialment en les versions més noves d'Android. Si teniu algun problema amb aquesta versió de c:geo, informeu-ne a [GitHub](https://github.com/cgeo/cgeo) o per correu electrònic a [support@cgeo.org](mailto:support@cgeo.org)

### Mapa
- Nou: l'optimització de la ruta guarda les dades calculades
- Nou: activar el mode en directe manté visibles els punts de referència de l'objectiu establert actualment
- Nou: Si toqueu llargament la línia de navegació, s'obre el gràfic d'elevació (UnifiedMap)
- Nou: Mostra els punts de referència generats al mapa
- Nou: Descarrega els catxés ordenats per distància
- Correcció: Duplicació d'elements de ruta individuals

### Detalls del catxé
- Nou: Detecteu caràcters addicionals a les fórmules: –, ⋅, ×
- Nou: conserva la marca de temps dels propis registres en actualitzar un catxé
- Novetat: minivisualització de la brúixola opcional (vegeu la configuració => detalls del catxé => Mostra la direcció a la vista detallada del catxé)
- Nou: Mostra els registres dels propietaris a la pestanya "amics/propis"
- Canvi: La pestanya "Amics/propis" mostra els recomptes de registre d'aquesta pestanya en lloc dels comptadors globals
- Canvi: Millora de la capçalera a les pestanyes de variables i punts de referència
- Correcció: Es mostren dos elements de "supressió del registre"
- Correcció: c:geo es bloqueja als detalls del catxé en girar la pantalla
- Canvi: Disseny més compacte per "afegir un nou punt de referència"
- Nou: Opció per carregar imatges pels catxés de geocaching.com en mida "sense canvis"
- Nou: la vista de variables es pot filtrar

### Wherigo Player
- Nou: Traducció fora de línia per a Wherigos
- Nou: Millora del maneig dels botons

### General
- Nou: opció de compartir després de registrar un catxé
- Canvi: no mostri les opcions "necessita manteniment" o "necessita arxivar" per a els catxés propis
- Correcció: La restauració d'una còpia de seguretat pot duplicar els fitxers de track a l'emmagatzematge intern i a les còpies de seguretat posteriors
- Canvi: S'han eliminat les referències a Twitter
- Nou: Elimina els fitxers de pista orfes en netejar i restaurar la còpia de seguretat
- Nou: Avís en intentar afegir massa catxés a una llista de marcadors
- Nou: Funcions de llista de seguiment/no seguiment
- Nou: Ofereix traducció fora de línia amb les aplicacions de Google Translate o DeepL (si estan instal·lades)
- Nou: Suprimeix elements de l'historial de cerca
- Canvi: Elimina GCVote (servei descontinuat)
