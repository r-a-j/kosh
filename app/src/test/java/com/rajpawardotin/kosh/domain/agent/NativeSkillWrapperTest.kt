package com.rajpawardotin.kosh.domain.agent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSkillWrapperTest {

    class DummySkill {
        @Tool(name = "hello_suspend", description = "Test suspend tool")
        suspend fun helloSuspend(
            @ToolParam(name = "name", description = "Name to greet") name: String
        ): String {
            kotlinx.coroutines.delay(10)
            return "Hello Suspend, $name!"
        }

        @Tool(name = "hello_normal", description = "Test normal tool")
        fun helloNormal(
            @ToolParam(name = "name", description = "Name to greet") name: String
        ): String {
            return "Hello Normal, $name!"
        }
    }

    @Test
    fun testNormalToolExecution() = runBlocking {
        val skillInstance = DummySkill()
        val method = skillInstance::class.java.declaredMethods.first { it.name == "helloNormal" }
        val wrapper = NativeSkillWrapper(skillInstance, method)

        val result = wrapper.execute(mapOf("name" to "World"))
        assertEquals("Hello Normal, World!", result)
    }

    @Test
    fun testSuspendToolExecution() = runBlocking {
        val skillInstance = DummySkill()
        val method = skillInstance::class.java.declaredMethods.first { it.name == "helloSuspend" }
        val wrapper = NativeSkillWrapper(skillInstance, method)

        val result = wrapper.execute(mapOf("name" to "World"))
        assertEquals("Hello Suspend, World!", result)
    }
}
