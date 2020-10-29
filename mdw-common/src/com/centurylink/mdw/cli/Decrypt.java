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
        getOut().println(decrypt());
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
