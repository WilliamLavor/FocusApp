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

// Limites aceitos para os minutos escolhidos pelo usuário
const val FOCO_MIN_MINUTOS = 1
const val FOCO_MAX_MINUTOS = 180
const val DESCANSO_MIN_MINUTOS = 0   // 0 = sem descanso
const val DESCANSO_MAX_MINUTOS = 60

private const val FOCO_PADRAO_MINUTOS = 25
private const val DESCANSO_PADRAO_MINUTOS = 5

// Guarda o estado e a lógica da sessão (cronômetro, descanso, sensores, ruído e banco).
// As telas só desenham o estado e repassam eventos.
// Este ViewModel é compartilhado entre a tela "Nova sessão" e a tela do timer.
class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).focusDao()
    private val sensorVirado = SensorVirado(application)

    // Configuração escolhida na tela "Nova sessão"
    var materia by mutableStateOf("")
        private set
    var focoMinutos by mutableIntStateOf(FOCO_PADRAO_MINUTOS)
        private set
    var descansoMinutos by mutableIntStateOf(DESCANSO_PADRAO_MINUTOS)
        private set

    // Estado observado pela interface
    var focando by mutableStateOf(false)
        private set
    var emDescanso by mutableStateOf(false)
        private set
    var tempoRestanteSegundos by mutableIntStateOf(FOCO_PADRAO_MINUTOS * 60)
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
    private var descansoJob: Job? = null
    private var ruidoJob: Job? = null

    init {
        // Só inicia o foco quando o acelerômetro confirma a posição.
        sensorVirado.aoMudar = { virado ->
            if (virado) iniciarFoco()
        }
    }

    // Eventos vindos da interface

    // Chamado pela tela "Nova sessão" ao tocar em "Iniciar sessão".
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
        if (emDescanso) encerrarDescanso()
    }

    // Chamado quando a tela sai da composição (ex.: abriu o histórico).
    fun pausarSensor() {
        sensorVirado.parar()
    }

    // Lógica da sessão

    private fun iniciarFoco() {
        // Durante o descanso, virar o celular não inicia um novo foco.
        if (focando || emDescanso) return

        inicioSessao = System.currentTimeMillis()
        inicioCronometroMs = SystemClock.elapsedRealtime()
        tempoRestanteSegundos = focoMinutos * 60
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
        val totalSegundos = focoMinutos * 60

        cronometroJob = viewModelScope.launch {
            while (isActive) {
                val decorrido = ((SystemClock.elapsedRealtime() - inicio) / 1000L).toInt()
                val restante = (totalSegundos - decorrido).coerceAtLeast(0)
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
            duracaoMinutos = focoMinutos,
            status = "CONCLUIDO",
            mediaDb = mediaRuido()
        )

        encerrarEstado()
        sessaoConcluida = true
        salvar(sessao)

        // Depois do foco vem o descanso (se o usuário escolheu mais de 0 minutos).
        if (descansoMinutos > 0) iniciarDescanso()
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

    // Zera o estado do foco e para cronômetro e microfone.
    private fun encerrarEstado() {
        inicioSessao = 0L
        inicioCronometroMs = 0L
        ruidoJob?.cancel()
        cronometroJob = null
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
                    encerrarDescanso()
                    break
                }
                delay(1000)
            }
        }
    }

    // Termina o descanso (acabou o tempo ou o usuário pulou) e deixa o timer pronto para o próximo foco.
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
