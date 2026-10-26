# FixTag

## Âmbito final do MVP

Dentro: identificação por QR ou número de série, ocorrência com descrição, gravidade, fotos e localização, funcionamento offline com sincronização, estado do pedido com notificações, perfis (cliente e assistência).

Fora (trabalho futuro): pedido de peças, garantia, integração com software de gestão de pedidos, leitura de códigos de erro do controlador, etc.

## Como experimentar (variante demo)

A variante `demo` funciona sem servidor e sem credenciais: os dados de exemplo são criados no dispositivo na primeira utilização.

1. Abrir a pasta `android/` no Android Studio.
2. Em *Build Variants*, escolher `demoDebug`.
3. Correr num emulador ou dispositivo com Android 10 (API 29) ou superior.
4. Escolher um perfil: **Operador** (abre em Máquinas) ou **Assistência** (abre em Ocorrências).

O protocolo experimental do tema 3 está em [`docs/protocolo-experimental.pdf`](docs/protocolo-experimental.pdf).

## Entregas

| Data | Branch | Conteúdo |
|---|---|---|
| 27/10 | [entrega-10-27](https://github.com/0xmaiev/FixTag/tree/entrega-10-27) | Tema 3: persistência local e funcionamento offline (variante demo) |
| 04/11 | a publicar | API, sincronização com WorkManager, atribuição de técnicos |
| 06/12 | a publicar | Cidadãos, espaços públicos e mapa de ocorrências |

## Estrutura do repositório

- `android/`: a app, com as variantes `demo` (sem servidor) e `full` (com a API)
- `backend/`: a API REST (Rust, Axum e PostgreSQL) usada pela variante `full`
- `docs/`: protocolo experimental, especificação da API e diagramas

### Stack

| Camada | Escolha | Porquê |
|---------------------------|---------------|-----------------------|
| Linguagem e UI	| XML Views + Fragments	| Fragments com navegação (grafo XML) | 
| Arquitetura | MVVM + Repository (camadas UI e Data) |	Separação clara e fácil de documentar|
| Base de dados local |	Room |	Fonte de verdade da app, base do offline-first |
| Sincronização em background | `WorkManager` |	Executa quando há rede, com retries e backoff |
| Câmara | `TakePicture` | Leitura offline, sem depender de serviços externos |
| Localização	| `FusedLocationProviderClient` (planeado)	| Posição anexada à ocorrência
| Voz	|  `MediaRecorder`	| Útil para quem trabalha com luvas
| Rede |	`Retrofit` + `OkHttp` + `Gson`	| API REST |
| Notificações	| Firebase Cloud Messaging (planeado)	| Alterações de estado do pedido |
| Preferências e sessão |	DataStore	| Substitui o SharedPreferences |

### Estrutura em camadas

```text
Fragment -> ViewModel -> Repository <-> Room (fonte de verdade)
                             |
                             v
                        WorkManager -> API REST (só com rede)
```

O ponto central é que a UI nunca fala com a rede. Lê e escreve sempre na Room, e a sincronização acontece por trás.

### Offline-first: como funciona

O operador cria a ocorrência, que é gravada na Room com `syncStatus = PENDING` e um `UUID` gerado no cliente.
É agendado um `OneTimeWorkRequest` com a restrição `NetworkType.CONNECTED`.
O worker envia primeiro os dados e depois as fotos (upload em separado), e atualiza o `syncStatus` para `SYNCED` ou `FAILED`.
O UUID do cliente torna o envio idempotente: se o pedido for repetido por falha de rede, o servidor não cria duplicados.
As alterações de estado feitas pela assistência chegam por FCM ou por pull periódico, e a Room é atualizada.

Para conflitos, uma regra simples e defensável é: campos descritivos da ocorrência são editáveis só por quem a criou, o estado só pela assistência, e o histórico é só de acrescentar (append-only). Assim quase não há conflitos reais para resolver.

### Modelo de dados

- Organization (id, nome, tipo)
- User (id, nome, role, organizationId)
- Machine (id, serialNumber, model, organizationId)
- Incident (id, machineId, autor, descrição, gravidade, estado, latitude, longitude, createdAt, syncStatus)
- Attachment (id, incidentId, tipo, path, syncStatus, fetchStatus)
- IncidentUpdate (id, incidentId, autor, mensagem, novoEstado, data)