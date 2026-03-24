# ADR-005: Lazy Loading por Default em Relacionamentos JPA

**Status:** Aceito
**Data:** 2026-03-24

## Contexto

JPA entities no ScopeFlow tem relacionamentos entre si (Proposal -> ProposalVersions, Workspace -> Members, etc.). Precisamos definir a estrategia de fetch (LAZY vs EAGER) para evitar problemas de performance em producao.

## Decisao

**LAZY por default em todos os relacionamentos.** Nenhuma `@OneToMany` ou `@ManyToOne` sera EAGER. Dados relacionados sao carregados explicitamente via queries separadas no repository adapter ou via `@EntityGraph` quando necessario.

Na pratica, como usamos **JPA entities separadas do domain** (ADR-003) e os converters fazem mapeamento campo-a-campo (sem navegar relacionamentos JPA), a maioria das entities nao tera relacionamentos JPA anotados. Em vez disso, usa UUID como FK e queries separadas:

```java
// Em vez de @ManyToOne EAGER:
@Column(name = "workspace_id")
private UUID workspaceId;  // FK como UUID simples

// Dados do workspace carregados via query separada quando necessario
```

## Alternativas Consideradas

### Opcao A: EAGER nos relacionamentos mais usados

- Pros: Conveniente; menos queries em casos comuns
- Contras: N+1 em listagens; dados carregados mesmo quando nao usados; dificil de controlar

### Opcao B: LAZY por default + EntityGraph quando necessario (escolhida)

- Pros: Performance previsivel; controle total sobre queries; sem N+1 surpresa
- Contras: Mais queries em cenarios onde todos os dados sao necessarios; precisa de EntityGraph ou fetch join

### Opcao C: Sem relacionamentos JPA (UUID FKs)

- Pros: Maximo controle; sem LazyInitializationException; alinhado com bounded context isolation
- Contras: Sem cascading; sem joins automaticos; mais codigo manual

## Consequencias

- **Sem LazyInitializationException:** Como nao usamos LAZY proxies (usamos UUID FKs), esse problema nao existe
- **Queries explicitas:** Cada repositorio faz queries explicitas para dados que precisa
- **Performance previsivel:** Nenhuma query escondida por EAGER fetch
- **Trade-off:** Para telas que mostram Proposal + Versions + Scope, precisa de 2-3 queries. Aceitavel para MVP (volume baixo)
- **Otimizacao futura:** Se performance for problema, adicionar `@EntityGraph` ou cache (Redis) em queries hot
- **Monitoring:** Adicionar `hibernate.generate_statistics=true` em dev para detectar N+1
