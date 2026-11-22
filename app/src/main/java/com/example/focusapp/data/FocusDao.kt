package com.example.focusapp.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {


    //  LISTAR TODAS AS SESSÕES
    @Query("SELECT * FROM sessoes ORDER BY data DESC")
    fun observarTodas(): Flow<List<FocusSession>>


    //  SESSÕES A PARTIR DE UMA DATA (PARA AS MÉTRICAS)
    @Query("SELECT * FROM sessoes WHERE data >= :desde ORDER BY data DESC")
    fun observarDesde(desde: Long): Flow<List<FocusSession>>

    //  BUSCAR UMA SESSÂO PELO ID
    @Query("SELECT * FROM sessoes WHERE id = :id")
    suspend fun buscarPorId(id: Int): FocusSession?


    //  INSERIR UMA SESSÃO
    @Insert()
    suspend fun inserir(sessao: FocusSession)

    //  ATUALIZAR UMA SESSÂO
    @Update
    suspend fun atualizar(sessao: FocusSession)


    //  EXCLUIR UMA SESSÂO
    @Delete
    suspend fun excluir(sessao: FocusSession)

    //  TIRAR O LOCAL DAS SESSÕES QUE USAVAM ELE
    @Query("UPDATE sessoes SET localId = NULL WHERE localId = :localId")
    suspend fun removerLocal(localId: Int)
}