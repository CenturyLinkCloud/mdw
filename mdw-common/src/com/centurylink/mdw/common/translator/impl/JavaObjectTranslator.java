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
package com.centurylink.mdw.common.translator.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;

import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class JavaObjectTranslator extends DocumentReferenceTranslator {

    public Object realToObject(String str) throws TranslationException {
        ObjectInputStream ois = null;
        try {
            byte[] decoded = decodeBase64(str);
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            ois = new ObjectInputStream(bais);
            try {
                return ois.readObject();
            }
            catch (ClassNotFoundException ex) {
                ois.close();
                bais = new ByteArrayInputStream(decoded);
                ois = new ObjectInputStream(bais) {
                    @Override
                    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                        try {
                            return CompiledJavaCache.getResourceClass(desc.getName(), getClass().getClassLoader(), getPackage());
                        }
                        catch (ClassNotFoundException ex){
                            if (getPackage()  != null && getPackage().getCloudClassLoader() != null)
                                return getPackage().getCloudClassLoader().loadClass(desc.getName());
                            else
                                throw ex;
                        }
                        catch (MdwJavaException ex) {
                            throw new ClassNotFoundException(desc.getName(), ex);
                        }
                    }
                };
                return ois.readObject();
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

    public String realToString(Object object) throws TranslationException {
        if (!(object instanceof Serializable))
            throw new TranslationException("Object must implement java.io.Serializable: " + object.getClass());

        ObjectOutputStream oos = null;
        try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          oos = new ObjectOutputStream(baos);
          oos.writeObject(object);
          return encodeBase64(baos.toByteArray());
        }
        catch (IOException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
        finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ex) {}
            }
        }
    }

    protected byte[] decodeBase64(String inputString) {
        return Base64.decodeBase64(inputString.getBytes());
    }

    protected String encodeBase64(byte[] inputBytes) {
        return new String(Base64.encodeBase64(inputBytes));
    }

}