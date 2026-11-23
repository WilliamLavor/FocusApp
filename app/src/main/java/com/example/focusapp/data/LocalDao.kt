package com.example.focusapp.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao {

    //lista todos
    @Query("SELECT * FROM locais ORDER BY nome")
    fun observarTodos(): Flow<List<Local>>

    //busca por id
    @Query("SELECT * FROM locais WHERE id = :id")
    suspend fun buscarPorId(id: Int): Local?

    //insere
    @Insert
    suspend fun inserir(local: Local)

    //atualiza
    @Update
    suspend fun atualizar(local: Local)

    //exclui
    @Delete
    suspend fun excluir(local: Local)
}
