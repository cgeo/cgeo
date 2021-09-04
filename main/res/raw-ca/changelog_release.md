### Correccions
- Error en "Afegeix a la llista de seguiment" / "Elimina de la llista de seguiment" (canvi de lloc web)
- Els botons "Afegeix als preferits" / "Elimina dels preferits" no apareixen després del registre "trobat"
- Data del registre tallada en tipus de lletra més grans
- El filtrat al mapa en temps real per obtenir tipus de catxés més rars proporciona pocs resultats

## Versió de correcció d'errors 2021.08.28

### Disseny
- Augmentada la mida de la lletra per als camps d’entrada de text
- Augmentada la mida de la lletra per a alguns elements de la brúixola
- Utilitzat el color de la lletra amb un contrast més alt a la pestanya waypoint
- Tornat a fer visible la marca de verificació del registre fora de línia
- Augmentada la mida del tipus de lletra per als camps d’entrada de coordenades
- Respectada la configuració de la mida del tipus de lletra del sistema també en versions anteriors d'Android (5,6 i 7)

### Detalls del catxé
- Corregit el títol del catxé que faltava si s'obria mitjançant un geocodi o un enllaç (canvi de lloc web)
- Corregiu la descripció del catxé que faltava en alguns catxés

### Altres
- Mostra de nou els catxés premium als resultats de cerca de membres bàsics
- Corregit la creació de catxés definides per l'usuari si alguns usuaris defineixen que s'han carregat catxés mitjançant GPX
- Utilitzada una abreviatura més comuna en anglès per el catxé tradicional al filtre de tipus de catxés

## Versió de funcions 2021.08.15:

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
