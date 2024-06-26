##
- Fix: Log length check counting some characters twice
- Fix: Adapt to hylly website change
- New: Additional theming options for Google Maps
- Fix: Compass rose hidden behind distance views (Google Maps v2)
- New: Enhance logging in case of GC login errors
- Fix: Editing cache logs does not take care of existing favorite points
- Fix: "Save offline" not working after failing to edit a found log
- New: Option to limit search radius for address search
- New: Show notification for missing location permission

##
- Correcção: Caches não carregam após activar o mapa ao vivo (Mapa Unificado)
- Correcção: Falta a opção 'usar lista actual" na criação de cache definida pelo utilizador (Mapa Unificado)
- Correcção: Bússola escondida atrás das visualizações de distância (Mapa Unificado)
- Correcção: Descrição da cache volta ao início, para o cabeçalho da página, após a edição da nota pessoal
- Novo: Mostrar data do evento no selector de cache
- Correcção: Início de sessão na plataforma OC não era reconhecida pelo assistente de instalação
- Correcção: Encaminhamento não funcionava por padrão, após uma nova instalação
- Correcção: Barra de Ferramentas da página de informações ficava escondida no modo paisagem, até mesmo em dispositivos grandes
- Correcção: "seguir a minha localização" permanecia activa após ampliação com deslocamento (Mapa Unificado)
- Correcção: Rotas individuais exportadas como percurso não podiam ser lidas por dispositivos Garmin
- Correcção: carregar TBs da base de dados interna falhava em certas condições
- Fix: Route to navigation target not recalculated on routing mode change
- Fix: Error while reading available trackable log types

##
- Correcção: Ligações de TB com parâmetro TB não funcionavam
- Novo: Adicionada mensagem para a busca por palavra-chave para membros básicos desactivada
- Correcção: Registo de TBs não funcionavam novamente (alterações no sítio web)
- Correcção: As informações de elevação rodavam com o marcador de posição
- Correcção: Nome de utilizador não detectado durante o início de sessão, se tivesse certos caracteres especiais

##
- Correcção: Mostrar/ocultar pontos adicionais não funcionava correctamente se ultrapassasse os limites do ponto adicional (Mapa Unificado)
- Correcção: Registo de caches ou TB deixaram de funcionar (mudanças no sítio web)
- Correcção: A eliminação dos registos pessoais não funcionava

##
- Correcção: O contador de "encontradas" não era detectado em certas situações devido a mudanças no sítio web
- Correcção: Falha na abertura do mapa com nomes de ficheiro de percurso vazios
- Correcção: Rotação automática do mapa ainda activa após repor definições usando a bússola (Mapa Unificado)
- Correcção: Bússola ausente nos modos de autorrotação no Google Maps (Mapa Unificado)
- Correcção: Registos dos TB não eram carregados devido a alterações no sítio web
- Alteração: Combinar elevação + informações das coordenadas no menu de toque longo no mapa numa única "posição selecionada" + mostrar distância até à posição actual

##
- Novo: Apagar registos offline através de menu de contexto
- Correcção: Apagar o registo offline não funcionava em determinada condição
- Correcção: Nome de filtro perdido na mudança rápida do filtro
- Alteração: Ordenar ficheiros de percurso pelo nome
- Alteração: Guardar acção do TB também para registos offline
- Correcção: Mudança do mapa para as coordenadas 0,0 na alteração do tipo de mapa (Mapa Unificado)
- Correcção: Destino do ponto adicional voltava para a cache, como destino (Mapa Unificado)
- Correcção: "Armazenar" uma cache sem seleccionar uma lista
- Correcção: Falha de início de sessão devido a alteração no sítio web 'geocaching.com'
- Alteração: Mostrar informação de elevação por baixo do marcador de posição (se activado)
- NOTA: Existem mais problemas devido a mudanças recentes no sítio Web geocaching.com, que ainda não foram corrigidos. Estamos a trabalhar para solucionar o problema. Veja a nossa [página de estado ](https://github.com/cgeo/cgeo/issues/15555) para saber do progresso actual.
