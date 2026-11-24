package com.example.focusapp.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {


    //lista todas
    @Query("SELECT * FROM sessoes ORDER BY data DESC")
    fun observarTodas(): Flow<List<FocusSession>>


    //a partir de uma data (métricas)
    @Query("SELECT * FROM sessoes WHERE data >= :desde ORDER BY data DESC")
    fun observarDesde(desde: Long): Flow<List<FocusSession>>

    //busca por id
    @Query("SELECT * FROM sessoes WHERE id = :id")
    suspend fun buscarPorId(id: Int): FocusSession?


    //insere
    @Insert()
    suspend fun inserir(sessao: FocusSession)

    //atualiza
    @Update
    suspend fun atualizar(sessao: FocusSession)


    //exclui
    @Delete
    suspend fun excluir(sessao: FocusSession)

    //apaga o histórico todo
    @Query("DELETE FROM sessoes")
    suspend fun excluirTodas()

    //tira o local das sessões que usavam ele
    @Query("UPDATE sessoes SET localId = NULL WHERE localId = :localId")
    suspend fun removerLocal(localId: Int)
}