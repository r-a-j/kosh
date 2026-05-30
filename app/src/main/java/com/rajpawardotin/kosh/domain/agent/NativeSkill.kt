package com.rajpawardotin.kosh.domain.agent

import java.lang.reflect.Method
import java.lang.reflect.Parameter
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tool(val name: String, val description: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ToolParam(val name: String, val description: String, val required: Boolean = true)

class NativeSkillWrapper(
    private val instance: Any,
    private val method: Method
) : Skill {
    private val toolAnnotation = method.getAnnotation(Tool::class.java)
        ?: throw IllegalArgumentException("Method must be annotated with @Tool")

    override val name: String = toolAnnotation.name
    override val description: String = toolAnnotation.description

    override fun getSchema(): String {
        val properties = StringBuilder()
        val requiredList = mutableListOf<String>()

        method.parameters.forEach { param ->
            val paramAnn = param.getAnnotation(ToolParam::class.java) ?: return@forEach
            val paramName = paramAnn.name
            val paramType = when (param.type) {
                Boolean::class.java, java.lang.Boolean::class.java -> "boolean"
                Int::class.java, java.lang.Integer::class.java -> "integer"
                Double::class.java, java.lang.Double::class.java -> "number"
                Float::class.java, java.lang.Float::class.java -> "number"
                else -> "string"
            }
            if (paramAnn.required) {
                requiredList.add(paramName)
            }
            properties.append("""
                "$paramName": {
                    "type": "$paramType",
                    "description": "${paramAnn.description}"
                },
            """.trimIndent())
        }

        val propsString = properties.toString().trim().removeSuffix(",")

        val requiredJson = if (requiredList.isNotEmpty()) {
            requiredList.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        } else {
            "[]"
        }

        return """
        {
          "name": "$name",
          "description": "$description",
          "parameters": {
            "type": "object",
            "properties": {
              $propsString
            },
            "required": $requiredJson
          }
        }
        """.trimIndent()
    }

    override suspend fun execute(arguments: Map<String, Any>): String {
        try {
            val paramTypes = method.parameterTypes
            val params = method.parameters
            val argValues = Array<Any?>(params.size) { null }

            for (i in params.indices) {
                val param = params[i]
                val paramAnn = param.getAnnotation(ToolParam::class.java)
                if (paramAnn == null) {
                    argValues[i] = null
                    continue
                }
                val paramName = paramAnn.name
                val rawVal = arguments[paramName]

                if (rawVal == null) {
                    if (paramAnn.required) {
                        throw IllegalArgumentException("Parameter '$paramName' is required")
                    }
                    argValues[i] = null
                    continue
                }

                // Safe casting/coercion
                val coercedVal = when (paramTypes[i]) {
                    Boolean::class.java, java.lang.Boolean::class.java -> when (rawVal) {
                        is Boolean -> rawVal
                        is String -> rawVal.toBoolean()
                        is Number -> rawVal.toInt() != 0
                        else -> rawVal.toString().toBoolean()
                    }
                    Int::class.java, java.lang.Integer::class.java -> when (rawVal) {
                        is Number -> rawVal.toInt()
                        is String -> rawVal.toDouble().toInt()
                        else -> throw IllegalArgumentException("Expected integer for $paramName")
                    }
                    Double::class.java, java.lang.Double::class.java -> when (rawVal) {
                        is Number -> rawVal.toDouble()
                        is String -> rawVal.toDouble()
                        else -> throw IllegalArgumentException("Expected number for $paramName")
                    }
                    Float::class.java, java.lang.Float::class.java -> when (rawVal) {
                        is Number -> rawVal.toFloat()
                        is String -> rawVal.toFloat()
                        else -> throw IllegalArgumentException("Expected number for $paramName")
                    }
                    else -> rawVal.toString()
                }
                argValues[i] = coercedVal
            }

            val isSuspend = paramTypes.isNotEmpty() && kotlin.coroutines.Continuation::class.java.isAssignableFrom(paramTypes.last())

            val result = if (isSuspend) {
                suspendCoroutineUninterceptedOrReturn { cont ->
                    argValues[argValues.size - 1] = cont
                    try {
                        val res = method.invoke(instance, *argValues)
                        if (res == kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                            kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
                        } else {
                            res
                        }
                    } catch (e: Exception) {
                        throw e.cause ?: e
                    }
                }
            } else {
                method.invoke(instance, *argValues)
            }

            return result?.toString() ?: "Success"
        } catch (e: Exception) {
            val cause = e.cause ?: e
            return "Error executing $name: ${cause.message}"
        }
    }
}
