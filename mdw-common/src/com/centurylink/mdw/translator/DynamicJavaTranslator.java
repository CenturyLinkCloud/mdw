/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.translator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.centurylink.mdw.common.translator.impl.JavaObjectTranslator;

public class DynamicJavaTranslator extends JavaObjectTranslator {
    @Override
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {

        if (tryProviders)
            return super.realToObject(str, tryProviders);

        ObjectInputStream ois = null;
        try {
            byte[] decoded = decodeBase64(str);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            ois = new ObjectInputStream(bais);
            try {
                return ois.readObject();
            }
            catch (Exception cnfex) {
                if (!(cnfex instanceof ClassNotFoundException))
                    cnfex.printStackTrace();  // shouldn't happen
                // try dynamic java classloader
                bais.reset();
                ois.close();
                ois = new DynamicJavaInputStream(bais, getPackage());
                try {
                    return ois.readObject();
                }
                catch (ClassNotFoundException ex2) {
                    // throw the original exception -- more likely to be the root cause
                    String msg = "Can't find: " + cnfex.getMessage() + " AND unable to locate class in Dynamic Java";
                    throw new ClassNotFoundException(msg, cnfex);
                }
            }
        }
        catch (Throwable t) {  // including NoClassDefFoundError
            throw new TranslationException(t.getMessage(), t);
        }
        finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ex) {}
            }
        }
    }

}
