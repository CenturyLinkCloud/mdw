/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.rest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.adapter.HeaderAwareAdapter;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.AdapterActivityBase;

@Tracked(LogLevel.TRACE)
public class RestfulServiceAdapter extends AdapterActivityBase implements HeaderAwareAdapter {

	public static final String HTTP_METHOD = "HttpMethod";
	public static final String ENDPOINT_URI = "EndpointUri"; // includes the
																// resource path
	public static final String CONNECT_TIMEOUT = "ConnectTimeout";
	public static final String READ_TIMEOUT = "ReadTimeout";
	public static final String HEADERS_VARIABLE = "HeadersVariable";
	private static final String PROP_USER = "USER";
	private static final String PROP_PASS = "PASS";

	@Override
	public final boolean isSynchronous() {
		return true;
	}

	/**
	 * Returns an HttpURLConnection based on the configured endpoint, which
	 * includes the resource path. Override for HTTPS or other connection type.
	 */
	@Override
	protected Object openConnection() throws ConnectionException {
		try {
			String endpointUri = getEndpointUri();
			Map<String, String> params = getRequestParameters();
			if (params != null && !params.isEmpty()) {
				StringBuilder query = new StringBuilder();
				for (String name : params.keySet()) {
					String encoding = getUrlEncoding();
					if (encoding == null) {
						query.append(name).append("=").append(params.get(name));

					} else {
						query.append(URLEncoder.encode(name, encoding)).append("=")
								.append(URLEncoder.encode(params.get(name), encoding));
					}
				}
				endpointUri += endpointUri.indexOf('?') < 0 ? '?' : '&';
				endpointUri += query.substring(1);
			}
			URL url = new URL(endpointUri);
			return url.openConnection();
		} catch (Exception ex) {
			throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
		}
	}

	@Override
	protected void closeConnection(Object connection) {
		// connection is closed by HttpHelper
	}

	/**
	 * Override to specify URL request parameters.
	 */
	protected Map<String, String> getRequestParameters() {
		return null;
	}

	protected String getUrlEncoding() {
		return "UTF-8";
	}

	/**
	 * Returns the endpoint URL for the RESTful service. Supports property
	 * specifiers via the syntax for
	 * {@link #AdapterActivityBase.getAttributeValueSmart(String)}.
	 */
	protected String getEndpointUri() throws PropertyException {
		return getAttributeValueSmart(ENDPOINT_URI);
	}

	@Override
	public Object invoke(Object conn, Object requestData) throws AdapterException {
		return invoke(conn, requestData, null);
	}

	/**
	 * Invokes the RESTful service by submitting an HTTP request against the
	 * configured endpoint URI. Override getRequestData() to provide the
	 * requestData value (usually a String).
	 */
	@Override
	public Object invoke(Object conn, Object requestData, Map<String, String> headers) throws AdapterException {
		HttpHelper httpHelper = null;
		try {
			httpHelper = getHttpHelper(conn);

			int connectTimeout = getConnectTimeout();
			if (connectTimeout > 0)
				httpHelper.setConnectTimeout(connectTimeout);

			int readTimeout = getReadTimeout();
			if (readTimeout > 0)
				httpHelper.setReadTimeout(readTimeout);

			if (headers != null)
				httpHelper.setHeaders(headers);

			String httpMethod = getHttpMethod();
			switch (httpMethod) {
			case "GET":
				return httpHelper.get();
			case "POST":
				return httpHelper.post(requestData.toString());
			case "PUT":
				return httpHelper.put(requestData.toString());
			case "DELETE":
				return httpHelper.delete();
			default:
				throw new AdapterException("Unsupported HTTP Method: " + httpMethod);
			}
		} catch (IOException ex) {
			AdapterException adapEx = new AdapterException(-1, ex.getMessage(), ex);
			if (isRetryable(ex))
				adapEx.setIsRetryableError(true);
			throw adapEx;
		} catch (Exception ex) {
			int responseCode = -1;
			if (httpHelper != null)
				responseCode = httpHelper.getResponseCode();
			throw new AdapterException(responseCode, ex.getMessage(), ex);
		} finally {
			if (httpHelper != null)
				setResponseHeaders(httpHelper.getHeaders());
		}
	}

	/**
	 * <p>
	 * Allows the overriding of this method without overriding invoke() Default
	 * implementation is using the constructor new
	 * HttpHelper((HttpURLConnection)connection, user, pass);
	 * </p>
	 *
	 * @return an HttpHelper instance
	 * @throws PropertyException
	 */
	public HttpHelper getHttpHelper(Object connection) throws PropertyException {
		String user = getAttributeValueSmart(PROP_USER);
		String pass = getAttributeValueSmart(PROP_PASS);
		return new HttpHelper((HttpURLConnection) connection, user, pass);
	}

	/**
	 * The method overrides the one from the super class and does the following:
	 * <ul>
	 * <li>For HTTP GET and DELETE requests, it returns an empty string</li>
	 * <li>Otherwise it gets the value of the variable with the name specified
	 * in the attribute REQUEST_VARIABLE. The value is typically an XML document
	 * or a string</li>
	 * <li>It invokes the variable translator to convert the value into a string
	 * and then returns the string value.</li>
	 * </ul>
	 * For HTTP methods other than GET and DELETE this will throw an exception
	 * if the request data variable is not bound, or the value is not a
	 * DocumentReference or String.
	 */
	@Override
	protected Object getRequestData() throws ActivityException {
		String httpMethod = getHttpMethod();
		if (httpMethod.equals("GET") || httpMethod.equals("DELETE"))
			return "";

		Object request = super.getRequestData();
		if (request == null)
			throw new ActivityException("Request data attribute is missing for HTTP method: " + httpMethod);

		if (request instanceof String)
			return request;
		else if (request instanceof DocumentReference)
			return getDocumentContent((DocumentReference) request);
		else
			throw new ActivityException("Cannot handle request of type " + request.getClass().getName());
	}

	protected String getHttpMethod() throws ActivityException {
		try {
			String httpMethod = getAttributeValueSmart(HTTP_METHOD);
			if (httpMethod == null)
				throw new ActivityException("RESTful adapter required attribute missing: " + HTTP_METHOD);
			return httpMethod;
		} catch (PropertyException ex) {
			throw new ActivityException(-1, ex.getMessage(), ex);
		}
	}

	protected int getConnectTimeout() throws ActivityException {
		String connectTimeout = null;
		try {
			connectTimeout = getAttributeValueSmart(CONNECT_TIMEOUT);
			if (connectTimeout != null)
				return Integer.parseInt(connectTimeout);
			return -1;
		} catch (NumberFormatException ex) {
			throw new ActivityException("Invalid value for Connect Timeout attribute: " + connectTimeout);
		} catch (PropertyException ex) {
			throw new ActivityException(-1, ex.getMessage(), ex);
		}
	}

	protected int getReadTimeout() throws ActivityException {
		String readTimeout = null;
		try {
			readTimeout = getAttributeValueSmart(READ_TIMEOUT);
			if (readTimeout != null)
				return Integer.parseInt(readTimeout);
			return -1;
		} catch (NumberFormatException ex) {
			throw new ActivityException("Invalid value for Request Timeout attribute: " + readTimeout);
		} catch (PropertyException ex) {
			throw new ActivityException(-1, ex.getMessage(), ex);
		}
	}

	/**
	 * Override to specify HTTP request headers.
	 */
	public Map<String, String> getRequestHeaders() {
		try {
			Map<String, String> headers = null;
			String headersVar = getAttributeValueSmart(HEADERS_VARIABLE);
			if (headersVar != null) {
				Process processVO = getProcessDefinition();
				Variable variableVO = processVO.getVariable(headersVar);
				if (variableVO == null)
					throw new ActivityException(
							"Headers variable '" + headersVar + "' is not defined for process " + processVO.getLabel());
				if (!variableVO.getVariableType().equals(Map.class.getName()))
					throw new ActivityException("Headers variable '" + headersVar + "' must be of type java.util.Map");
				Object headersObj = getVariableValue(headersVar);
				if (headersObj != null) {
					headers = new HashMap<String, String>();
					for (Object key : ((Map<?, ?>) headersObj).keySet()) {
						headers.put(key.toString(), ((Map<?, ?>) headersObj).get(key).toString());
					}
				}
			}
			return headers;
		} catch (Exception ex) {
			logger.severeException(ex.getMessage(), ex);
			return null;
		}
	}

	protected boolean isRetryable(IOException ex) {
		// timeout and connection errors are retryable by default
		return ex instanceof SocketTimeoutException || ex instanceof ConnectException;
	}

	private Map<String, String> responseHeaders;

	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	protected void setResponseHeaders(Map<String, String> headers) {
		this.responseHeaders = headers;
	}

	@Override
	protected Response getResponse(Object connection, Object responseObject) throws IOException {
		Response response = super.getResponse(connection, responseObject);
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection httpConn = (HttpURLConnection) connection;
			response.setStatusCode(httpConn.getResponseCode());
			response.setStatusMessage(httpConn.getResponseMessage());
		}
		return response;
	}

}
