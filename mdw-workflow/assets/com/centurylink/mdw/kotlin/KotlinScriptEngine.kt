/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.kotlin

import com.centurylink.mdw.cache.impl.PackageCache
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.Reader
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import javax.script.*
import kotlin.reflect.KClass
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.util.log.LoggerUtil

/**
 * TODO: Figure out whether this changes with:
 * https://github.com/Kotlin/KEEP/blob/scripting/proposals/scripting-support.md
 */
open class KotlinScriptEngine(
        val kotlinFactory: ScriptEngineFactory,
        val templateClasspath: List<File>,
        templateClassName: String,
        val getScriptArgs: (ScriptContext, Array<out KClass<out Any>>?) -> ScriptArgsWithTypes?,
        val scriptArgsTypes: Array<out KClass<out Any>>?
) : AbstractScriptEngine(), ScriptEngine {

    val logger = LoggerUtil.getStandardLogger()
    
    override fun getFactory(): ScriptEngineFactory = kotlinFactory

    val scriptCompiler: ScriptCompiler by lazy {
        ScriptCompiler(
                makeScriptDefinition(templateClasspath, templateClassName),
                makeCompilerConfiguration(),
                PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
    }

    val scriptEvaluator: ScriptEvaluator by lazy {
        ScriptEvaluator(
                templateClasspath,
                PackageCache.getPackage(KOTLIN_PACKAGE).getCloudClassLoader(),
                getScriptArgs(getContext(), scriptArgsTypes))
    }

    override fun createBindings(): Bindings = SimpleBindings().apply { put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this) }

    override fun eval(reader: Reader, context: ScriptContext): Any? {
        return eval(reader.readText(), context)
    }

    override fun eval(script: String, context: ScriptContext): Any? {
        return eval((compile(java.lang.Integer.toHexString(script.hashCode()), script) as KotlinCompiledScript), context)
    }

    fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? {
        var scriptArgs = getScriptArgs(context, scriptArgsTypes)
        if (scriptArgs != null) {
            var args = scriptArgs.scriptArgs.toMutableList()
            if (args.size > 0 && args[0] != null && args[0] is Bindings) {
                var argTypes = scriptArgs.scriptArgsTypes.toMutableList()
                val bindings: Bindings = args[0] as Bindings;
                var variables = mutableMapOf<String,Any?>()
                args.add(variables)
                argTypes.add(MutableMap::class)
                for (key in bindings.keys) {
                    if ("runtimeContext".equals(key)) {
                      args.add(bindings[key])
                      argTypes.add(ActivityRuntimeContext::class)
                    }
                    else {
                        variables[key] = bindings[key]
                    }
                }
                val argsArray = Array<Any?>(args.size) { args[it] }
                val argTypesArray = Array<KClass<out Any>>(argTypes.size) { argTypes[it] }
                return ScriptArgsWithTypes(argsArray, argTypesArray)
            }
        }
        return scriptArgs
    }

    @Throws(ScriptException::class)
    fun compile(name: String, script: String) : CompiledScript {
		val result = if (logger.isDebugEnabled()) {
            val start = System.currentTimeMillis()
            var res = scriptCompiler.compile(name, script)
            logger.debug("Kotlin script compile time: ${System.currentTimeMillis() - start} ms")
			res
		} else {
		    scriptCompiler.compile(name, script)
		}
        val compiled = when (result) {
            is ScriptCompileResult.Error -> throw ScriptException("Error${locationString(result)}: ${result.message}")
            is ScriptCompileResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ScriptCompileResult.CompiledClasses -> result
        }
        return KotlinCompiledScript(this, compiled)
    }

    fun eval(compiledScript: KotlinCompiledScript) : Any? {
        return eval(compiledScript, getContext())
    }

    fun eval(compiledScript: KotlinCompiledScript, context: ScriptContext) : Any? {
        var errorLineNum: Int = 0
        val result = try {
            scriptEvaluator.eval(compiledScript.compiledData, overrideScriptArgs(context), object : InvokeWrapper {
                override fun <T> invoke(body: () -> T): T {
                    try {
                        val instance = body()
                        return instance
                    }
                    catch (e: InvocationTargetException) {
						if (logger.isMdwDebugEnabled) {
							logger.severeException(e.message, e)
						}
                        if (e.cause is Throwable) {
                            val th = e.cause as Throwable
                            if (th.stackTrace != null && th.stackTrace.size > 0) {
                                errorLineNum =  th.stackTrace[0].lineNumber
                            }
                        }
                        throw e
                    }
                }
            })
        }
        catch (e: ScriptException) {
            throw e
        }
        catch (e: Exception) {
            throw ScriptException(e)
        }

        return when (result) {
            is ReplEvalResult.ValueResult -> result.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> {
                throw KotlinScriptException(result.message, null, errorLineNum)
            }
            is ReplEvalResult.Incomplete -> {
                throw KotlinScriptException("error: incomplete code", null, errorLineNum)
            }
            is ReplEvalResult.HistoryMismatch -> {
                throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
            }
        }
    }

    fun locationString(result: ScriptCompileResult.Error): String {
        if (result.location == null)
            return ""
        else
            return " at ${result.location.line}:${result.location.column}"
    }


    private fun makeScriptDefinition(templateClasspath: List<File>, templateClassName: String): KotlinScriptDefinition {
        val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), this.javaClass.classLoader)
        val cls = classloader.loadClass(templateClassName)
        return KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, emptyMap())
    }

    private fun makeCompilerConfiguration() = CompilerConfiguration().apply {
        addJvmSdkRoots(PathUtil.getJdkClassesRootsFromCurrentJre())
        addJvmClasspathRoots(templateClasspath)
        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
        languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, mapOf(AnalysisFlag.skipMetadataVersionCheck to true)
        )
    }
}

public class KotlinCompiledScript(val engine: KotlinScriptEngine, val compiledData: ScriptCompileResult.CompiledClasses) : CompiledScript() {
    override fun eval(context: ScriptContext): Any? = engine.eval(this, context)
    override fun getEngine(): ScriptEngine = engine
}
