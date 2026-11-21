# FocusApp

Aplicativo Android nativo desenvolvido para ajudar estudantes universitários a manterem o foco durante os estudos. O aplicativo utiliza o acelerômetro para detectar quando o celular é colocado com a tela para baixo, inicia uma sessão Pomodoro de 25 minutos e monitora o ruído do ambiente.

Projeto Final (AP2) da disciplina **RUS0291 – Desenvolvimento de Software para Dispositivos Móveis**, da Universidade Federal do Ceará (UFC) – Campus Russas.

---

## Como o aplicativo funciona

1. Na tela principal, o usuário informa a matéria que deseja estudar.
2. O aplicativo aguarda o celular ser colocado com a tela para baixo.
3. O acelerômetro verifica a orientação e a estabilidade do aparelho. É necessário permanecer na posição por aproximadamente 2 segundos para iniciar o foco.
4. Após a confirmação, começa uma sessão Pomodoro de 25 minutos.
5. Durante o foco:
   - A tela apresenta uma contagem regressiva.
   - O aplicativo mede e exibe o nível relativo de ruído do ambiente em decibéis.
   - A interface utiliza cores invertidas para reduzir distrações.
   - O botão de voltar é bloqueado e a tela permanece ligada.
6. Quando o cronômetro chega a zero, a sessão é salva com status `CONCLUIDO`.
7. Caso o usuário saia do aplicativo antes do término, a sessão é salva com status `ABANDONADO`.

As sessões ficam armazenadas localmente no banco Room e podem ser consultadas no histórico.

---

## Telas

| Tela | Pacote | Funcionalidades |
|---|---|---|
| **Foco** | `ui/focus` | Campo da matéria, aguardando detecção do celular, cronômetro, medição de ruído e tela de foco. |
| **Histórico** | `ui/history` | Lista de sessões armazenadas no banco, com matéria, data, duração, status e média de ruído. |
| **Detalhes** | `ui/details` | Consulta, edição e exclusão de uma sessão selecionada. |

A navegação entre as telas é realizada com **Navigation Compose**. A tela de detalhes recebe o identificador da sessão (`sessionId`) como argumento.

---

## Arquitetura

Pacote base: `com.example.focusapp`

```text
com.example.focusapp
├── MainActivity.kt
├── ui
│   ├── FocusApp.kt
│   ├── focus
│   │   └── FocusScreen.kt
│   ├── history
│   │   └── HistoryScreen.kt
│   ├── details
│   │   └── DetailScreen.kt
│   └── theme
│       └── # Cores, tipografia e tema Material 3
├── navigation
│   ├── NavRoutes.kt
│   └── AppNavHost.kt
├── sensor
│   ├── SensorVirado.kt
│   ├── SensorProximidade.kt
│   └── MedidorRuido.kt
└── data
    ├── FocusSession.kt
    ├── FocusDao.kt
    └── AppDatabase.kt
```

### Responsabilidades

- **ui:** contém as telas e os componentes da interface desenvolvidos com Jetpack Compose.
- **navigation:** define as rotas e a navegação entre as telas.
- **sensor:** encapsula o acesso aos sensores físicos e ao microfone.
- **data:** contém as entidades, operações de persistência e configuração do banco Room.

### Estado da aplicação

O estado da tela de foco é gerenciado atualmente na própria `FocusScreen`, utilizando `remember`, `mutableStateOf`, `mutableIntStateOf` e `mutableLongStateOf`.

A próxima evolução arquitetural planejada é utilizar ViewModel para separar melhor a lógica de negócio da interface.

---

## Sensores e recursos do aparelho

### Acelerômetro (`SensorVirado.kt`)

O acelerômetro é responsável por detectar quando o celular está com a tela voltada para baixo.

A implementação utiliza três condições principais:

- Orientação: o eixo Z precisa atingir o limite de -9,0 m/s².
- Estabilidade: a intensidade da aceleração deve estar próxima da gravidade.
- Permanência: a posição precisa ser mantida por aproximadamente 2 segundos.

Também existe uma pequena histerese na saída da posição, reduzindo oscilações na detecção.

O sensor é utilizado somente para iniciar a sessão. Levantar o celular depois que o foco começou não encerra o cronômetro.

### Sensor de proximidade (`SensorProximidade.kt`)

O projeto mantém uma implementação do sensor de proximidade, que permite identificar objetos próximos à parte frontal do aparelho.

Atualmente, ele não participa da ativação automática da sessão. O mecanismo principal de ativação é o acelerômetro.

### Microfone (`MedidorRuido.kt`)

O microfone é utilizado para medir o nível de ruído do ambiente.

- A permissão `RECORD_AUDIO` é solicitada em tempo de execução.
- A leitura é realizada utilizando `AudioRecord`.
- As amostras são processadas com cálculo RMS e convertidas para decibéis.
- A medição ocorre em uma thread de trabalho, fora da thread principal.
- O valor é atualizado na interface aproximadamente a cada 500 ms.
- A média das medições é armazenada junto à sessão.

**Observação:** os valores em dB são relativos e não representam uma medição profissional calibrada. Servem principalmente para acompanhar e comparar o ambiente durante as sessões.

---

## Cronômetro Pomodoro

O FocusApp utiliza uma sessão padrão de 25 minutos.

A contagem regressiva é baseada em `SystemClock.elapsedRealtime()`, evitando depender diretamente do relógio civil do aparelho.

Ao finalizar:

- O cronômetro chega a zero.
- A sessão é registrada no banco Room.
- O status é definido como `CONCLUIDO`.
- A duração registrada é de 25 minutos.
- O aplicativo retorna à tela inicial e apresenta uma mensagem de conclusão.

Caso o aplicativo seja interrompido antes do término, a sessão é registrada como `ABANDONADO`.

---

## Banco de dados

A persistência local é realizada com **Room**, utilizando SQLite.

A entidade `FocusSession` armazena informações como:

| Campo | Descrição |
|---|---|
| `id` | Identificador da sessão |
| `materia` | Matéria estudada |
| `data` | Data e horário de início |
| `duracaoMinutos` | Duração da sessão em minutos |
| `status` | `CONCLUIDO` ou `ABANDONADO` |
| `mediaDb` | Média de ruído durante a sessão |
| `notas` | Anotações da sessão |

O DAO concentra as operações de consulta, inserção, atualização e exclusão.

O histórico observa os dados armazenados no banco, e a tela de detalhes permite consultar e modificar as informações de uma sessão.

---

## Ferramentas e bibliotecas

| Tecnologia | Utilização |
|---|---|
| Kotlin | Linguagem principal |
| Jetpack Compose | Desenvolvimento da interface |
| Material Design 3 | Componentes e identidade visual |
| Navigation Compose | Navegação entre telas |
| Room | Persistência local com SQLite |
| KSP | Processamento de anotações do Room |
| Coroutines | Operações assíncronas |
| Lifecycle | Gerenciamento do ciclo de vida da Activity |
| SensorManager | Acesso ao acelerômetro e proximidade |
| AudioRecord | Captura e processamento de áudio |
| Gradle Kotlin DSL | Configuração e compilação |

O aplicativo funciona localmente e não depende de conexão com a internet.

---

## Status do desenvolvimento

- [x] Interface com Jetpack Compose e Material Design 3.
- [x] Navegação entre Foco, Histórico e Detalhes.
- [x] Cadastro e edição da matéria.
- [x] Integração com banco de dados Room.
- [x] Operações de persistência das sessões.
- [x] Histórico de sessões.
- [x] Consulta, edição e exclusão de sessões.
- [x] Detecção da orientação do aparelho pelo acelerômetro.
- [x] Filtro de estabilidade para evitar disparos acidentais.
- [x] Contagem regressiva Pomodoro de 25 minutos.
- [x] Registro automático de sessões concluídas.
- [x] Registro de sessões abandonadas.
- [x] Medição e exibição do ruído ambiente.
- [x] Armazenamento da média de ruído por sessão.
- [ ] Implementação de ViewModel e separação do estado da interface.
- [ ] Testes automatizados.
- [ ] Melhorias na experiência visual e nos controles de sessão.

---

## Como executar

1. Clone o repositório:

   ```bash
   git clone https://github.com/WilliamLavor/FocusApp.git
   ```

2. Abra o projeto no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Utilize um celular físico Android 8.0 ou superior, com depuração USB ativada.
5. Execute o aplicativo e conceda a permissão de microfone quando solicitada.

### Como testar

1. Abra o aplicativo.
2. Informe a matéria desejada.
3. Coloque o celular com a tela para baixo sobre uma superfície estável.
4. Aguarde aproximadamente 2 segundos.
5. Confira a contagem regressiva e a medição de ruído.
6. Aguarde o término da sessão para verificar o registro no histórico.

Para testar rapidamente o término do cronômetro, pode-se alterar temporariamente a constante `DURACAO_SESSAO_SEGUNDOS` no `FocusScreen.kt`.

---

## Convenções do projeto

- Classes, variáveis e comentários relacionados ao domínio do aplicativo utilizam nomes em português.
- O código procura manter simplicidade e acompanhar os conceitos trabalhados na disciplina.
- Não versionar arquivos locais de configuração, como `local.properties`, pastas `build/` ou `.gradle/`.

---

## Autor

- William Lavor — [@WilliamLavor](https://github.com/WilliamLavor)