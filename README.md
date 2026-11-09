# FocusApp (Silenciômetro)

Aplicativo Android nativo, **open-source**, para ajudar estudantes universitários a manterem o foco. O diferencial é usar os sensores do celular durante a sessão de estudo: o **sensor de proximidade** confirma que o aparelho está virado para baixo na mesa e o **microfone** mede o nível de ruído do ambiente.

Projeto Final (AP2) da disciplina **RUS0291 – Desenvolvimento de Software para Dispositivos Móveis** (UFC – Campus Russas).

> Este README descreve o que já foi feito, o que falta, a arquitetura e as ferramentas usadas, para que quem entrar no projeto consiga se localizar rápido.

---

## Como o app funciona

1. Na tela principal o usuário digita a matéria e o app fica em **"AGUARDANDO CELULAR"**.
2. Ao virar o celular para baixo na mesa, o **sensor de proximidade** detecta que a frente do aparelho foi coberta (ou o **acelerômetro** detecta que a tela está voltada para baixo) e o app entra na **tela de foco**. A partir daí o foco **continua mesmo que o celular seja desvirado**: o sensor serve apenas para iniciar a sessão.
3. Na tela de foco:
   - as **cores são invertidas** (fundo vira a cor do texto e vice-versa);
   - não existe nenhum botão ou campo para tocar;
   - o botão voltar não faz nada e a tela não apaga sozinha;
   - o **ruído do ambiente** continua sendo medido e exibido em dB.
4. **Regra de ciclo de vida:** se o usuário sair do app durante o foco, a sessão é considerada **abandonada**. Se completar o tempo, é **concluída com sucesso**. (O timer ainda não está implementado, então por enquanto a única forma de encerrar o foco é sair do app.)
5. As sessões ficam salvas no **histórico**, com matéria, data, duração, status e média de ruído. Cada sessão pode ser aberta, editada ou excluída.

> O Android não permite que um app comum bloqueie o botão Home. Por isso a regra é *detectar* a saída (`ON_STOP`) e encerrar o foco, em vez de impedi-la.

---

## Telas

| Tela | Pacote | O que mostra |
|---|---|---|
| **Foco** | `ui/focus` | Campo da matéria, timer, estado do sensor, ruído atual em dB, botão para o histórico. Troca para a tela de foco (cores invertidas) quando o celular é virado para baixo. |
| **Histórico** | `ui/history` | Lista (`LazyColumn`) de cards com matéria, status (SUCESSO / ABANDONADO), data, minutos e média de dB. Tocar no card abre os detalhes. |
| **Detalhes** | `ui/details` | Recebe o `sessionId` pela navegação. Mostra os dados da sessão e permite editar matéria e anotações, salvar ou excluir. |

Rotas (Navigation Compose): `focus`, `history`, `details/{sessionId}`.

---

## Arquitetura

Pacote base: `com.example.focusapp`

```
com.example.focusapp
├── MainActivity.kt          # Só aplica o tema e chama FocusApp()
├── ui
│   ├── FocusApp.kt          # Cria o NavController e carrega o AppNavHost
│   ├── focus/FocusScreen.kt
│   ├── history/HistoryScreen.kt
│   ├── details/DetailScreen.kt
│   └── theme/               # Cores, tipografia e tema Material 3
├── navigation
│   ├── NavRoutes.kt         # Sealed class com as rotas (Focus, History, Details)
│   └── AppNavHost.kt        # Grafo de navegação
├── sensor
│   ├── SensorProximidade.kt # Lê o sensor de proximidade (SensorManager)
│   ├── SensorVirado.kt      # Lê o acelerômetro para saber se a tela está virada para baixo
│   └── MedidorRuido.kt      # Lê o microfone (AudioRecord) e calcula o dB
└── data                     # Persistência com Room (em construção)
    ├── FocusSession.kt      # @Entity: id, materia, duration, date, notas (+ status e mediaDb planejados)
    ├── FocusDao.kt          # Operações CRUD
    └── AppDatabase.kt       # Banco Room (singleton)
```

### Responsabilidades

- **`ui`**: só desenha a interface e reage a eventos. Componentes do Material 3 (`Scaffold`, `TopAppBar`, `Card`, `OutlinedTextField`, `Button`).
- **`navigation`**: define as rotas e a passagem de argumentos (`sessionId` do histórico para os detalhes).
- **`sensor`**: classes simples que encapsulam o hardware. A tela não conhece os detalhes de `SensorManager` nem de `AudioRecord`; ela só chama `iniciar()`, `parar()` e recebe valores.
- **`data`**: persistência local (Room).

### Estado

Hoje o estado da tela principal (`materia`, `focando`, `ruidoDb`, `temPermissao`) fica na própria `FocusScreen`, com `remember` e `mutableStateOf`. A próxima etapa é elevar esse estado para um **ViewModel** (*state hoisting*), deixando as telas apenas com estado e callbacks.

---

## Ferramentas e bibliotecas

| Item | Uso |
|---|---|
| **Kotlin** | Linguagem do projeto |
| **Jetpack Compose** (via BOM) | UI declarativa, sem nenhum layout XML |
| **Material Design 3** | Componentes visuais e tema |
| **Navigation Compose** `2.8.4` | Navegação entre as telas e argumentos |
| **Activity Compose** | `setContent`, `rememberLauncherForActivityResult` (pedido de permissão) e `BackHandler` |
| **Core KTX** | `ContextCompat.checkSelfPermission` |
| **Lifecycle Runtime KTX** | `LifecycleEventObserver` para reagir ao app ir para segundo plano |
| **Material Icons Extended** | Ícones (histórico, voltar, lixeira) |
| **Room** *(planejado)* | Banco de dados local com CRUD completo |
| **Gradle Kotlin DSL + Version Catalog** | Build e versões em `gradle/libs.versions.toml` |
| **APIs nativas do Android** | `SensorManager` (proximidade e acelerômetro) e `AudioRecord` (microfone), sem bibliotecas extras |

Configuração do build: `minSdk 26` (Android 8.0), `compileSdk 37`, `targetSdk 37`.

O app é **offline-first**: não usa internet, não declara a permissão `INTERNET` e não depende de rede, JSON ou serialização.

---

## Sensores e permissões

### Sensor de proximidade
- Fica na frente do aparelho, perto do alto-falante. Com o celular virado para baixo, ele é coberto.
- Considera-se "perto" quando o valor lido é menor que o alcance máximo do sensor (`maximumRange`).
- Só fica ligado enquanto o app está visível (liga em `ON_START`, desliga em `ON_STOP`).
- Ele apenas **inicia** o foco: depois que o foco começa, levantar o celular não o encerra.

### Acelerômetro
- Complementa o sensor de proximidade: com capinha alta, o sensor de proximidade pode não detectar a mesa.
- Lê o eixo Z da gravidade: perto de +9,8 m/s² com a tela para cima e de -9,8 m/s² com a tela para baixo. Abaixo de -7 consideramos "virado para baixo".
- Como a proximidade, apenas **inicia** o foco.

### Microfone
- Permissão: `RECORD_AUDIO`, pedida em tempo de execução ao abrir a tela principal.
- Se for negada, o app **não quebra**: mostra "Microfone sem permissão" e um botão para pedir de novo.
- O nível é calculado pelo **RMS** das amostras de áudio, convertido para decibéis (`20 * log10(rms)`).
- **Atenção:** o valor é *relativo* e não calibrado. Serve para comparar sessões entre si, mas não é um decibelímetro profissional.
- A medição é feita fora da thread principal (`Dispatchers.IO`) e o microfone é liberado quando a tela sai de cena.

---

## Status do desenvolvimento

- [x] Telas em Jetpack Compose com Material 3 (Foco, Histórico, Detalhes)
- [x] Navegação com argumento (`sessionId`)
- [x] Leitura do microfone e exibição do ruído em dB
- [x] Pedido de permissão do microfone com tratamento de negação
- [x] Sensor de proximidade ligando a tela de foco com cores invertidas *(em validação)*
- [x] Regra de ciclo de vida: sair do app encerra o foco *(em validação)*
- [ ] Contagem regressiva do timer
- [ ] ViewModel e *state hoisting*
- [ ] Persistência com Room (criar, ler, atualizar e excluir sessões)
- [ ] Histórico e Detalhes lendo dados reais do banco (hoje usam dados fixos)
- [ ] Salvar a sessão com status (SUCESSO / ABANDONADO) e média de ruído

### Requisitos da disciplina (AP2)

| Requisito | Situação |
|---|---|
| UI declarativa com Compose (sem XML) | Atendido |
| Material Design 3 | Atendido |
| 3 ou mais telas, com argumentos entre duas delas | Atendido |
| Estado (`State`, `remember`, `mutableStateOf`) e *state hoisting* | Parcial (falta o ViewModel) |
| Room com CRUD completo, funcionando offline | Em andamento |
| Sensor físico usado na lógica da interface | Implementado, em validação |

---

## Como rodar

1. Clone o repositório:
   ```bash
   git clone https://github.com/WilliamLavor/FocusApp.git
   ```
2. Abra a pasta no **Android Studio** e aguarde o *Sync* do Gradle. Se faltar, instale o **Android SDK Platform 37** em *Tools → SDK Manager*.
3. Rode em um **celular físico** (Android 8.0 ou superior) com a depuração USB ativada. O sensor de proximidade e o microfone funcionam muito melhor em aparelho real do que no emulador.
4. Aceite a permissão do microfone na primeira abertura.

Para testar o foco: vire o celular para baixo na mesa, ou cubra com a mão a parte de cima da frente do aparelho. Depois pode desvirar: o foco continua. Para encerrar, saia do app (botão Home).

---

## Convenções

- Nomes de classes, variáveis e comentários do domínio do app estão em **português** (`MedidorRuido`, `SensorProximidade`, `materia`).
- Preferimos o código **simples e direto**, usando só o que foi visto na disciplina.
- Não versionar `local.properties`, a pasta `build/` nem a pasta `.gradle/`.

---

## Autores

- William ([@WilliamLavor](https://github.com/WilliamLavor))