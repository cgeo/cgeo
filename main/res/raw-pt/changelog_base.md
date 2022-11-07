
### Aviso: mudança de nível de API
Devido às próximas restrições na Play Store, actualizámos o nível de API do Android em questão. Isto não deve afectar o uso do c:geo e ainda deve funcionar a partir do Android 5 em diante, mas se notar quaisquer irregularidades, entre em contacto connosco através de support@cgeo.org

### Mapa
- Novo: Permitir a mudança do nome do percurso

### Detalhes da cache
- Correcção: A imagem de registo era rotulada de "Imagem 1" mesmo quando só era adicionada uma única imagem
- Novo: espaço reservado para o modelo de registo de GEO-CÓDIGO
- Novo: Suporte à formatação básica de HTML para listas definidas (dl/dt/dd)
- Novo: Ampliação da imagem em visualização (ao tocar numa imagem da descrição)
- Correcção: Abrir ligações nas descrições no navegador web integrado (se estiver activo)
- Alteração: Renderizar a descrição da cache em segundo plano e limitar o seu tamanho a 50.000 caracteres, por defeito
- Change: GCVote service connection disabled due to severe performance problems - You can manually re-enable it using Settings - Services - GCVote
- New: Log caches: Preserve last trackable action per trackable

### Geral
- Novo: Exibição das transferências pendentes
- Novo: Acrescentar o nome da cache / nome da lista ao nome do ficheiro na exportação para GPX
- Alteração: Removida a definição "Identificar como navegador Android"
- Novo: Verificar transferências pendentes (mapas / dados de encaminhamento) no arranque
- Novo: Possibilidade de seleccionar os ficheiros a transferir
- Novo: Filtro para o estado DNF
- Novo: Exibir elevação no ecrã inicial (se disponível)
- Novo: Permitir a introdução manual de valores em filtros, usando controles deslizantes
- Novo: Activar o envio de coordenadas modificadas para caches importadas a partir dum ficheiro GPX, quando a cache tem um ponto adicional do tipo "ORIGINAL"
- Alteração: Melhorar o texto da linha de estado do filtro
- Change: Use a better readable color for archived cache names in titles and remove coloring from cache details page
