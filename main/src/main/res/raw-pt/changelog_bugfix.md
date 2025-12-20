##
- Time to update! If you are still using Android 7 or older, this might be the last c:geo update for you! With our next feature release of c:geo we will drop support for Android 5-7 to reduce our maintenance load and to be able to update some external components used by c:geo which we are currently still holding back. We will still be supporting Android 8 up to Android 16 then (and newer versions when they will be published), a span of more than eight years of Android history.
- Change: Maximum number of GC trackables visiting per cache log reduced to 100 (as per request from geocaching.com to reduce their server load caused by extreme trackable lovers)
- Fix: Some possible security exceptions when user has not granted certain rights (eg.: notifications)
- Fix: Cache circles incomplete on low zoom levels (VTM only)

##
- Change: UnifiedMap set as default map for anyone (as part of our roadmap to UnifiedMap) You can switch back in "settings" - "map sources" for the time being. Removal of legacy maps is planned for spring 2026 in our regular releases.
- Correcção: A caixa de seleção de favorito é reposta ao regressar ao ecrã de registo offline
- Fix: Geofence radius input box shows decimal number
- Fix: Syncing of personal notes not working
- Change: New icon for GPX track/route import in map track/route quick settings

##
- Correcção: Valores negativos no gráfico de altitude não eram dimensionados
- Correcção: Coordenadas próximas de 0 ficavam inválidas na exportação para GPX
- Correcção: Alguns encerramentos abruptos
- Tentativa de correcção: Aplicação Não Responde no arranque
- Tentativa de correcção: Dados de geocache em falta no mapa ao vivo

##
- Correcção: Falha na pesquisa por palavra-chave
- Correcção: Bloqueio no mapa
- Correcção: Texto de dica não era seleccionável
- Correcção: Vários erros de Wherigo

##
- Correcção: Criptografar/Descriptografar uma dica obrigava, na primeira vez, a um toque extra
- Correcção: Falha Wherigo ao ler jogos gravados antigos
- Correcção: O registo a partir do c:geo por vezes não era recordado
- Correcção: Actualização de dados ao vivo em falta, para caches encontradas & arquivadas
- Correcção: Pontos adicionais no mapa offline por vezes não eram mostrados

##
- Correcção: Dicas de cache não encriptadas (alteração do site)
- Correcção: As Adventure Lab não eram carregadas, devido a alteração no site (para poder executá-las novamente a partir do c:geo, terá de actualizar as Adventure Lab armazenadas)
- Correcção: Mapa Unificado VTM: Alternar edifícios 3D não funciona para mapas combinados
- Correcção: Tradução offline: O idioma da descrição às vezes é detectado como --

##
- Correcção: Falha no módulo de tradução
- Correcção: Falha na detecção do início de sessão (devido a mudança no sítio Web)
- Correcção: Falha ao obter cartucho Wherigo
- Correcção: "Carregar mais" não respeitava os filtros offline

##
- Correcção: Inventário de Trackables não era carregado enquanto registava uma cache

##
- Correcção: Migração de caches definidas pelo utilizador durante a inicialização do c:geo falhava => removida a funcionalidade, por enquanto
- Correcção: As tarefas Wherigo finalizadas não eram marcadas como terminadas ou falhadas



















