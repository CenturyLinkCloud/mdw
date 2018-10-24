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
package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.util.MiniCrypter;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Parameters(commandNames = "decrypt", commandDescription = "Decrypt a value using $MDW_APP_TOKEN secret key", separators = "=")
public class Decrypt implements Operation {

    @Parameter(names="--input", description="Value to be encrypted", required=true)
    private String input;
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public Decrypt run(ProgressMonitor... progressMonitors) throws IOException {
        System.out.println(decrypt());
        return this;
    }

    public String decrypt() throws IOException {
        String appToken = System.getenv(Encrypt.MDW_APP_TOKEN);
        if (appToken == null)
            throw new IOException("Missing environment variable: " + Encrypt.MDW_APP_TOKEN);

        try {
            return MiniCrypter.decrypt(input, appToken);
        }
        catch (GeneralSecurityException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }
}
