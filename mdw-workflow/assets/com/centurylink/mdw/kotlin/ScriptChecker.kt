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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.ByteBuffer

class ScriptChecker(
        disposable: Disposable,
        val scriptDefinition: KotlinScriptDefinition,
        val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector) {

    internal val environment = run {
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

    private val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl

    internal fun createDiagnosticHolder() = ScriptDiagnosticMessageHolder()

    fun check(name: String, script: String): ScriptCheckOutcome {
        val virtualFile =
                LightVirtualFile("$name${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, StringUtil.convertLineSeparators(script)).apply {
                    charset = CharsetToolkit.UTF8_CHARSET
                }
        val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                ?: error("Script file ${name} not analyzed: ${script}")

        val errorHolder = createDiagnosticHolder()

        val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

        val result = when {
            syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> ScriptCheckResult.Incomplete()
            syntaxErrorReport.isHasErrors -> ScriptCheckResult.Error(errorHolder.renderMessage())
            else -> ScriptCheckResult.Ok()
        }

        return ScriptCheckOutcome(psiFile, errorHolder, result)
    }
}

class ScriptDiagnosticMessageHolder : MessageCollectorBasedReporter, DiagnosticMessageHolder {
    private val outputStream = ByteArrayOutputStream()

    override val messageCollector: GroupingMessageCollector = GroupingMessageCollector(
            PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.WITHOUT_PATHS, false),
            false
    )

    override fun renderMessage(): String {
        messageCollector.flush()
        val bytes = outputStream.toByteArray()
        return Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString()
    }
}

class ScriptCheckOutcome(val psiFile: KtFile, val errorHolder: DiagnosticMessageHolder, val result: ScriptCheckResult ) {

}

sealed class ScriptCheckResult {
    class Ok : ScriptCheckResult() {
    }
    class Incomplete : ScriptCheckResult() {
    }
    class Error(val message: String, val location: CompilerMessageLocation? = null) : ScriptCheckResult() {
        override fun toString(): String = "Error(message = \"$message\")"
    }
}
