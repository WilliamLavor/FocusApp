package com.example.focusapp.ui.metricas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import kotlinx.coroutines.flow.Flow

private const val DIA_MS = 24L * 60 * 60 * 1000

class MetricasViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).focusDao()

    //dias = 0 pega tudo
    fun sessoes(dias: Int): Flow<List<FocusSession>> =
        dao.observarDesde(if (dias > 0) System.currentTimeMillis() - dias * DIA_MS else 0L)
}
