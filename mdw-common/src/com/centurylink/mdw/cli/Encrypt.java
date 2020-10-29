package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.util.MiniCrypter;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Parameters(commandNames = "encrypt", commandDescription = "Encrypt a value using $MDW_APP_TOKEN secret key", separators = "=")
public class Encrypt implements Operation {

    public static final String MDW_APP_TOKEN = "MDW_APP_TOKEN";

    @Parameter(names="--input", description="Value to be encrypted", required=true)
    private String input;
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public Encrypt run(ProgressMonitor... progressMonitors) throws IOException {
        getOut().println(encrypt());
        return this;
    }

    public String encrypt() throws IOException {
        String appToken = System.getenv(MDW_APP_TOKEN);
        if (appToken == null)
            throw new IOException("Missing environment variable: " + MDW_APP_TOKEN);

        try {
            return MiniCrypter.encrypt(input, appToken);
        }
        catch (GeneralSecurityException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

    }
}
