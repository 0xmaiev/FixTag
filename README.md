# FixTag

Devido a limites de tamanho na submissão do trabalho no Moodle, os APKs das variantes Demo e Full (que requer o [backend](backend/README.md)), encontram-se no repositório em [apk-demo-debug](apk/app-demo-debug.apk) e [apk-full-debug](apk/app-full-debug.apk).

## Âmbito final do MVP

Dentro: identificação por QR ou número de série, ocorrência com descrição, gravidade, fotos e localização, funcionamento offline com sincronização, estado do pedido com notificações, perfis (cliente e assistência).

Fora (trabalho futuro): pedido de peças, garantia, integração com software de gestão de pedidos, leitura de códigos de erro do controlador, etc.

## Como experimentar (variante demo)

A variante `demo` funciona sem servidor e sem credenciais: os dados de exemplo são criados no dispositivo na primeira utilização.

1. Abrir a pasta `android/` no Android Studio.
2. Em *Build Variants*, escolher `demoDebug`.
3. Correr num emulador ou dispositivo com Android 10 (API 29) ou superior.
4. Escolher uma organização: **Cliente** ou **Fabricante**.
5. Escolher um dos diversos perfis para cada tipo de organização.
6. Ver secção abaixo

## Como experimentar (variante full)

A variante `full` funciona com servidor e com credenciais: os dados de exemplo são inseridos na instância postgresql e o servidor realiza queries contra o.

1. Abrir a pasta `android/` no Android Studio.
2. Em *Build Variants*, escolher `fullDebug`.
3. Correr num emulador ou dispositivo com Android 10 (API 29) ou superior.
4. Ver instruções para preparar o servidor em [README.md](backend/README.md)
5. Escolher um tipo de perfil de acordo com os dados no [README.md](backend/README.md)
6. Fazer o login e experimentar a aplicação

## Protocolo Experimental

O protocolo experimental do tema 3 está em [`docs/protocolo-experimental.pdf`](docs/protocolo-experimental.pdf), em ambos os casos acima, grande parte do que está descrito no protocolo mantém-se.

Adicionam-se as seguintes funcionalidades:
- Adição de dados de localização GPS a uma Ocorrência.
- Atribuição de Ocorrências aos Técnicos de uma Organização por parte dos membros da mesma cuja função seja de Assistência ou Gestor.

As seguintes requerem o servidor:
- Como Gestor de uma organização Cliente ou Fabricante, a adição de Membros à mesma.
- Como Gestor de uma organização Fabricante, a criação de organizações Cliente, sendo que os seus Gestores têm a sua conta criada com uma palavra-passe temporária, e ao fazer o login com o mesmo, é pedido que a palavra-passe seja alterada.

## Entregas

| Data | Branch | Conteúdo |
|---|---|---|
| 27/10 | [entrega-10-27](https://github.com/0xmaiev/FixTag/tree/entrega-10-27) | Tema 3: persistência local e funcionamento offline (variante demo) |
| 04/11 | [entrega-11-08](https://github.com/0xmaiev/FixTag/tree/entrega-11-08) | API, sincronização com WorkManager, atribuição de técnicos |
| 06/12 | a publicar | Cidadãos, espaços públicos e mapa de ocorrências |

## Estrutura do repositório

- `android/`: a app, com as variantes `demo` (sem servidor) e `full` (com a API)
- `apk/`: apks para ambas as variantes, `demo` (sem servidor) e `full` (com a API)
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