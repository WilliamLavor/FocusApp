package com.example.focusapp.ui.focus

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.alerta.AlertaFim
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import com.example.focusapp.sensor.MedidorRuido
import com.example.focusapp.sensor.SensorVirado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// limites dos minutos (foco e descanso)
const val FOCO_MIN_MINUTOS = 1
const val FOCO_MAX_MINUTOS = 180
const val DESCANSO_MIN_MINUTOS = 0   // 0 = sem descanso
const val DESCANSO_MAX_MINUTOS = 60

private const val FOCO_PADRAO_MINUTOS = 25
private const val DESCANSO_PADRAO_MINUTOS = 5

// Estado e lógica da sessão. As duas telas (Nova sessão e timer) usam o mesmo.
class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).focusDao()
    private val sensorVirado = SensorVirado(application)
    private val alerta = AlertaFim(application)

    // vem da tela Nova sessão
    var materia by mutableStateOf("")
        private set
    var focoMinutos by mutableIntStateOf(FOCO_PADRAO_MINUTOS)
        private set
    var descansoMinutos by mutableIntStateOf(DESCANSO_PADRAO_MINUTOS)
        private set

    // estado que a tela observa
    var focando by mutableStateOf(false)
        private set
    var pausado by mutableStateOf(false)
        private set
    var emDescanso by mutableStateOf(false)
        private set
    var tempoRestanteSegundos by mutableIntStateOf(FOCO_PADRAO_MINUTOS * 60)
        private set
    var ruidoDb by mutableIntStateOf(0)
        private set
    var sessaoConcluida by mutableStateOf(false)
        private set

    // sessão que acabou e ainda espera a avaliação para ser salva
    var sessaoPendente by mutableStateOf<FocusSession?>(null)
        private set

    val aguardandoAvaliacao: Boolean
        get() = sessaoPendente != null

    val acelerometroExiste: Boolean = sensorVirado.existe

    // controle interno
    private var temPermissao = false
    private var inicioSessao = 0L
    private var fimPrevistoMs = 0L          // quando o foco acaba (elapsedRealtime)
    private var restanteAoPausarMs = 0L     // quanto faltava ao pausar
    private var somaRuido = 0
    private var quantidadeMedicoes = 0
    private var cronometroJob: Job? = null
    private var descansoJob: Job? = null
    private var ruidoJob: Job? = null

    init {
        // virou: inicia ou retoma. desvirou: pausa
        sensorVirado.aoMudar = { virado ->
            if (virado) {
                if (focando) retomar() else iniciarFoco()
            } else {
                pausar()
            }
        }
    }

    // ações da tela

    fun configurarSessao(novaMateria: String, foco: Int, descanso: Int) {
        if (focando) return

        encerrarDescanso()
        materia = novaMateria.trim()
        focoMinutos = foco.coerceIn(FOCO_MIN_MINUTOS, FOCO_MAX_MINUTOS)
        descansoMinutos = descanso.coerceIn(DESCANSO_MIN_MINUTOS, DESCANSO_MAX_MINUTOS)
        sessaoConcluida = false
        tempoRestanteSegundos = focoMinutos * 60
    }

    fun pularDescanso() {
        encerrarDescanso()
    }

    fun retomarFoco() {
        retomar()
    }

    // encerra e salva como abandonada
    fun encerrarSessao() {
        if (focando) abandonarSessao()
    }

    // salva a sessão pendente com a nota e começa o descanso
    fun salvarAvaliacao(estrelas: Int, fatores: List<String>) {
        val pendente = sessaoPendente ?: return

        sessaoPendente = null
        salvar(
            pendente.copy(
                avaliacao = estrelas.coerceIn(0, 5),
                fatores = fatores.joinToString(",")
            )
        )

        if (descansoMinutos > 0) iniciarDescanso()
    }

    fun pularAvaliacao() {
        salvarAvaliacao(0, emptyList())
    }

    // app saiu de cena na avaliação: salva sem nota e sem descanso
    fun salvarPendenteSemAvaliacao() {
        val pendente = sessaoPendente ?: return

        sessaoPendente = null
        salvar(pendente)
    }

    fun definirPermissaoMicrofone(concedida: Boolean) {
        temPermissao = concedida
        if (focando) iniciarMedicaoRuido()
    }

    fun aoIniciarApp() {
        sensorVirado.iniciar()
    }

    fun aoPararApp() {
        sensorVirado.parar()
        // sessão em andamento vira abandonada
        if (focando) abandonarSessao()
        if (emDescanso) encerrarDescanso()
    }

    // a tela saiu (ex.: foi pro histórico)
    fun pausarSensor() {
        sensorVirado.parar()
    }

    private fun iniciarFoco() {
        // no descanso não começa foco novo
        if (focando || emDescanso) return

        inicioSessao = System.currentTimeMillis()
        fimPrevistoMs = SystemClock.elapsedRealtime() + focoMinutos * 60_000L
        tempoRestanteSegundos = focoMinutos * 60
        pausado = false
        sessaoConcluida = false
        somaRuido = 0
        quantidadeMedicoes = 0
        focando = true

        iniciarCronometro()
        iniciarMedicaoRuido()
    }

    private fun iniciarCronometro() {
        cronometroJob?.cancel()
        val fim = fimPrevistoMs

        cronometroJob = viewModelScope.launch {
            while (isActive) {
                val faltaMs = (fim - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                // arredonda pra cima pra não mostrar 00:00:00 antes da hora
                val restante = ((faltaMs + 999L) / 1000L).toInt()
                tempoRestanteSegundos = restante

                if (restante == 0) {
                    concluirSessao()
                    break
                }
                delay(1000)
            }
        }
    }

    // pausa o timer e o microfone, guardando o que faltava
    private fun pausar() {
        if (!focando || pausado) return

        restanteAoPausarMs = (fimPrevistoMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        cronometroJob?.cancel()
        cronometroJob = null
        ruidoJob?.cancel()
        ruidoJob = null
        pausado = true
    }

    private fun retomar() {
        if (!focando || !pausado) return

        fimPrevistoMs = SystemClock.elapsedRealtime() + restanteAoPausarMs
        pausado = false
        iniciarCronometro()
        iniciarMedicaoRuido()
    }

    private fun iniciarMedicaoRuido() {
        ruidoJob?.cancel()
        if (!temPermissao || !focando || pausado) return

        ruidoJob = viewModelScope.launch {
            val medidor = MedidorRuido()
            medidor.iniciar()

            try {
                while (isActive) {
                    val leitura = withContext(Dispatchers.IO) { medidor.lerDb() }
                    ruidoDb = leitura

                    // leitura 0 não entra na média
                    if (leitura > 0) {
                        somaRuido += leitura
                        quantidadeMedicoes++
                    }
                    delay(500)
                }
            } finally {
                medidor.parar()
            }
        }
    }

    private fun mediaRuido(): Int =
        if (quantidadeMedicoes > 0) somaRuido / quantidadeMedicoes else 0

    private fun concluirSessao() {
        val sessao = FocusSession(
            materia = materia.ifBlank { "Estudo livre" },
            data = inicioSessao,
            duracaoMinutos = focoMinutos,
            status = "CONCLUIDO",
            mediaDb = mediaRuido()
        )

        encerrarEstado()
        sessaoConcluida = true
        alerta.tocar()

        // só salva depois da avaliação; o descanso começa junto
        sessaoPendente = sessao
    }

    private fun abandonarSessao() {
        if (inicioSessao > 0L) {
            // minutos estudados de verdade (sem contar a pausa)
            val minutos = ((focoMinutos * 60 - tempoRestanteSegundos) / 60.0)
                .roundToInt()
                .coerceAtLeast(1)

            val sessao = FocusSession(
                materia = materia.ifBlank { "Estudo livre" },
                data = inicioSessao,
                duracaoMinutos = minutos,
                status = "ABANDONADO",
                mediaDb = mediaRuido()
            )
            salvar(sessao)
        }
        encerrarEstado()
    }

    // limpa o estado do foco
    private fun encerrarEstado() {
        inicioSessao = 0L
        fimPrevistoMs = 0L
        restanteAoPausarMs = 0L
        ruidoJob?.cancel()
        cronometroJob?.cancel()
        cronometroJob = null
        pausado = false
        focando = false
        tempoRestanteSegundos = focoMinutos * 60
    }

    private fun iniciarDescanso() {
        descansoJob?.cancel()
        val inicio = SystemClock.elapsedRealtime()
        val totalSegundos = descansoMinutos * 60

        emDescanso = true
        tempoRestanteSegundos = totalSegundos

        descansoJob = viewModelScope.launch {
            while (isActive) {
                val decorrido = ((SystemClock.elapsedRealtime() - inicio) / 1000L).toInt()
                val restante = (totalSegundos - decorrido).coerceAtLeast(0)
                tempoRestanteSegundos = restante

                if (restante == 0) {
                    alerta.tocar()
                    encerrarDescanso()
                    break
                }
                delay(1000)
            }
        }
    }

    // acabou o descanso (ou pulou)
    private fun encerrarDescanso() {
        descansoJob?.cancel()
        descansoJob = null
        emDescanso = false
        if (!focando) tempoRestanteSegundos = focoMinutos * 60
    }

    private fun salvar(sessao: FocusSession) {
        viewModelScope.launch { dao.inserir(sessao) }
    }

    override fun onCleared() {
        sensorVirado.parar()
        super.onCleared()
    }
}
