# ADR-003: Sealed Classes no Domain + JPA Entities Separados

**Status:** Aceito
**Data:** 2026-03-24

## Contexto

O domain model do ScopeFlow usa sealed classes (Java 21) para modelar estados: `User permits UserActive, UserInactive, UserDeleted`. JPA/Hibernate nao suporta sealed classes nativamente como discriminator strategy. Precisamos decidir como persistir entidades com state-based polymorphism.

## Decisao

Manter **domain classes puras (sem anotacoes JPA)** e criar **JPA entities separadas** no adapter layer. Cada repository adapter converte entre domain e JPA usando metodos `toDomain()` e `fromDomain()`.

O status da sealed class e armazenado como `VARCHAR` na coluna `status` da tabela. O adapter usa `switch` expression para reconstruir o subtipo correto:

```java
return switch (jpa.getStatus()) {
    case "ACTIVE" -> new UserActive(id, email, hash, ...);
    case "INACTIVE" -> new UserInactive(id, email, hash, ...);
    case "DELETED" -> new UserDeleted(id, email, hash, ...);
    default -> throw new IllegalStateException("Unknown: " + jpa.getStatus());
};
```

## Alternativas Consideradas

### Opcao A: Anotar domain classes com JPA (Single Table Inheritance)

- Pros: Menos codigo, sem conversores manuais
- Contras: Domain depende de Jakarta Persistence; sealed classes + @Inheritance tem bugs no Hibernate 6.x; domain impuro

### Opcao B: Domain separado + JPA separado (escolhida)

- Pros: Domain 100% puro (zero dependencias de framework); testavel sem banco; sealed classes funcionam perfeitamente; pattern matching no adapter
- Contras: Mais codigo (converter para cada entidade); risco de divergencia entre domain e JPA

### Opcao C: Mapped Superclass sem sealed

- Pros: Simples, Hibernate suporta bem
- Contras: Perde type safety das sealed classes; domain nao pode usar pattern matching

## Consequencias

- Cada bounded context tera ~2-4 JPA entities com converters bidirecionais
- Testes de domain rodam sem banco (unit tests puros)
- Testes de adapter exigem Testcontainers (integacao real com PostgreSQL)
- Se Hibernate adicionar suporte a sealed classes no futuro, podemos migrar gradualmente
- Risco mitigado: testes de integracao validam que toDomain/fromDomain sao consistentes
