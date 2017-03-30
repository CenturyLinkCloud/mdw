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
package com.centurylink.mdw.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Utility for invoking HTTP requests.
 */
public class HttpHelper {

    private HttpConnection connection;
    public HttpConnection getConnection() { return connection; }

    public void setReadTimeout(int ms) {
        connection.setReadTimeout(ms);
    }
    public void setConnectTimeout(int ms) {
        connection.setConnectTimeout(ms);
    }

    public Map<String,String> getHeaders() {
        return connection.getHeaders();
    }
    public void setHeaders(Map<String,String> headers) {
        connection.setHeaders(headers);
    }

    private HttpResponse response;
    public int getResponseCode() {
        return response == null ? 0 : response.getCode();
    }

    public String getResponseMessage() {
        return response == null ? null : response.getMessage();
    }

    public byte[] getResponseBytes() {
        return response == null ? null : response.getContent();
    }
    public String getResponse() {
        if (response == null || response.getContent() == null)
            return null;
        return new String(response.getContent());
    }

    public HttpHelper(URL url) {
        this.connection = new HttpConnection(url);
    }

    public HttpHelper(URL url, String user, String password) {
        this.connection = new HttpConnection(url, user, password);
    }

    public HttpHelper(HttpURLConnection connection) {
        this.connection = new HttpConnection(connection);
    }

    public HttpHelper(HttpURLConnection connection, String user, String password) {
        this.connection = new HttpConnection(connection, user, password);
    }

    public HttpHelper(HttpConnection connection) {
        this.connection = connection;
    }

    /**
     * Perform an HTTP POST request to the URL.
     * @param content string containing the content to be posted
     * @return string containing the response from the server
     */
    public String post(String content) throws IOException {
        postBytes(content.getBytes());
        return getResponse();
    }

    /**
     * Perform an HTTP POST request to the URL.
     * @param content string containing the content to be posted
     * @return string containing the response from the server
     */
    public byte[] postBytes(byte[] content) throws IOException {

        if (!connection.isOpen())
            connection.open();

        connection.prepare("POST");

        OutputStream os = connection.getOutputStream();
        os.write(content);

        response = connection.readInput();
        os.close();
        return getResponseBytes();
    }

    public String get() throws IOException {
        return new String(getBytes());
    }

    /**
     * Perform an HTTP GET request against the URL.
     * @return the string response from the server
     */
    public byte[] getBytes() throws IOException {
        if (!connection.isOpen())
            connection.open();

        connection.prepare("GET");
        response = connection.readInput();
        return getResponseBytes();
    }

    /**
     * Perform an HTTP PUT request to the URL.
     * @param content bytes
     * @return string containing the response from the server
     */
    public byte[] putBytes(byte[] content) throws IOException {

        if (!connection.isOpen())
            connection.open();

        connection.prepare("PUT");

        OutputStream os = connection.getOutputStream();
        os.write(content);

        response = connection.readInput();
        os.close();
        return getResponseBytes();
    }

    /**
     * Perform an HTTP PATCH request to the URL.
     * @param content bytes
     * @return string containing the response from the server
     */
    public byte[] patchBytes(byte[] content) throws IOException {

        if (!connection.isOpen())
            connection.open();

        connection.prepare("PATCH");

        OutputStream os = connection.getOutputStream();
        os.write(content);

        response = connection.readInput();
        os.close();
        return getResponseBytes();
    }

   /**
     * Submit an HTTP PUT request with a String value
     * @param content string containing the content to be put
     * @return string containing the response from the server
     */
    public String put(String content) throws IOException {
        putBytes(content.getBytes());
        return getResponse();
     }

    /**
     * Submit an HTTP PATCH request with a String value
     * @param content string containing the content to be patched
     * @return string containing the response from the server
     */
     public String patch(String content) throws IOException {
         patchBytes(content.getBytes());
         return getResponse();
     }

    /**
     * Upload a text file to the destination URL
     * @param file the file to be uploaded
     * @param force whether to overwrite if the destination file is newer
     * @return string with the response from the server
     */
    public String put(File file) throws IOException {

        if (!connection.isOpen())
            connection.open();

        connection.prepare("PUT");

        String contentType = connection.getHeader("Content-Type");
        if (contentType == null)
            contentType = connection.getHeader("content-type");
        if (contentType == null || contentType.isEmpty()) {
            /**
             * Default it to application/octet-stream if nothing has been specified
             */
            connection.setHeader("Content-Type", "application/octet-stream");
        }

        OutputStream outStream = connection.getOutputStream();
        InputStream inStream = new FileInputStream(file);

        byte[] buf = new byte[1024];
        int len = 0;
        while (len != -1)
        {
          len = inStream.read(buf);
          if (len > 0)
            outStream.write(buf, 0, len);
        }

        inStream.close();
        outStream.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getConnection().getInputStream()));
        StringBuffer responseBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            responseBuffer.append(line).append('\n');
        }

        reader.close();

        connection.getConnection().disconnect();

        response = new HttpResponse(responseBuffer.toString().getBytes());
        response.setCode(connection.getConnection().getResponseCode());
        if (response.getCode() < 200 || response.getCode() >= 300)
        {
            response.setMessage(connection.getConnection().getResponseMessage());
            throw new IOException("Error uploading file: " + response.getCode() + " -- " + response.getMessage());
        }

        return getResponse();
    }

    /**
     * Perform an HTTP DELETE request against the URL.
     * @return the string response from the server
     */
    public String delete() throws IOException {
        return delete(null);
    }

    public String delete(String content) throws IOException {
        deleteBytes(content == null ? null : content.getBytes());
        return getResponse();
    }

    /**
     * Perform an HTTP DELETE request to the URL.
     * @param content bytes (not usually populated)
     * @return string containing the response from the server
     */
    public byte[] deleteBytes(byte[] content) throws IOException {

        if (!connection.isOpen())
            connection.open();

        connection.prepare("DELETE");

        OutputStream os = null;
        if (content != null) {
            connection.getConnection().setDoOutput(true);
            os = connection.getOutputStream();
            os.write(content);
        }

        response = connection.readInput();
        if (os != null)
           os.close();
        return getResponseBytes();
    }

    public static HttpHelper getHttpHelper(String method, URL url) {
        if ("PATCH".equalsIgnoreCase(method))
            return new HttpHelper(new HttpAltConnection(url));
        else
            return new HttpHelper(url);
    }
}
