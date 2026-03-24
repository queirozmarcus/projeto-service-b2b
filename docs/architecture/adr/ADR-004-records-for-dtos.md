# ADR-004: Records para DTOs de Request e Response

**Status:** Aceito
**Data:** 2026-03-24

## Contexto

Precisamos de ~50 DTOs para cobrir requests e responses da API REST. Java 21 oferece records como alternativa a classes tradicionais ou Lombok.

## Decisao

Usar **Java records** para todos os DTOs (request e response). Records sao imutaveis por design, geram `equals()`, `hashCode()`, `toString()` automaticamente, e combinam com Bean Validation annotations.

```java
public record RegisterRequest(
    @NotBlank @Email @Size(max=255) String email,
    @NotBlank @Size(min=8, max=128) String password,
    @NotBlank @Size(min=2, max=255) String fullName,
    @Size(max=20) String phone
) {}
```

## Alternativas Consideradas

### Opcao A: Classes com Lombok (@Data, @Value)

- Pros: Familiar; suporta builders; mais flexivel
- Contras: Dependencia extra (Lombok); problemas com IDE; nao padrao Java; mutavel por default (@Data)

### Opcao B: Records (escolhida)

- Pros: Java nativo (sem dependencias); imutavel por design; menos boilerplate que Lombok; Jackson deserializa corretamente; Bean Validation funciona com compact constructor
- Contras: Nao suporta heranca; sem builders nativos (usa static factory methods se necessario)

### Opcao C: Classes manuais (POJO)

- Pros: Maximo controle; sem surpresas
- Contras: Muito boilerplate (getters, setters, equals, hashCode, toString); propenso a erros

## Consequencias

- Todos os DTOs usam `record` sem excecao
- Jackson precisa de `jackson-module-parameter-names` (incluido no Spring Boot 3.2 por default) para deserializacao correta
- Para DTOs com muitos campos opcionais, usar `@JsonInclude(NON_NULL)` no response
- Se precisar de builder pattern no futuro, usar static factory method ou biblioteca separada (nao Lombok)
- Swagger/OpenAPI via `@Schema` annotations funciona normalmente com records
