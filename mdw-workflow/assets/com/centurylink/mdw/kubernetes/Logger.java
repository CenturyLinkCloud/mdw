package com.centurylink.mdw.kubernetes;

import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Ability to override default 1G size limit
 */
class Logger {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Pod pod;

    private FileOutputStream out;
    private InputStream in;

    private boolean isTerminating;

    Logger(Pod pod) {
        this.pod = pod;
    }

    // TODO: configurable
    private int maxBytes = 1048576; // 1G

    void startup() {

        logger.info("Logger startup: " + this);

        new Thread(() -> {
            try {
                String path = "pods/" + pod.getName() + "/log?follow=true";
                URL url = new URL(Context.getBaseUrl() + "/" + path);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                HttpHelper helper = new HttpHelper(urlConnection);
                Map<String, String> requestHeaders = new HashMap<>();
                requestHeaders.put("Authorization", "Bearer " + Context.getServiceToken());
                helper.setHeaders(requestHeaders);

                HttpConnection connection = helper.getConnection();
                connection.prepare("GET");
                byte[] buffer = new byte[2048];
                in = urlConnection.getInputStream();
                File logFile = getFile();
                out = new FileOutputStream(logFile, true);
                while (!isTerminating) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        logger.warn("Logger: " + this + " terminating due to EOF");
                        terminate();
                    }
                    else {
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                        if (logFile.length() > maxBytes) {
                            out.close();
                            Files.copy(logFile.toPath(), new File(logFile + ".old").toPath(), StandardCopyOption.REPLACE_EXISTING);
                            Files.write(logFile.toPath(), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                            out = new FileOutputStream(logFile, true);
                        }
                    }
                }
                shutdown();
            }
            catch (IOException ex) {
                logger.severeException(ex.getMessage(), ex);
                shutdown();
            }
        }).start();
    }

    void terminate() {
        this.isTerminating = true;
    }

    void shutdown() {

        logger.info("Logger shutdown: " + this);

        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
        catch (IOException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    File getFile() {
        return new File(Context.getLogsDir() + "/pods/" + pod.getName() + ".log");
    }

    public String toString() {
        return getFile().toString();
    }
}
