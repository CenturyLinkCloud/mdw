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

import java.io.File
import com.centurylink.mdw.app.ApplicationContext
import com.centurylink.mdw.cache.impl.PackageCache
import com.centurylink.mdw.cloud.CloudClasspath

const val CLASSES_PATH = "kotlin/classes"
const val KOTLIN_PACKAGE = "com.centurylink.mdw.kotlin"

class KotlinClasspath {
    
    val asFiles: List<File> by lazy {
        var files = mutableListOf<File>()
        files.add(File(ApplicationContext.getTempDirectory() + "/" + CLASSES_PATH));
        val kotlinPackage = PackageCache.getPackage(KOTLIN_PACKAGE)
        val cloudClasspath = CloudClasspath(kotlinPackage.getCloudClassLoader())
        cloudClasspath.read()
        files.addAll(cloudClasspath.getFiles())
        files
    }
    
    val asString: String by lazy {
        asFiles.joinToString(File.pathSeparator)
    }
}