/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.ByteArrayBuffer;

/**
 * Uses the Apache HTTP Client (supports PATCH).
 */
public class HttpAltConnection extends HttpConnection {

    private CloseableHttpClient httpClient;
    private HttpHost proxy;
    private HttpRequestBase methodRequest;
    private CloseableHttpResponse httpResponse;

    public HttpAltConnection(URL url) {
        super(url);
    }

    public HttpAltConnection(URL url, String user, String password) {
        super(url, user, password);
    }

    @Override
    public boolean isOpen() {
        return httpClient != null;
    }

    @Override
    public void open() throws IOException {
        if (getProxyHost() != null) {
            int port = getProxyPort() == 0 ? getUrl().getPort() : getProxyPort();
            this.proxy = new HttpHost(getProxyHost(), port, getProxyProtocol());
        }
        httpClient = HttpClients.createDefault();
    }

    /**
     * Note SOCKS protocol not supported.
     */
    void open(HttpHost proxy) throws IOException {
        this.proxy = proxy;
        httpClient = HttpClients.createDefault();
    }

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream getByteArrayOutputStream() throws IOException {
        return ((ByteArrayOutputStream)getOutputStream());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null)
            outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    @Override
    public void prepare(String method) throws IOException {

        RequestConfig.Builder configBuilder = RequestConfig.custom();

        if (proxy != null)
            configBuilder.setProxy(proxy).build();

        if (getReadTimeout() > 0)
            configBuilder.setSocketTimeout(getReadTimeout());
        if (getConnectTimeout() > 0)
            configBuilder.setConnectTimeout(getConnectTimeout());

        try {
            URI uri = getUrl().toURI();
            if ("GET".equalsIgnoreCase(method)) {
                methodRequest = new HttpGet(uri);
            }
            else if ("POST".equalsIgnoreCase(method)) {
                methodRequest = new HttpPost(uri);
            }
            else if ("PUT".equalsIgnoreCase(method)) {
                methodRequest = new HttpPut(uri);
            }
            else if ("DELETE".equalsIgnoreCase(method)) {
                methodRequest = new HttpDelete(uri);
            }
            else if ("PATCH".equalsIgnoreCase(method)) {
                methodRequest = new HttpPatch(uri);
            }

            if (getHeaders() != null) {
                for (String key : getHeaders().keySet()) {
                    methodRequest.setHeader(key, getHeaders().get(key));
                }
            }
            if (getUser() != null) {
                String value = getUser() + ":" + getPassword();
                methodRequest.setHeader(HTTP_BASIC_AUTH_HEADER, "Basic " + new String(Base64.encodeBase64(value.getBytes())));
            }
        }
        catch (URISyntaxException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    @Override
    protected HttpResponse readInput() throws IOException {
        try {
            if (methodRequest instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) methodRequest;
                ByteArrayOutputStream outputStream = getByteArrayOutputStream();
                outputStream.flush();
                entityRequest.setEntity(new ByteArrayEntity(outputStream.toByteArray()));
            }
            httpResponse = httpClient.execute(methodRequest);
            response = new HttpResponse(extractResponseBytes(httpResponse.getEntity().getContent()));
            return response;
        }
        finally {
            httpClient.close();
            if (httpResponse != null)
                httpResponse.close();
            if (response != null && httpResponse != null) {
                response.setCode(httpResponse.getStatusLine().getStatusCode());
                response.setMessage(httpResponse.getStatusLine().getReasonPhrase());
                headers = new HashMap<String,String>();
                for (Header header : httpResponse.getAllHeaders()) {
                    headers.put(header.getName(), header.getValue());
                }
            }
        }
    }

    private byte[] extractResponseBytes(InputStream is) throws IOException {
        BufferedInputStream bis = null;
        BufferedReader br = null;
        try {
            ByteArrayBuffer baf = new ByteArrayBuffer(1024);
            bis = new BufferedInputStream(is);
            int b = 0;
            while ((b = bis.read()) != -1)
                baf.append((byte) b);
            byte[] bytes = baf.toByteArray();
            return bytes;
        }
        finally {
            if (bis != null)
                bis.close();
            if (br != null)
                br.close();
        }
    }

}
