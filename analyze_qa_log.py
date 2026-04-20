#!/usr/bin/env python3
"""Analisa o log de validação QA e gera relatório estruturado."""

import re
from collections import defaultdict
from pathlib import Path

def parse_log(log_path):
    """Parse the QA validation log file."""
    with open(log_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract test results summary from the end
    lines = content.split('\n')
    backend_success = backend_total = 0
    user_success = user_total = 0

    for line in reversed(lines[-100:]):
        if 'Backend:' in line and 'testes' in line:
            match = re.search(r'(\d+)/(\d+) testes', line)
            if match:
                backend_success, backend_total = map(int, match.groups())
        elif 'User-service:' in line and 'testes' in line:
            match = re.search(r'(\d+)/(\d+) testes', line)
            if match:
                user_success, user_total = map(int, match.groups())

    total_success = backend_success + user_success
    total_total = backend_total + user_total

    # Parse failed test classes
    failed_classes = []
    for match in re.finditer(r'\[ERROR\] Tests run: (\d+), Failures: (\d+).*?in ([\w\.]+)', content):
        tests_run = int(match.group(1))
        failures = int(match.group(2))
        test_class = match.group(3)
        failed_classes.append({
            'class': test_class.split('.')[-1],
            'full_class': test_class,
            'failures': failures,
            'tests_run': tests_run
        })

    # Parse individual failed test methods
    failed_methods = []
    pattern = r'\[ERROR\]\s+([\w\.]+)\s+Time elapsed:'
    for match in re.finditer(pattern, content):
        method_full = match.group(1)
        # Extract class and method
        parts = method_full.rsplit('.', 1)
        if len(parts) == 2:
            class_part = parts[0].split('.')[-1]
            method_name = parts[1]
        else:
            class_part = "Unknown"
            method_name = method_full

        # Find error context (next 500 chars)
        start = match.end()
        error_context = content[start:start+500]

        # Determine cause
        cause = "Unknown error"
        severity = "Média"

        if "Status = 404" in error_context or "404" in error_context:
            cause = "HTTP 404 - endpoint não encontrado"
            severity = "Alta"
        elif "Status = 401" in error_context or "401" in error_context:
            cause = "HTTP 401 - falha de autenticação"
            severity = "Crítica"
        elif "Status = 400" in error_context or "400" in error_context:
            cause = "HTTP 400 - validação falhou"
            severity = "Alta"
        elif "AssertionFailedError" in error_context or "expected:" in error_context.lower():
            if "status" in error_context.lower():
                status_match = re.search(r'Status = (\d+)', error_context)
                if status_match:
                    cause = f"Status HTTP inesperado: {status_match.group(1)}"
                    severity = "Alta"
                else:
                    cause = "Assertion falhou - verificar resposta HTTP"
                    severity = "Média"
            else:
                cause = "Assertion falhou - verificar lógica de teste"
                severity = "Média"
        elif "NullPointerException" in error_context:
            cause = "NullPointerException - objeto não inicializado"
            severity = "Alta"
        elif "Connection" in error_context or "timeout" in error_context.lower():
            cause = "Timeout ou falha de conexão"
            severity = "Alta"

        failed_methods.append({
            'class': class_part,
            'method': method_name,
            'full_method': method_full,
            'cause': cause,
            'severity': severity
        })

    return {
        'backend_success': backend_success,
        'backend_total': backend_total,
        'user_success': user_success,
        'user_total': user_total,
        'total_success': total_success,
        'total_total': total_total,
        'failed_classes': failed_classes,
        'failed_methods': failed_methods
    }

def categorize_failures(failed_methods):
    """Categorize failures by domain and recommend agents."""
    categories = defaultdict(list)

    for failure in failed_methods:
        class_name = failure['class']

        if 'Auth' in class_name or 'Login' in class_name or 'Register' in class_name:
            categories['Authentication & Authorization'].append(failure)
        elif 'User' in class_name:
            categories['User Management'].append(failure)
        elif 'Email' in class_name or 'InvalidEmail' in class_name:
            categories['Email Validation'].append(failure)
        elif 'Briefing' in class_name:
            categories['Briefing Domain'].append(failure)
        elif 'Workspace' in class_name:
            categories['Workspace Domain'].append(failure)
        elif 'Contract' in class_name or 'Pact' in class_name:
            categories['Contract Tests'].append(failure)
        elif 'Integration' in class_name:
            categories['Integration Tests'].append(failure)
        else:
            categories['Other/Unit Tests'].append(failure)

    return categories

def recommend_agent(category):
    """Recommend the appropriate agent for a failure category."""
    if 'Contract' in category:
        return 'contract-test-engineer', 'Alta'
    elif 'Integration' in category:
        return 'integration-test-engineer', 'Alta'
    elif 'Auth' in category:
        return 'security-test-engineer', 'Crítica'
    elif 'Email' in category:
        return 'backend-dev', 'Alta'
    else:
        return 'unit-test-engineer', 'Média'

def generate_report(data, output_path):
    """Generate structured markdown report."""
    lines = []

    # Header
    lines.append("# Análise de Validação QA - Relatório de Problemas")
    lines.append(f"\n**Data da validação:** 2026-04-19 21:26:54")
    lines.append(f"**Log analisado:** `logs/qa-validation-20260419-212654.log`")
    lines.append("")

    # Executive Summary
    lines.append("## Resumo Executivo\n")
    backend_pct = (data['backend_success'] * 100 // data['backend_total']) if data['backend_total'] > 0 else 0
    user_pct = (data['user_success'] * 100 // data['user_total']) if data['user_total'] > 0 else 0
    total_pct = (data['total_success'] * 100 // data['total_total']) if data['total_total'] > 0 else 0

    lines.append(f"- **Backend:** {data['backend_success']}/{data['backend_total']} testes ({backend_pct}% sucesso)")
    lines.append(f"- **User-service:** {data['user_success']}/{data['user_total']} testes ({user_pct}% sucesso)")
    lines.append(f"- **Total:** {data['total_success']}/{data['total_total']} testes ({total_pct}% sucesso)")
    lines.append("")

    if total_pct == 100:
        lines.append("### ✅ STATUS: TODOS OS TESTES PASSARAM!\n")
        lines.append("Parabéns! A suíte de testes está 100% funcional.")
    else:
        lines.append(f"### ⚠️ STATUS: {data['total_total'] - data['total_success']} TESTES FALHANDO\n")
        lines.append(f"**Total de falhas detectadas:** {len(data['failed_methods'])} métodos de teste")
        lines.append(f"**Classes afetadas:** {len(data['failed_classes'])}")

    lines.append("")

    # Categorized Problems
    if data['failed_methods']:
        lines.append("## Problemas por Categoria\n")

        categories = categorize_failures(data['failed_methods'])

        for category_name in sorted(categories.keys()):
            items = categories[category_name]
            agent, default_severity = recommend_agent(category_name)

            lines.append(f"### {category_name}")
            lines.append(f"**Agent recomendado:** `{agent}`")
            lines.append(f"**Severidade:** {default_severity}")
            lines.append(f"**Total:** {len(items)} problemas\n")

            for i, item in enumerate(items, 1):
                lines.append(f"#### Problema {i}")
                lines.append(f"- **Classe:** `{item['class']}`")
                lines.append(f"- **Método:** `{item['method']}`")
                lines.append(f"- **Causa:** {item['cause']}")
                lines.append(f"- **Severidade individual:** {item['severity']}")
                lines.append(f"- **Método completo:** `{item['full_method']}`")
                lines.append("")

            lines.append("---\n")

    # Failed Classes Summary
    if data['failed_classes']:
        lines.append("## Classes com Falhas (Resumo)\n")
        lines.append("| Classe | Falhas | Testes Executados | Taxa de Falha |")
        lines.append("|--------|--------|-------------------|---------------|")

        for cls in data['failed_classes']:
            failure_rate = (cls['failures'] * 100 // cls['tests_run']) if cls['tests_run'] > 0 else 0
            lines.append(f"| `{cls['class']}` | {cls['failures']} | {cls['tests_run']} | {failure_rate}% |")

        lines.append("")

    # Next Steps
    lines.append("## Próximos Passos Recomendados\n")

    if total_pct == 100:
        lines.append("1. ✅ Validação concluída com sucesso")
        lines.append("2. Considerar aumentar cobertura de testes")
        lines.append("3. Revisar mutation testing (Pitest)")
    else:
        lines.append("1. **Prioridade CRÍTICA:** Corrigir falhas de autenticação (security-test-engineer)")
        lines.append("2. **Prioridade ALTA:** Corrigir endpoints 404 (integration-test-engineer)")
        lines.append("3. **Prioridade MÉDIA:** Revisar assertions e lógica de testes (unit-test-engineer)")
        lines.append("4. Após correções, executar nova validação completa")

    lines.append("")
    lines.append("---")
    lines.append("*Relatório gerado automaticamente pela análise do log de validação QA*")

    # Write report
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print(f"✅ Relatório gerado: {output_path}")
    print(f"\n📊 Estatísticas:")
    print(f"   - Total de testes: {data['total_total']}")
    print(f"   - Sucesso: {data['total_success']} ({total_pct}%)")
    print(f"   - Falhas: {data['total_total'] - data['total_success']}")
    print(f"   - Classes afetadas: {len(data['failed_classes'])}")
    print(f"   - Métodos com falha: {len(data['failed_methods'])}")

if __name__ == '__main__':
    log_path = Path('/home/mq/iGitHub/projeto-service-b2b/logs/qa-validation-20260419-212654.log')
    output_path = Path('/home/mq/iGitHub/projeto-service-b2b/logs/qa-problems-analysis-212654.md')

    print("🔍 Analisando log de validação QA...")
    data = parse_log(log_path)

    print("📝 Gerando relatório estruturado...")
    generate_report(data, output_path)
