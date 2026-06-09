---
status: partial
phase: 06-chat-groups-real-time
source: [06-VERIFICATION.md]
started: 2026-06-08T00:00:00.000Z
updated: 2026-06-08T00:00:00.000Z
---

## Current Test

[aguardando teste humano]

## Tests

### 1. Loop completo de envio/recebimento em tempo real
expected: Uma mensagem de texto aparece em tempo real no thread aberto do outro usuário sem atualizar a página
result: [pending]

### 2. Envio de imagem em anexo
expected: Imagem renderiza na bolha; upload S3 funciona com credenciais reais; estado otimista resolve para bolha final
result: [pending]

### 3. Deep-link da notificação message_received
expected: Tocar na notificação navega para o thread da conversa, NÃO para o perfil do remetente
result: [pending]

### 4. Badge do chat no AppShell
expected: Contagem reflete conversas não lidas; atualiza ao receber eventos socket
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
