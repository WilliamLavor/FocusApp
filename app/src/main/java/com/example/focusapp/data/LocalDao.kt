package com.example.focusapp.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao {

    // LISTAR TODOS OS LOCAIS
    @Query("SELECT * FROM locais ORDER BY nome")
    fun observarTodos(): Flow<List<Local>>

    // BUSCAR UM LOCAL PELO ID
    @Query("SELECT * FROM locais WHERE id = :id")
    suspend fun buscarPorId(id: Int): Local?

    // INSERIR UM LOCAL
    @Insert
    suspend fun inserir(local: Local)

    // ATUALIZAR UM LOCAL
    @Update
    suspend fun atualizar(local: Local)

    // EXCLUIR UM LOCAL
    @Delete
    suspend fun excluir(local: Local)
}
