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
package com.centurylink.mdw.workflow.adapter.file;

import java.io.FileWriter;
import java.io.IOException;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.ObjectAdapterActivity;

@Tracked(LogLevel.TRACE)
public class FileWriterAdapter extends ObjectAdapterActivity {

    public static final String OUTPUT_FILEPATH = "OutputFilepath";
    public static final String APPEND_TO_FILE = "AppendToFile";
    public static final String WRITE_STRING = "WriteString";

    public boolean isSynchronous() {
        return true;
    }

    protected Object openConnection() throws ConnectionException, AdapterException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(getFilePath(), isAppend());
        }
        catch (IOException ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
        catch (PropertyException ex) {
            throw new AdapterException(ConnectionException.CONFIGURATION_WRONG, ex.getMessage(), ex);
        }
        return writer;
    }

    protected void closeConnection(Object connection) {
        FileWriter writer = (FileWriter) connection;
        try {
            writer.flush();
            writer.close();
        }
        catch (IOException ex) {
            logexception("Failed to close connection in FileWriterAdapter", ex);
        }
    }

    protected Object getRequestData() throws ActivityException {
        try {
            Object value = getAttributeValueSmart(WRITE_STRING);
            if (value instanceof String && ((String)value).startsWith("DOCUMENT:")) {
                return getDocumentContent(new DocumentReference(value.toString()));
            }
            else {
              return value;
            }
        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    protected Object invoke(Object connection, Object requestData) throws AdapterException, ConnectionException {
        if (requestData != null) {
            String writeString = requestData.toString();
            FileWriter writer = (FileWriter) connection;
            try {
                writer.write(writeString);
                writer.flush();
                return "SUCCESS";
            }
            catch (IOException ex) {
                throw new AdapterException(-1, ex.getMessage(), ex);
            }
        }
        return "NULL";
    }

    protected String getFilePath() throws PropertyException {
        return getAttributeValueSmart(OUTPUT_FILEPATH);
    }

    protected boolean isAppend() throws PropertyException {
        return Boolean.parseBoolean(getAttributeValueSmart(APPEND_TO_FILE));
    }

}
