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

import org.jetbrains.kotlin.script.jsr223.KotlinStandardJsr223ScriptTemplate
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.util.*
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

/**
 * From org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
 */
class KotlinScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    public override fun getScriptEngine(): ScriptEngine {
        setIdeaIoUseFallback()
        return KotlinScriptEngine(
            this,
            KotlinClasspath().asFiles,
            KotlinScriptTemplate::class.qualifiedName!!,
            { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
            arrayOf(Bindings::class)
        )
    }

}