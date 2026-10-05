# Korczak Nexa

**A próxima geração de planilhas.**

O **Korczak Nexa** é um aplicativo de planilhas da **Korczak Technologies**, dentro do **Korczak HUB**, criado para unir e superar as capacidades do Microsoft Excel e do Google Sheets em uma única plataforma. A ambição é construir o aplicativo de planilhas mais tecnológico, funcional, bonito e atraente possível, com foco inicial no mercado brasileiro e interface em português do Brasil.

## 1. Visão
O Nexa será uma plataforma completa de planilhas, e não apenas um editor de células. Deve combinar planilhas tradicionais, fórmulas, gráficos, tabelas, análise de dados, dashboards, colaboração, automações, importação/exportação e recursos profissionais.

Princípios:
- tecnologia sem complexidade desnecessária;
- interface limpa, futurista e simples;
- verde como cor principal;
- português como idioma inicial;
- desempenho, segurança e acessibilidade desde a fundação;
- compatibilidade entre plataformas;
- arquitetura preparada para crescimento;
- orçamento inicial de R$ 0;
- nenhuma funcionalidade será considerada concluída apenas por existir visualmente.

## 2. Plataformas
| Plataforma | Forma | Objetivo |
|---|---|---|
| Desktop | Aplicativo | Todas as funcionalidades e interface completa |
| Desktop | Navegador | Mesma experiência adaptada ao navegador |
| iOS | PWA | Todas as funcionalidades possíveis em PWA |
| Android | PWA | Todas as funcionalidades possíveis em PWA |
| Android | Aplicativo nativo | Todas as funções em Android Nativo |

A arquitetura deve compartilhar o máximo possível de lógica, modelos, contratos e componentes entre plataformas.

## 3. Funcionalidades
O Nexa deverá evoluir para contemplar as funcionalidades relevantes existentes no Excel e Google Sheets, além de recursos próprios.

### Planilha
Células, linhas, colunas, múltiplas abas, seleção, edição, copiar/colar, preenchimento automático, redimensionamento, congelamento, ocultação, agrupamento, mesclagem, formatação condicional e validação de dados.

### Formatação
Fonte, tamanho, negrito, itálico, sublinhado, tachado, cores, bordas, alinhamento, quebra de texto, formatos numéricos, moeda, porcentagem, data, hora, casas decimais e estilos.

### Fórmulas
Engine própria de fórmulas, preparada para funções simples e avançadas e para compatibilidade progressiva com fórmulas populares do Excel e Google Sheets.

### Dados
Filtros, classificação, tabelas, tabelas dinâmicas, consultas, estatísticas, análise, remoção de duplicados, importação e exportação.

### Visualização
Gráficos, gráficos combinados, dashboards, indicadores e visualizações interativas.

### Arquivos
Suporte progressivo a XLSX, CSV, TSV, ODS e formato próprio do Nexa.

### Colaboração
A arquitetura deverá permitir compartilhamento, permissões, edição colaborativa, comentários, histórico, versões, presença e sincronização.

## 4. Morok AI
A integração futura com a **Morok AI** será planejada desde a arquitetura, mas não é requisito da fundação e o Nexa não poderá depender dela para funcionar. No futuro, poderá auxiliar em fórmulas, análise de dados, gráficos, relatórios, automações, explicações e transformação de dados.

## 5. Planos
O Nexa terá planos pagos e uma opção **Free**. O modelo comercial será implementado depois que a base funcional estiver sólida. Planos e limites deverão ser controlados por configuração, não espalhados pelo código. O plano Free deve ser um produto realmente utilizável.

## 6. Infraestrutura
O orçamento inicial é **R$ 0**. A infraestrutura inicial será baseada em GitHub, GitHub Pages, Render e MongoDB Atlas.

- GitHub: código, versionamento, Actions, Issues, Pull Requests e releases.
- GitHub Pages: experiência web/PWA quando aplicável.
- Render: API e serviços backend.
- MongoDB Atlas: persistência dos dados.

## 7. MongoDB
Projeto MongoDB Atlas: **KOS**.

Cluster: utilizar o único cluster existente no projeto KOS.

Database principal do produto: **KZNexa**.

A KZNexa armazenará os dados próprios do Nexa: planilhas, documentos, abas, configurações, arquivos, preferências, compartilhamentos, histórico e metadados.

## 8. Database global de contas
Database global: **Contas**.

A database Contas pertence ao ecossistema Korczak HUB e armazenará os usuários compartilhados entre produtos, incluindo identidade e informações necessárias para saber se o usuário possui plano ou benefícios do HUB.

### REGRA CRÍTICA
**NUNCA apagar a database Contas.** Nenhuma rotina de limpeza, teste, migração, reset ou manutenção do Nexa poderá remover essa database ou executar operações destrutivas nela sem autorização explícita e cuidadosamente delimitada.

Princípio: identidade global fica no HUB; dados específicos ficam na KZNexa.

## 9. Segurança
Desde a fundação: autenticação, autorização, validação de entrada, controle de acesso, isolamento de dados, sessões seguras, secrets em variáveis de ambiente, CORS adequado, rate limiting quando aplicável e logs sem exposição de dados sensíveis. Secrets nunca devem ser enviados ao GitHub.

## 10. As 4 fases
O projeto possui quatro fases: **Fase 0 — Fundação**, **Fase 1 — Núcleo**, **Fase 2 — Nexa Completo** e **Fase 3 — Ecossistema Nexa**.

## Fase 0 — Fundação
Objetivo: criar todos os arquivos, pastas, contratos e estruturas necessários para o projeto existir corretamente.

Deve estabelecer:
- estrutura do repositório;
- frontend e backend/API;
- arquitetura compartilhada;
- PWA;
- base desktop;
- base Android;
- builds de desenvolvimento e produção;
- testes;
- componentes e serviços;
- modelos de dados e contratos de API;
- configuração MongoDB;
- autenticação;
- ambientes;
- GitHub Actions;
- documentação;
- versionamento;
- tratamento de erros e logging;
- acessibilidade;
- internacionalização;
- sistema visual e temas;
- arquitetura preparada para expansão.

A Fase 0 não deve criar arquivos apenas para preencher pastas. Cada diretório precisa ter responsabilidade clara e a fundação precisa ser executável.

## Fase 1 — Núcleo do Nexa
Objetivo: construir o motor real da planilha.

Prioridades: grid, células, linhas, colunas, seleção, edição, clipboard, múltiplas abas, undo/redo, formatação, fórmulas, cálculo, referências entre células/abas, arquivos, salvamento, carregamento, persistência, importação e exportação.

A engine de planilhas deverá ser independente da interface sempre que possível, permitindo reutilização entre desktop, web, Android e PWA.

## Fase 2 — Nexa Completo
Objetivo: transformar o núcleo em uma plataforma de produtividade comparável às principais soluções do mercado.

Adicionar progressivamente: gráficos, tabelas, tabelas dinâmicas, dashboards, filtros avançados, validação, colaboração, comentários, compartilhamento, permissões, histórico, versões, sincronização, recursos avançados de dados, templates e experiências completas de desktop e mobile.

Performance, acessibilidade, responsividade, UX, estabilidade e compatibilidade de arquivos serão requisitos permanentes.

## Fase 3 — Ecossistema Nexa
Objetivo: transformar o Nexa em uma plataforma integrada ao Korczak HUB.

Possibilidades: planos pagos, integração com Korczak HUB, Morok AI, automações, API pública, integrações externas, extensões, marketplace, recursos empresariais, colaboração avançada, analytics e recursos inteligentes.

A Fase 3 será orientada por produto, mercado e sustentabilidade financeira.

## 11. Engenharia
O projeto deve ser tratado como produto de produção desde o início. Não serão aceitos como solução permanente: código duplicado sem necessidade, funcionalidades falsas, botões sem implementação, telas meramente decorativas, APIs simuladas quando a funcionalidade deveria ser real, dados fictícios apresentados como reais, hardcode de configurações dinâmicas, secrets no código ou dependências desnecessárias.

Uma funcionalidade só está implementada quando existe, funciona, possui tratamento de erro, integração correta, fluxo real, não quebra funcionalidades existentes, é adequada à plataforma e foi testada.

## 12. Performance
Planilhas podem manipular grandes quantidades de dados. A arquitetura deverá considerar virtualização, renderização incremental, memoização, cálculo incremental, workers, processamento assíncrono, cache e otimização de consultas quando necessários.

## 13. Acessibilidade e internacionalização
A interface deverá considerar contraste, teclado, leitores de tela, foco, navegação sem mouse, feedback visual/textual e responsividade. O idioma inicial será **Português do Brasil (pt-BR)**, mas a arquitetura deve permitir outros idiomas futuramente.

## 14. GitHub Actions e deploy
GitHub Actions será utilizado para validação, testes, build, qualidade e deploy quando aplicável. O frontend será hospedado no GitHub Pages quando compatível; a API ficará no Render; o banco ficará no MongoDB Atlas; Android será distribuído por releases e iOS começará como PWA.

## 15. Custos
O orçamento é **R$ 0**. A arquitetura deve priorizar GitHub Free, GitHub Pages, GitHub Actions dentro das cotas, Render Free quando suficiente, MongoDB Atlas Free quando suficiente e software open source. Se uma funcionalidade exigir recurso pago, ela deverá ser otimizada, substituída, adiada ou documentada como requisito futuro.

## 16. Filosofia
O Nexa não será construído como um simples clone. Excel e Sheets são referências funcionais e de compatibilidade. A pergunta de engenharia será: **como podemos fazer isso melhor?**

## 17. Origem
**Produto:** Korczak Nexa  
**Ecossistema:** Korczak HUB  
**Empresa:** Korczak Technologies  
**Integração futura:** Morok AI  
**Infraestrutura:** GitHub + Render + MongoDB Atlas

## 18. Regra máxima
> **Construir primeiro uma fundação correta. Depois construir uma planilha excelente. Depois construir uma plataforma.**

O Nexa deve crescer sem precisar ser reescrito a cada nova funcionalidade.

## Status inicial
**Fase atual:** Fase 1 — Núcleo (implementação em validação)  
**Produto:** Korczak Nexa  
**Idioma:** Português do Brasil  
**Orçamento:** R$ 0  
**Database do produto:** KZNexa  
**Database global:** Contas — **NUNCA APAGAR**  
**Infraestrutura:** GitHub Pages + Render + MongoDB Atlas  
**Plataformas:** Desktop App + Desktop Web + iOS PWA + Android PWA + Android Nativo



<!-- phase1-final-verification -->
