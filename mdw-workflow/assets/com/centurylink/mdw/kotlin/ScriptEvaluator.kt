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

import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.common.repl.renderReplStackTrace
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import javax.script.Bindings

open class ScriptEvaluator(
        val baseClasspath: Iterable<File>,
        val baseClassloader: ClassLoader? = Thread.currentThread().contextClassLoader,
        protected val fallbackScriptArgs: ScriptArgsWithTypes? = null
) {

    fun eval(compileResult: ScriptCompileResult.CompiledClasses,
             scriptArgs: ScriptArgsWithTypes?,
             invokeWrapper: InvokeWrapper?): ReplEvalResult {

        val (classLoader, scriptClass) = try {
            processClasses(compileResult)
        }
        catch (e: Exception) {
            e.printStackTrace() // TODO
            return@eval ReplEvalResult.Error.Runtime(e.message ?: "unknown", e)
        }

        val currentScriptArgs = scriptArgs ?: fallbackScriptArgs
        val useScriptArgs = currentScriptArgs?.scriptArgs
        val useScriptArgsTypes = currentScriptArgs?.scriptArgsTypes?.map { it.java }

        val constructorParams: Array<Class<*>> = emptyArray<Class<*>>() +
                (useScriptArgs?.mapIndexed { i, it -> useScriptArgsTypes?.getOrNull(i) ?: it?.javaClass ?: Any::class.java } ?: emptyList())

        val constructorArgs: Array<out Any?> = useScriptArgs.orEmpty()

        // TODO: try/catch ?
        val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)

        val scriptInstance =
                try {
                    if (invokeWrapper != null) invokeWrapper.invoke { scriptInstanceConstructor.newInstance(*constructorArgs) }
                    else scriptInstanceConstructor.newInstance(*constructorArgs)
                }
                catch (e: InvocationTargetException) {
                    // ignore everything in the stack trace until this constructor call
                    return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e.targetException as? Exception)
                }
                catch (e: Throwable) {
                    // ignore everything in the stack trace until this constructor call
                    return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                }


        val resultField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
        val resultValue: Any? = resultField.get(scriptInstance)

        if (currentScriptArgs != null) {
            val bindings = currentScriptArgs.scriptArgs[0] as Bindings
            val vars = currentScriptArgs.scriptArgs[1] as Map<String,Any?>
            for ((key, value) in vars) {
                bindings[key] = value
            }
        }

        return if (compileResult.hasResult) ReplEvalResult.ValueResult(resultValue, compileResult.type)
        else ReplEvalResult.UnitResult()
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }

    private fun processClasses(compileResult: ScriptCompileResult.CompiledClasses
    ): Pair<ClassLoader, Class<out Any>> {
        var mainLineClassName: String? = null
        val classLoader = makeScriptClassLoader(baseClassloader, compileResult.classpathAddendum)
        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.removeSuffix(".class"))
        fun compiledClassesNames() = compileResult.classes.map { classNameFromPath(it.path).internalName.replace('/', '.') }
        val expectedClassName = compileResult.mainClassName
        compileResult.classes.filter { it.path.endsWith(".class") }
                .forEach {
                    val className = classNameFromPath(it.path)
                    if (className.internalName == expectedClassName || className.internalName.endsWith("/$expectedClassName")) {
                         mainLineClassName = className.internalName.replace('/', '.')
                    }
                    classLoader.addClass(className, it.bytes)
                }

        val scriptClass = try {
            classLoader.loadClass(mainLineClassName!!)
        }
        catch (t: Throwable) {
            throw Exception("Error loading class $mainLineClassName: known classes: ${compiledClassesNames()}", t)
        }
        return Pair(classLoader, scriptClass)
    }

    fun makeScriptClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
            ScriptClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))

}