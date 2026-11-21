package com.example.focusapp.ui.focus

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

const val DURACAO_SESSAO_SEGUNDOS = 25 * 60

// Guarda o estado e a lógica da tela de foco (cronômetro, sensores, ruído e banco).
// A FocusScreen só desenha o estado e repassa eventos.
class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).focusDao()
    private val sensorVirado = SensorVirado(application)

    // Estado observado pela interface
    var materia by mutableStateOf("")
        private set
    var focando by mutableStateOf(false)
        private set
    var tempoRestanteSegundos by mutableIntStateOf(DURACAO_SESSAO_SEGUNDOS)
        private set
    var ruidoDb by mutableIntStateOf(0)
        private set
    var sessaoConcluida by mutableStateOf(false)
        private set

    val acelerometroExiste: Boolean = sensorVirado.existe

    // Estado interno da sessão
    private var temPermissao = false
    private var inicioSessao = 0L
    private var inicioCronometroMs = 0L
    private var somaRuido = 0
    private var quantidadeMedicoes = 0
    private var cronometroJob: Job? = null
    private var ruidoJob: Job? = null

    init {
        // Só inicia o foco quando o acelerômetro confirma a posição.
        sensorVirado.aoMudar = { virado ->
            if (virado) iniciarFoco()
        }
    }

    // Eventos vindos da interface

    fun alterarMateria(novaMateria: String) {
        materia = novaMateria
    }

    fun limparMateria() {
        materia = ""
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
        // Se havia uma sessão ativa, salva como abandonada.
        if (focando) abandonarSessao()
    }

    // Chamado quando a tela sai da composição (ex.: abriu o histórico).
    fun pausarSensor() {
        sensorVirado.parar()
    }

    // Lógica da sessão

    private fun iniciarFoco() {
        if (focando) return

        inicioSessao = System.currentTimeMillis()
        inicioCronometroMs = SystemClock.elapsedRealtime()
        tempoRestanteSegundos = DURACAO_SESSAO_SEGUNDOS
        sessaoConcluida = false
        somaRuido = 0
        quantidadeMedicoes = 0
        focando = true

        iniciarCronometro()
        iniciarMedicaoRuido()
    }

    private fun iniciarCronometro() {
        cronometroJob?.cancel()
        val inicio = inicioCronometroMs

        cronometroJob = viewModelScope.launch {
            while (isActive) {
                val decorrido = ((SystemClock.elapsedRealtime() - inicio) / 1000L).toInt()
                val restante = (DURACAO_SESSAO_SEGUNDOS - decorrido).coerceAtLeast(0)
                tempoRestanteSegundos = restante

                if (restante == 0) {
                    concluirSessao()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun iniciarMedicaoRuido() {
        ruidoJob?.cancel()
        if (!temPermissao || !focando) return

        ruidoJob = viewModelScope.launch {
            val medidor = MedidorRuido()
            medidor.iniciar()

            try {
                while (isActive) {
                    val leitura = withContext(Dispatchers.IO) { medidor.lerDb() }
                    ruidoDb = leitura

                    // Ignora leituras 0 (silêncio total/falha) para não puxar a média para baixo.
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
            duracaoMinutos = DURACAO_SESSAO_SEGUNDOS / 60,
            status = "CONCLUIDO",
            mediaDb = mediaRuido()
        )

        encerrarEstado()
        sessaoConcluida = true
        salvar(sessao)
    }

    private fun abandonarSessao() {
        if (inicioSessao > 0L) {
            val minutos = ((System.currentTimeMillis() - inicioSessao) / 60000.0)
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

    // Zera o estado da sessão e para cronômetro e microfone.
    private fun encerrarEstado() {
        inicioSessao = 0L
        inicioCronometroMs = 0L
        ruidoJob?.cancel()
        cronometroJob = null
        focando = false
        tempoRestanteSegundos = DURACAO_SESSAO_SEGUNDOS
    }

    private fun salvar(sessao: FocusSession) {
        viewModelScope.launch { dao.inserir(sessao) }
    }

    override fun onCleared() {
        sensorVirado.parar()
        super.onCleared()
    }
}
