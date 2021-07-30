### Sistema de filtratge avançat
- Presentació d’un nou sistema de filtratge a C:geo, que admet filtres flexibles, combinables i emmagatzemables
- Disponible tant a les llistes de catxés com a la vista de mapa
- Nova funció "Cerca per filtre"

### Mapa
- Novetat: Al crear un catxé definit per l'usuari mentre es mostra un mapa d'una llista: oferim a l'usuari que emmagatzemi un catxé nou a la llista actual (en lloc de la llista predeterminada per a els catxés definits per l'usuari)
- Novetat: separeu els filtres "propis" i "trobats" a la configuració ràpida del mapa
- Canvi: mostra també el nom del catxé als detalls emergents

### Detalls del catxé
- Novetat: Utilitzeu la finestra emergent de traducció de l'aplicació de Google Translate
- Novetat: Permet canviar la icona assignada a la finestra emergent de detalls de catxé mitjançant un clic llarg (només als catxés emmagatzemats)

### Descarregador
- Canvi: Les descàrregues es faran completament en segon pla; es mostrarà una notificació
- Canvi: Els fitxers descarregats correctament sobreescriuran automàticament els fitxers existents amb el mateix nom
- Canvi: Si un mapa requereix un tema determinat que encara no està instal·lat, C:geo també descarregarà i instal·larà aquest tema automàticament

### Altres
- Canvi: Hem reelaborat completament els aspectes tècnics interns de C:geo per poder fer ús d’alguns components més moderns proporcionats per Android. Això tindrà un parell d'efectes secundaris, alguns d'ells no desitjats. Informeu de qualsevol error a la nostra [ pàgina de GitHub ](https://www.github.com/cgeo/cgeo/issues) o contactant amb el servei d'assistència.
- Novetat: Admet el mode dia / nit des del sistema (opcional)
- Novetat: Descarregueu llistes de marcadors de geocaching.com; consulteu "Llistes / pocket queries" al menú principal
- Novetat: Ignora la capacitat de geocaching.su
- Canvi: S'ha suprimit l'aplicació de navegació RMAPS que ja no es mantenia
- Solució: extreu el waypoint amb el mateix nom però diferents coordenades de la nota personal
- Solució: Error en l'extracció de la nota d'usuari del waypoint amb fórmula
- Solució: Exporteu la fórmula a PN en lloc de coordenades per a la fórmula completa
- Solució: La carpeta de mapes fora de línia i temes és incorrecta després de tornar a instal·lar i restaurar la còpia de seguretat
- Solució: El track/ruta no es pot actualitzar
- Solució: Error temàtic per al descarregador en el tema lleuger
