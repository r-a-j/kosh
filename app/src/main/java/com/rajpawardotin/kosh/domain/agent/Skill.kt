package com.rajpawardotin.kosh.domain.agent

interface Skill {
    val name: String
    val description: String
    fun getSchema(): String
    suspend fun execute(arguments: Map<String, Any>): String
}
