/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility for invoking HTTP requests.
 */
public class HttpHelper {

    public static final String HTTP_BASIC_AUTH_HEADER = "Authorization";

    private URL url;
    private HttpURLConnection connection;

    private int responseCode;
    public int getResponseCode() { return responseCode; }

    private String responseMessage;
    public String getResponseMessage() { return responseMessage; }

    private int connectTimeout = -1;
    public int getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(int timeout) { this.connectTimeout = timeout; }

    private int readTimeout = -1;
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int timeout) { this.readTimeout = timeout; }

    private Map<String,String> headers;
    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }
    public void setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(name, value);
    }

    private long maxBytes = -1;
    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long max) { this.maxBytes = max; }

    private Proxy proxy;
    public Proxy getProxy() { return proxy; }
    public void setProxy(Proxy proxy) { this.proxy = proxy; }


    /**
     * Only for GET requests.
     */
    private boolean followHtmlHeadMetaRefresh;
    public boolean isFollowHtmlHeadMetaRefresh() { return followHtmlHeadMetaRefresh; }
    public void setFollowHtmlHeadMetaRefresh(boolean follow) { this.followHtmlHeadMetaRefresh = follow; }
    private URL redirect; // for following meta refresh
    public URL getRedirect() { return redirect; }

    private byte[] response;
    public byte[] getResponseBytes() { return response; }
    public String getResponse() {
        if (response == null)
            return null;
        return new String(response);
    }

    public static void main(String[] args) {
        try {
            HttpHelper helper = new HttpHelper(new URL(args[0]));
            System.out.println(helper.get());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private String user;
    private String password;

    public HttpHelper(URL url) {
        this.url = url;
        if (url.getUserInfo() != null) {
            int colon = url.getUserInfo().indexOf(':');
            if (colon > 0) {
                this.user = url.getUserInfo().substring(0, colon);
                this.password = url.getUserInfo().substring(colon + 1);
            }
        }
    }

    public HttpHelper(URL url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public HttpHelper(HttpURLConnection connection) {
        this.connection = connection;
    }

    public HttpHelper(HttpURLConnection connection, String user, String password) {
        this.connection = connection;
        this.user = user;
        this.password = password;
    }

    public HttpHelper(HttpHelper cloneFrom, URL url) {
        this.url = url;
        this.user = cloneFrom.user;
        this.password = cloneFrom.password;
        this.connectTimeout = cloneFrom.connectTimeout;
        this.readTimeout = cloneFrom.readTimeout;
        this.headers = cloneFrom.headers;
        this.maxBytes = cloneFrom.maxBytes;
        this.followHtmlHeadMetaRefresh = false;  // avoid loop
    }

     /**
     * Perform an HTTP POST request to the URL.
     * @param content string containing the content to be posted
     * @return string containing the response from the server
     */
    public String post(String content, int connectTimeout, int readTimeout) throws IOException {
        this.connectTimeout = connectTimeout<0?connectTimeout:(connectTimeout*1000);
        this.readTimeout = readTimeout<0?readTimeout:(readTimeout*1000);
        return post(content);
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

        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        OutputStream os = connection.getOutputStream();
        os.write(content);

        readInput(connection);
        os.close();
        return response;
    }

    public String get() throws IOException {
        return new String(getBytes());
    }

    /**
     * Perform an HTTP GET request against the URL.
     * @return the string response from the server
     */
    public byte[] getBytes() throws IOException {
        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);

        connection.setDoOutput(false);
        connection.setRequestMethod("GET");

        HttpURLConnection.setFollowRedirects(true);

        readInput(connection);
        return response;
    }

    /**
     * Populates the response member.  Closes the connection.
     */
    private void readInput(HttpURLConnection connection) throws IOException {
        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[2048];
            try {
                is = connection.getInputStream();
                while (maxBytes == -1 || baos.size() < maxBytes) {
                    int bytesRead = is.read(buffer);
                    if (bytesRead == -1)
                        break;
                    baos.write(buffer, 0, bytesRead);
                }
                response = baos.toByteArray();
                if (followHtmlHeadMetaRefresh && getResponse().indexOf("http-equiv") > 0) {
                    try {
                      redirect = parseResponseForHtmlMetaEquiv(getResponse());
                      if (redirect != null)
                          response = new HttpHelper(this, redirect).getBytes();
                    }
                    catch (Exception ex) {
                        throw new IllegalArgumentException("Unparseable response: " + ex.getMessage(), ex);
                    }

                }
            }
            catch (IOException ex) {
                InputStream eis = null;
                try {
                    eis = connection.getErrorStream();
                    while (maxBytes == -1 || baos.size() < maxBytes) {
                        int bytesRead = eis.read(buffer);
                        if (bytesRead == -1)
                            break;
                        baos.write(buffer, 0, bytesRead);
                    }
                    response = baos.toByteArray();
                }
                catch (Exception ex2) {
                    // throw original exception
                }
                finally {
                    if (eis != null) {
                        eis.close();
                    }
                }
                throw ex;
            }
        }
        finally {
            if (is != null)
                is.close();
            connection.disconnect();
            responseCode = connection.getResponseCode();
            responseMessage = connection.getResponseMessage();
            headers = new HashMap<String,String>();
            for (String headerKey : connection.getHeaderFields().keySet()) {
                if (headerKey == null)
                    headers.put("HTTP", connection.getHeaderField(headerKey));
                else
                    headers.put(headerKey, connection.getHeaderField(headerKey));
            }
        }
    }

    /**
     * Perform an HTTP PUT request to the URL.
     * @param content bytes
     * @return string containing the response from the server
     */
    public byte[] putBytes(byte[] content) throws IOException {

        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);

        String contentType = connection.getRequestProperty("Content-Type");
        if (contentType == null)
            contentType = connection.getRequestProperty("content-type");
        if (contentType == null || contentType.isEmpty()) {
            /**
             * Default it to application/octet-stream if nothing has been specified
             */
            connection.setRequestProperty("Content-Type", "application/octet-stream");
        }

        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");

        OutputStream os = connection.getOutputStream();
        os.write(content);

        readInput(connection);
        os.close();
        return response;
    }

    /**
     * Perform an HTTP PATCH request to the URL.
     * @param content bytes
     * @return string containing the response from the server
     */
    public byte[] patchBytes(byte[] content) throws IOException {

        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);
        connection.setDoOutput(true);
        connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        connection.setRequestMethod("POST");

        OutputStream os = connection.getOutputStream();
        os.write(content);

        readInput(connection);
        os.close();
        return response;
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

        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);

        String contentType = connection.getRequestProperty("Content-Type");
        if (contentType == null)
            contentType = connection.getRequestProperty("content-type");
        if (contentType == null || contentType.isEmpty()) {
            /**
             * Default it to application/octet-stream if nothing has been specified
             */
            connection.setRequestProperty("Content-Type", "application/octet-stream");
        }
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);

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


        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer responseBuffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
            responseBuffer.append(line).append('\n');
        }

        reader.close();

        connection.disconnect();

        responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300)
        {
            String response = connection.getResponseMessage();
            throw new IOException("Error uploading file: " + responseCode + " -- " + response);
        }

        return responseBuffer.toString();
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

        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);
        connection.setRequestMethod("DELETE");

        OutputStream os = null;
        if (content != null) {
            connection.setDoOutput(true);
            os = connection.getOutputStream();
            os.write(content);
        }

        readInput(connection);
        if (os != null)
           os.close();
        return response;
    }

    public String mkcol() throws IOException {
        if (connection == null) {
            if (proxy == null)
                connection = (HttpURLConnection) url.openConnection();
            else
                connection = (HttpURLConnection) url.openConnection(proxy);
        }

        prepareConnection(connection);

        connection.setDoOutput(false);
        connection.setRequestMethod("MKCOL");

        HttpURLConnection.setFollowRedirects(true);

        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            is = connection.getInputStream();
            byte[] buffer = new byte[2048];
            while (maxBytes == -1 || baos.size() < maxBytes) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                baos.write(buffer, 0, bytesRead);
            }
        }
        finally {
            if (is != null)
                is.close();
            connection.disconnect();
            responseCode = connection.getResponseCode();
            responseMessage = connection.getResponseMessage();
            headers = new HashMap<String,String>();
            for (String headerKey : connection.getHeaderFields().keySet()) {
                headers.put(headerKey, connection.getHeaderField(headerKey));
            }
        }

        return baos.toString();
    }

    /**
     * Configures the connection timeout values and headers.
     */
    protected void prepareConnection(HttpURLConnection connection) throws IOException {
        if (readTimeout>=0) connection.setReadTimeout(readTimeout);
        if (connectTimeout>=0) connection.setConnectTimeout(connectTimeout);

        if (headers != null) {
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
        }
        if (user != null) {
            String value = user + ":" + password;
            connection.setRequestProperty(HTTP_BASIC_AUTH_HEADER, "Basic " + new String(Base64.encodeBase64(value.getBytes())));
        }
    }

    public static String getBasicAuthHeader(String user, String password) {
        String value = user + ":" + password;
        return "Basic " + new String(Base64.encodeBase64(value.getBytes()));
    }

    /**
     * In return array, zeroth element is user and first is password.
     */
    public static String[] extractBasicAuthCredentials(String authHeader) {
        return new String(Base64.decodeBase64(authHeader.substring("Basic ".length()).getBytes())).split(":");
    }

    /**
     * Use SAX for fastest parsing.
     */
    private URL parseResponseForHtmlMetaEquiv(String response) throws IOException, SAXException, ParserConfigurationException {
        final StringBuffer urlBuf = new StringBuffer();
        InputStream xmlStream = new ByteArrayInputStream(response.getBytes());
        InputSource src = new InputSource(xmlStream);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, new DefaultHandler()
        {
          boolean inHtml;
          boolean inHead;
          boolean inMeta;

          public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
          {
            if (qName.equals("html"))
              inHtml = true;
            else if (qName.equals("head"))
              inHead = true;
            else if (qName.equals("meta"))
              inMeta = true;

            if (inHtml && inHead && inMeta)
            {
              if ("refresh".equals(attrs.getValue("http-equiv")))
              {
                String cAttr = attrs.getValue("content");
                if (cAttr != null)
                {
                  int urlIdx = cAttr.indexOf("url=");
                  if (urlIdx >= 0)
                    urlBuf.append(cAttr.substring(urlIdx + 4));
                }
              }
            }
          }

          public void endElement(String uri, String localName, String qName) throws SAXException
          {
            if (qName.equals("html"))
              inHtml = false;
            else if (qName.equals("head"))
              inHead = false;
            else if (qName.equals("meta"))
              inMeta = false;
          }
        });

        String str = urlBuf.toString().trim();
        if (str.isEmpty())
            return null;
        else
            return new URL(str);
    }
}
