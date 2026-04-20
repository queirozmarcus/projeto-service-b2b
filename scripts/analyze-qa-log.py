#!/usr/bin/env python3
import re
import sys

log_path = sys.argv[1] if len(sys.argv) > 1 else 'logs/qa-validation-20260419-200650.log'

with open(log_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Split backend e user-service
sections = content.split('=' * 50)
backend_section = ''
user_service_section = ''

for i, section in enumerate(sections):
    if 'Backend Tests' in section or 'backend' in section.lower():
        backend_section = sections[i] + (sections[i+1] if i+1 < len(sections) else '')
    if 'User Service Tests' in section or 'user-service' in section.lower():
        user_service_section = sections[i] + (sections[i+1] if i+1 < len(sections) else '')

# Extrair resultados do backend
backend_match = re.search(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)', backend_section)
backend = {'total': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
if backend_match:
    backend['total'] = int(backend_match.group(1))
    backend['failures'] = int(backend_match.group(2))
    backend['errors'] = int(backend_match.group(3))
    backend['skipped'] = int(backend_match.group(4))

backend_success = 'BUILD SUCCESS' in backend_section and backend_section.index('BUILD SUCCESS') > backend_section.rfind('Tests run:')

# Extrair resultados do user-service
user_match = re.search(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)', user_service_section)
user = {'total': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
if user_match:
    user['total'] = int(user_match.group(1))
    user['failures'] = int(user_match.group(2))
    user['errors'] = int(user_match.group(3))
    user['skipped'] = int(user_match.group(4))

user_success = 'BUILD SUCCESS' in user_service_section and 'BUILD FAILURE' not in user_service_section

# Buscar testes falhados
failed_tests = re.findall(r'(\w+Test).*?<<< (FAILURE|ERROR)!(.*?)(?=\n\w+Test|\nTests run:|\Z)', content, re.DOTALL)

print("=" * 80)
print("RELATÓRIO DE VALIDAÇÃO QA - 2026-04-19 20:06")
print("=" * 80)
print()

backend_passed = backend['total'] - backend['failures'] - backend['errors']
user_passed = user['total'] - user['failures'] - user['errors']
total = backend['total'] + user['total']
total_passed = backend_passed + user_passed

print("### RESUMO EXECUTIVO")
print()
print(f"Backend:       {backend_passed}/{backend['total']} testes passaram")
print(f"User-service:  {user_passed}/{user['total']} testes passaram")
print(f"TOTAL:         {total_passed}/{total} testes")
print()
print(f"Status Backend:       {'✅ BUILD SUCCESS' if backend_success else '❌ BUILD FAILURE'}")
print(f"Status User-service:  {'✅ BUILD SUCCESS' if user_success else '❌ BUILD FAILURE'}")
print()

if backend['failures'] > 0 or backend['errors'] > 0 or user['failures'] > 0 or user['errors'] > 0:
    print("=" * 80)
    print("### PROBLEMAS IDENTIFICADOS (PRIORIDADE ALTA)")
    print("=" * 80)
    print()

    for i, (test_class, error_type, details) in enumerate(failed_tests[:20], 1):
        method_match = re.search(r'(\w+)\(', details)
        method = method_match.group(1) if method_match else 'unknown'

        cause_match = re.search(r'(Exception|Error):\s*(.*?)(?=\n\s*at|\Z)', details, re.DOTALL)
        cause = cause_match.group(0)[:150] if cause_match else details[:150]

        print(f"{i}. {test_class}.{method}()")
        print(f"   Tipo: {error_type}")
        print(f"   Causa: {cause.strip()}")
        print()
else:
    print("✅ TODOS OS TESTES PASSARAM!")

print("=" * 80)
print("### VALIDAÇÃO DAS CORREÇÕES ANTERIORES")
print("=" * 80)
print()

user_test_failed = any('UserTest' in t[0] for t in failed_tests)
email_test_failed = any('InvalidEmailValidationIntegrationTest' in t[0] for t in failed_tests)

print("UserTest:", "❌ ainda falha" if user_test_failed else "✅ correções OK")
print("InvalidEmailValidationIntegrationTest:", "❌ ainda falha" if email_test_failed else "✅ correções OK")
