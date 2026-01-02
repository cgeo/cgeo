##
- Hora d'actualitzar! Si encara feu servir Android 7 o una versió anterior, aquesta podria ser la darrera actualització de c:geo! Amb la propera versió de c:geo, deixarem de tenir compatibilitat amb Android 5-7 per reduir la càrrega de manteniment i poder actualitzar alguns components externs que utilitza c:geo i que actualment encara estem retenint. Continuarem donant suport a Android 8 fins a Android 16 aleshores (i a les versions més noves quan es publiquin), un període de més de vuit anys d'història d'Android.
- Canvi: S'ha reduït a 100 el nombre màxim de visites de rastrejables de GC per registre de catxé (segons la sol·licitud de geocaching.com per reduir la càrrega del servidor causada pels amants extrems dels rastrejables)
- Correcció: Algunes possibles excepcions de seguretat quan l'usuari no ha atorgat certs drets (per exemple: notificacions)
- Correcció: Cercles de catxés incomplets en nivells de zoom baixos (només VTM)
- Correcció: Error en recarregar punts de referència en determinades condicions de càrrega
- Correcció: El filtre de data d'esdeveniment no funciona en determinades condicions
- Correcció: El límit màxim de línia de registre no funciona de manera fiable en la configuració "il·limitada"
- Correcció: Error en obrir el mapa sota certes condicions
- Correcció: No es mostra el mapa si wherigo no té zones visibles
- Correcció: Error a la pestanya d'imatge dels detalls de catxé en determinades condicions
- Correcció: Cerques de mapes amb coordenades no vàlides
- Fix: Some translations do not respect c:geo-internal language setting

##
- Canvi: UnifiedMap s'ha definit com a mapa predeterminat per a tothom (com a part del nostre full de ruta cap a UnifiedMap). Podeu tornar a canviar a "configuració" - "fonts del mapa" de moment. L'eliminació dels mapes antics està prevista per a la primavera del 2026 a les nostres versions habituals.
- Correcció: La casella de selecció de preferits es restableix en tornar a entrar a la pantalla de registre fora de línia
- Correcció: El quadre d'entrada del radi de la geozona mostra un nombre decimal
- Correcció: La sincronització de notes personals no funciona
- Canvi: Nova icona per a la importació de rutes/tracks GPX a la configuració ràpida de rutes/tracks del mapa

##
- Correcció: Els valors negatius del gràfic d'elevació no s'escalen
- Correcció: Coordenades properes a 0 trencades a les exportacions GPX
- Correcció: Alguns errors
- Intenta solucionar: ANR a l'inici
- Intenta solucionar: Falten dades de catxés al mapa en directe

##
- Correcció: Error en la cerca de paraules clau
- Correcció: Error al mapa
- Correcció: El text de la pista ja no es pot seleccionar
- Correcció: Diversos problemes amb Wherigo

##
- Correcció: Xifrar/desxifrar una pista requereix un toc addicional inicialment
- Correcció: Wherigo s'ha bloquejat en llegir partides guardades antigues
- Correcció: De vegades no es recorda el registre des de c:geo
- Correcció: Falta una actualització de dades en directe per a trobats & catxés arxivats
- Correcció: Els punts de referència del mapa fora de línia de vegades no es mostren

##
- Correcció: Suggeriments de catxé sense xifrar (canvi de lloc web)
- Correcció: Els Labs no es carregaven a l'aplicació (canvi de lloc web, haureu d'actualitzar els Adventure labs emmagatzemades per poder tornar-les a cridar des de c:geo)
- Correcció: UnifiedMap VTM: L'activació o desactivació d'edificis en 3D no funciona per a mapes combinats
- Correcció: Traducció fora de línia: De vegades es detecta l'idioma de la llista com a --

##
- Correcció: Error al mòdul de traducció
- Correcció: La detecció d'inici de sessió falla (canvi de lloc web)
- Correcció: Error en recuperar el cartutx Wherigo
- Correcció: "Carrega'n més" no respecta els filtres fora de línia

##
- Correcció: L'inventari rastrejable no es carregava en registrar un catxé

##
- Correcció: La migració de catxés definits per l'usuari durant l'inici de c:geo falla => s'ha eliminat de moment'
- Correcció: Les tasques de Wherigo finalitzades no es marquen com a finalitzades o fallides
























