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

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.CompiledClassData
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.repl.ReplCodeAnalyzer
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import java.io.File

// WARNING: not thread safe, assuming external synchronization

class ScriptCompiler(disposable: Disposable,
        protected val scriptDefinition: KotlinScriptDefinition,
        protected val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) {

    constructor(scriptDefinition: KotlinScriptDefinition, compilerConfiguration: CompilerConfiguration, messageCollector: MessageCollector) :
            this(Disposer.newDisposable(), scriptDefinition, compilerConfiguration, messageCollector)

    private val checker = ScriptChecker(disposable, scriptDefinition, compilerConfiguration, messageCollector)

    private val checkerEnvironment = run {
        compilerConfiguration.apply {
            add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
            if (get(JVMConfigurationKeys.JVM_TARGET) == null) {
                put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
            }
        }
        KotlinCoreEnvironment.createForProduction(disposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    fun check(name: String, script: String): ScriptCheckOutcome = checker.check(name, script)

    fun compile(name: String, script: String): ScriptCompileResult {
        val (psiFile, errorHolder) = run {
            val outcome = checker.check(name, script)
            when (outcome.result) {
                is ScriptCheckResult.Incomplete -> return@compile ScriptCompileResult.Incomplete()
                is ScriptCheckResult.Error -> return@compile ScriptCompileResult.Error(outcome.result.message, outcome.result.location)
                is ScriptCheckResult.Ok -> {} // continue
            }
            Pair(outcome.psiFile, outcome.errorHolder)
        }

        val newDependencies = ScriptDependenciesProvider.getInstance(checkerEnvironment.project).getScriptDependencies(psiFile)
        var classpathAddendum = newDependencies?.let { checkerEnvironment.updateClasspath(it.classpath.map(::JvmClasspathRoot)) }

        // Drag in the REPL analyzerEngine rather than reproducing.  Dummy-up a codeLine
        val analyzerEngine = ReplCodeAnalyzer(checker.environment)
        val dummyCodeLine = ReplCodeLine(1, 1, script)
        val analysisResult = analyzerEngine.analyzeReplLine(psiFile, dummyCodeLine)
        AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)
        val scriptDescriptor = when (analysisResult) {
            is ReplCodeAnalyzer.ReplLineAnalysisResult.WithErrors -> return ScriptCompileResult.Error(errorHolder.renderMessage())
            is ReplCodeAnalyzer.ReplLineAnalysisResult.Successful -> analysisResult.scriptDescriptor
            else -> error("Unexpected result ${analysisResult::class.java}")
        }

        val generationState = GenerationState.Builder(
                psiFile.project,
                ClassBuilderFactories.binaries(false),
                analyzerEngine.module,
                analyzerEngine.trace.bindingContext,
                listOf(psiFile),
                compilerConfiguration
        ).build()
        generationState.replSpecific.scriptResultFieldName = SCRIPT_RESULT_FIELD_NAME
        // generationState.replSpecific.earlierScriptsForReplInterpreter = compilerState.history.map { it.item }
        generationState.beforeCompile()
        KotlinCodegenFacade.generatePackage(
                generationState,
                psiFile.script!!.containingKtFile.packageFqName,
                setOf(psiFile.script!!.containingKtFile),
                org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

        val expression = psiFile.getMyChildOfType<KtScript>()?.
                getMyChildOfType<KtBlockExpression>()?.
                getMyChildOfType<KtScriptInitializer>()?.
                getMyChildOfType<KtExpression>()

        val type = expression?.let {
            analyzerEngine.trace.bindingContext.getType(it)
        }?.let {
            DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it)
        }

        return ScriptCompileResult.CompiledClasses(name,
                generationState.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) },
                generationState.replSpecific.hasResult,
                classpathAddendum ?: emptyList(),
                type)
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }

    // replaces function in psiUtils.kt due to compiler error
    inline fun <reified T : PsiElement> PsiElement.getMyChildOfType(): T? {
        return PsiTreeUtil.getChildOfType(this, T::class.java)
    }
}

sealed class ScriptCompileResult {
    class CompiledClasses(val mainClassName: String,
                          val classes: List<CompiledClassData>,
                          val hasResult: Boolean,
                          val classpathAddendum: List<File>,
                          val type: String?) : ScriptCompileResult() {
    }

    class Incomplete : ScriptCompileResult() {
    }

    class Error(val message: String, val location: CompilerMessageLocation? = null) : ScriptCompileResult() {
        override fun toString(): String = "Error(message = \"$message\""
    }
}
