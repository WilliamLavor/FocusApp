package com.example.focusapp.ui.locais

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.AppDatabase
import com.example.focusapp.data.Local
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// CRUD dos locais
class LocaisViewModel(application: Application) : AndroidViewModel(application) {

    private val banco = AppDatabase.getDatabase(application)
    private val localDao = banco.localDao()
    private val sessaoDao = banco.focusDao()

    val locais: Flow<List<Local>> = localDao.observarTodos()

    //id 0 = local novo
    fun salvar(local: Local) {
        viewModelScope.launch {
            if (local.id == 0) {
                localDao.inserir(local)
            } else {
                localDao.atualizar(local)
            }
        }
    }

    // sessões desse local ficam sem local
    fun excluir(local: Local) {
        viewModelScope.launch {
            sessaoDao.removerLocal(local.id)
            localDao.excluir(local)
        }
    }
}
