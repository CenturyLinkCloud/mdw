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

import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor

import java.io.PrintWriter

class ScriptClassLoader(parent: ClassLoader) : ClassLoader(parent) {

    private val classes = mutableMapOf<JvmClassName, ByteArray>()

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        val classBytes = classes[JvmClassName.byFqNameWithoutInnerClasses(name)]
        return if (classBytes != null) {
            defineClass(name, classBytes, 0, classBytes.size)
        } else {
            super.findClass(name)
        }
    }

    fun addClass(className: JvmClassName, bytes: ByteArray) {
        val oldBytes = classes.put(className, bytes)
        if (oldBytes != null) {
            throw IllegalStateException("Rewrite at key $className")
        }
    }

    fun dumpClasses(writer: PrintWriter) {
        for (classBytes in classes.values) {
            ClassReader(classBytes).accept(TraceClassVisitor(writer), 0)
        }
    }
}
