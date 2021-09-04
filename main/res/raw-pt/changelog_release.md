### Fixes
- "Add to watchlist" / "Remove from watchlist" failing (Website change)
- "Add to favorite" / "Remove from favorite" buttons not shown after "found" log
- Date in logbook cut off on larger fonts
- Filtering in live map for more rare cache types returning only few results

## Bugfix Release 2021.08.28

### Aparência
- Increase font size for text input fields
- Increase font size for some compass elements
- Use font color with higher contrast in waypoint tab
- Make quick offline log check mark visible again
- Increase font size for coordinate input fields
- Respect system font size settings also on older Android versions (5,6 and 7)

### Detalhes da cache
- Corrige o título da cache em falta se a cache for aberta pelo geocode ou ligação (alteração do site)
- Corrige a descrição da cache em falta, em algumas caches

### Outro
- Mostra novamente as caches premium nos resultados de pesquisa de membros básicos
- Corrige a criação adicional de caches definidas pelo utilizador, se algumas caches definidas pelo utilizador tiverem sido carregadas via GPX
- Usa abreviaturas em inglês mais comuns, para as caches tradicionais, no filtro por tipo de cache

## Versão de Funcionalidades 2021.08.15:

### Sistema de filtragem avançado
- Apresentamos um novo sistema de filtragem no c:geo, que suporta filtros flexíveis, combináveis e armazenáveis
- Disponível nas listas de caches e visualização de mapas
- Nova função "Pesquisar por filtro"

### Mapa
- Novo: Ao criar uma cache definida pelo utilizador enquanto exibe um mapa de uma lista – é permitido que o utilizador armazene a nova cache na lista actual (em vez da lista padrão para caches definidas pelo utilizador)
- Novo: Filtros separados para "minhas" e "encontradas" nas configurações rápidas do mapa
- Alteração: Adicionalmente é mostrado o nome da cache nos detalhes do pop-up

### Detalhes da cache
- Novo: Utilização do Google Tradutor numa janela pop-up de tradução
- Novo: Permite a alteração do ícone atribuído, ao tocar sem soltar sobre o nome da cache, na janela pop-up (apenas em caches armazenadas)

### Gestor de Transferências
- Alteração: As transferências agora decorrem, completamente, em segundo plano. Uma notificação é mostrada
- Alteração: Os ficheiros transferidos com sucesso irão substituir automaticamente os ficheiros já existentes com o mesmo nome
- Alteração: Se um mapa requer um determinado tema que ainda não esteja instalado, o c:geo também descarregará e instalará esse tema automaticamente

### Outro
- Alteração: Reformulámos completamente os aspectos técnicos internos de personalização do c:geo para que se possa tirar partido de alguns componentes mais modernos disponibilizados pelo Android. Isto terá efeitos colaterais, alguns dos quais não intencionais. Por favor, reporte quaisquer erros ou falhas na nossa [página do GitHub](https://www.github.com/cgeo/cgeo/issues) ou entre em contacto com o suporte.
- Novo: Modo dia/noite do sistema do dispositivo (opcional)
- Novo: Transferir as listas de geocaching.com - consulte "Listas / pocket queries" no menu principal
- Novo: Ignorar a capacidade para geocaching.su
- Alteração: Removida a aplicação de navegação RMAPS, já sem manutenção
- Correcção: Extracção do ponto adicional a partir das notas pessoais, com o mesmo nome mas tendo coordenadas diferentes
- Correcção: Erro na extracção de nota do utilizador num ponto adicional com fórmula
- Correcção: Exportar fórmula para Nota Pessoal, em vez das coordenadas, para fórmula completa
- Correcção: pasta de temas e mapas offline incorrecta, após a reinstalação e restauro de cópia de segurança
- Correcção: Não poder actualizar percurso/rota
- Correcção: Erro de tema no gestor de transferências com o tema claro
