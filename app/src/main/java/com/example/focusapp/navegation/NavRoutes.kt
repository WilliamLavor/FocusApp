package com.example.focusapp.navegation

/**
 * Define todas as rotas de navegação da aplicação.
 * Utiliza-se uma "sealed class" para garantir a segurança de tipos e
 * agrupar as rotas estáticas e dinâmicas num único local.
 */
sealed class NavRoutes(val route: String) {

    // Rota para o Ecrã 1: Temporizador e Sensor
    data object Focus : NavRoutes("focus")

    // Rota para o Ecrã 2: Histórico de Sessões
    data object History : NavRoutes("history")

    // Rota para o Ecrã 3: Detalhes e Edição da Sessão
    data object Details : NavRoutes("details/{sessionId}") {
        /**
         * Cria a string exata para navegar para os detalhes de uma sessão específica.
         * @param sessionId O ID da sessão selecionada no histórico.
         * @return A rota formatada, por exemplo: "details/5"
         */
        fun createRoute(sessionId: Int): String {
            return "details/$sessionId"
        }
    }
}