package com.example.focusapp.ui.metricas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.FocusSession
import kotlinx.coroutines.flow.Flow

private const val TRINTA_DIAS_MS = 30L * 24 * 60 * 60 * 1000

class MetricasViewModel(application: Application) : AndroidViewModel(application) {

    //últimos 30 dias a partir de quando a tela abre
    val sessoes: Flow<List<FocusSession>> = AppDatabase.getDatabase(application)
        .focusDao()
        .observarDesde(System.currentTimeMillis() - TRINTA_DIAS_MS)
}
