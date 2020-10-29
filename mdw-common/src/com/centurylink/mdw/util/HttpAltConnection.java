package com.centurylink.mdw.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.ByteArrayBuffer;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

/**
 * Uses the Apache HTTP Client (supports PATCH).
 */
public class HttpAltConnection extends HttpConnection {

    private CloseableHttpClient httpClient;
    private HttpHost proxy;
    private HttpRequestBase methodRequest;
    public String getMethod() {
        return methodRequest == null ? null : methodRequest.getMethod();
    }

    private CloseableHttpResponse httpResponse;

    private String proxyProtocol = "http";
    public String getProxyProtocol() { return proxyProtocol; }
    public void setProxyProtocol(String protocol) { this.proxyProtocol = protocol; }

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
    @SuppressWarnings("squid:S2647") // we need to support basic auth
    /**
     * Prepares HTTP request.
     *
     * @throws IOException in case of a problem or the connection was aborted
     */
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

    /**
     * Executes HTTP request.
     *
     * @return  the response to the request.
     * @throws IOException in case of a problem or the connection was aborted
     */
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
                headers = new HashMap<>();
                for (Header header : httpResponse.getAllHeaders()) {
                    headers.put(header.getName(), header.getValue());
                }
            }
        }
    }
    /**
     * Converts the inpust stream to byte array.
     * @param is input stream to be converted
     * @return  byte[] byte array
     * @throws IOException in case of a problem
     */
    private byte[] extractResponseBytes(InputStream is) throws IOException {
        BufferedInputStream bis = null;
        try {
            ByteArrayBuffer baf = new ByteArrayBuffer(1024);
            bis = new BufferedInputStream(is);
            int b;
            while ((b = bis.read()) != -1)
                baf.append((byte) b);
            return baf.toByteArray();
        }
        finally {
            if (bis != null)
                bis.close();
        }
    }

}
