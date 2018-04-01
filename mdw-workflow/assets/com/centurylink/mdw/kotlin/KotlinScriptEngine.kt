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
package com.centurylink.mdw.kotlin.script

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompiler
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.script.jsr223.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import javax.script.ScriptException
import javax.script.CompiledScript
import kotlin.reflect.KClass

/**
 * From org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
 */
class KotlinScriptEngine(
        factory: ScriptEngineFactory,
        val templateClasspath: List<File>,
        templateClassName: String,
        val getScriptArgs: (ScriptContext, Array<out KClass<out Any>>?) -> ScriptArgsWithTypes?,
        val scriptArgsTypes: Array<out KClass<out Any>>?
) : KotlinJsr223JvmScriptEngineBase(factory), KotlinJsr223JvmInvocableScriptEngine {

    override val replCompiler: ReplCompiler by lazy {
       GenericReplCompiler(
               makeScriptDefinition(templateClasspath, templateClassName),
               makeCompilerConfiguration(),
               PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
    }
    // TODO: bindings passing works only once on the first eval, subsequent setContext/setBindings call have no effect. Consider making it dynamic, but take history into account
    private val localEvaluator by lazy { GenericReplCompilingEvaluator(replCompiler, templateClasspath, Thread.currentThread().contextClassLoader, getScriptArgs(getContext(), scriptArgsTypes)) }

    override val replEvaluator: ReplFullEvaluator get() = localEvaluator

    override val state: IReplStageState<*> get() = getCurrentState(getContext())

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = replEvaluator.createState(lock)

    override fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? {
        return getScriptArgs(context, scriptArgsTypes)
    }
  
    /**
     * Injects binding declarations into script content (TODO: better way once this is finalized:
     * https://github.com/Kotlin/KEEP/blob/scripting/proposals/scripting-support.md)
     */
    fun compile(script: String, types: Map<String,String>?) : CompiledScript {
        var pre = ""
        if (types != null) {
            for ((key, value) in types) {
                val type = mapType(value)
                pre += "var ${key}: ${type}? = bindings[\"${key}\"] as ${type}?\n";
            }
        }
        return compile(pre + script, getContext());
    }
  
    /**
     * Evaluate separately from compile to allow precompilation.
     * Shows how to implement an InvokeWrapper that has access to the script class instance.
     */
    fun eval(compiledScript: CompiledKotlinScript) : Any? {
        val context = getContext()
        val state = getCurrentState(context)
        val result = try {
            replEvaluator.eval(state, compiledScript.compiledData, overrideScriptArgs(context), object : InvokeWrapper {
                override fun <T> invoke(body: () -> T): T {
                    val instance = body()
                    return instance
                }
            })
        }
        catch (e: Exception) {
            throw ScriptException(e)
        }

        return when (result) {
            is ReplEvalResult.ValueResult -> result.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> throw ScriptException(result.message)
            is ReplEvalResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
        }      
    }
  
    private fun mapType(type: String) : String {
        // TODO: map primitive types (but make sure values can be set through reflection)
        return type
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
