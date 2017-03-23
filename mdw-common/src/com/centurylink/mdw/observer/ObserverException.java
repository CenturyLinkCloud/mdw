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
package com.centurylink.mdw.observer;

import com.centurylink.mdw.common.MdwException;

/**
 * ObserverException
 *
  */
public class ObserverException extends MdwException {

    private static final long serialVersionUID = 1L;

    public ObserverException(String pMessage){
        super(pMessage);
    }

    public ObserverException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

    public ObserverException(String pMessage, Throwable pTh){
        super(pMessage, pTh);
    }

    public ObserverException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }



}
