package com.sendgrid;

import okhttp3.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class Client allows for quick and easy access any REST or REST-like API.
 */
public class Client implements Closeable {

	private OkHttpClient httpClient;
	private Boolean      test;
	private boolean      createdHttpClient;

	/**
	 * Constructor for using the default CloseableHttpClient.
	 */
	public Client() {
		this.httpClient = new OkHttpClient.Builder().build();
		this.test = false;
		this.createdHttpClient = true;
	}

	/**
	 * Constructor for passing in an httpClient, typically for mocking. Passed-in httpClient will not be closed by this Client.
	 *
	 * @param httpClient an Apache CloseableHttpClient
	 */
	public Client(OkHttpClient httpClient) {
		this(httpClient, false);
	}

	/**
	 * Constructor for passing in a test parameter to allow for http calls.
	 *
	 * @param test is a Bool
	 */
	public Client(Boolean test) {
		this(new OkHttpClient.Builder().build(), test);
	}

	/**
	 * Constructor for passing in an httpClient and test parameter to allow for http calls.
	 *
	 * @param httpClient an Apache CloseableHttpClient
	 * @param test       is a Bool
	 */
	public Client(OkHttpClient httpClient, Boolean test) {
		this.httpClient = httpClient;
		this.test = test;
		this.createdHttpClient = true;
	}

	/**
	 * Add query parameters to a URL.
	 *
	 * @param baseUri     (e.g. "api.sendgrid.com")
	 * @param endpoint    (e.g. "/your/endpoint/path")
	 * @param queryParams map of key, values representing the query parameters
	 * @throws URISyntaxException in of a URI syntax error
	 */
	public HttpUrl buildUri(String baseUri, String endpoint, Map<String, String> queryParams) throws URISyntaxException {
		HttpUrl.Builder httpUrlBuilder = new HttpUrl.Builder();
		StringBuilder urlString = new StringBuilder();

		if (this.test) {
			httpUrlBuilder.scheme("http");
		} else {
			httpUrlBuilder.scheme("https");
		}

		httpUrlBuilder.host(baseUri);
		httpUrlBuilder.encodedPath(endpoint);

		if (queryParams != null) {
			for (Map.Entry<String, String> entry : queryParams.entrySet()) {
				httpUrlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
			}
		}

		return httpUrlBuilder.build();
	}

	/**
	 * Prepare a Response object from an API call via Apache's HTTP client.
	 *
	 * @param response from a call to a CloseableHttpClient
	 * @return the response object
	 * @throws IOException in case of a network error
	 */
	public Response getResponse(okhttp3.Response response) throws IOException {
		ResponseBody responseBody = response.body();
		if (responseBody == null) { return null; }
		String responseBodyString = responseBody.string();

		int statusCode = response.code();

		Headers headers = response.headers();
		Map<String, String> responseHeaders = new HashMap<String, String>();
		for (String name : headers.names()) {
			responseHeaders.put(name, headers.get(name));
		}

		return new Response(statusCode, responseBodyString, responseHeaders);
	}

	public okhttp3.Request.Builder req(Request request) throws URISyntaxException, IOException {
		Headers.Builder headersBuilder = new Headers.Builder();

		if (request.getHeaders() != null) {
			for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
				headersBuilder.set(entry.getKey(), entry.getValue());
			}
		}

		HttpUrl uri = buildUri(request.getBaseUri(), request.getEndpoint(), request.getQueryParams());
		return new okhttp3.Request.Builder()				.url(uri).headers(headersBuilder.build());
	}

	/**
	 * Make a GET request and provide the status code, response body and response headers.
	 *
	 * @param request the request object
	 * @return the response object
	 * @throws URISyntaxException in case of a URI syntax error
	 * @throws IOException        in case of a network error
	 */
	public Response get(Request request) throws URISyntaxException, IOException {
		okhttp3.Request req = req(request).get().build();
		return executeApiCall(req);
	}

	/**
	 * Make a POST request and provide the status code, response body and response headers.
	 *
	 * @param request the request object
	 * @return the response object
	 * @throws URISyntaxException in case of a URI syntax error
	 * @throws IOException        in case of a network error
	 */
	public Response post(Request request) throws URISyntaxException, IOException {
		RequestBody requestBody = RequestBody.create(request.getBody(), MediaType.get("application/json"));
		okhttp3.Request req = req(request).post(requestBody).build();
		return executeApiCall(req);
	}

	/**
	 * Make a PATCH request and provide the status code, response body and response headers.
	 *
	 * @param request the request object
	 * @return the response object
	 * @throws URISyntaxException in case of a URI syntax error
	 * @throws IOException        in case of a network error
	 */
	public Response patch(Request request) throws URISyntaxException, IOException {
		RequestBody requestBody = RequestBody.create(request.getBody(), MediaType.get("application/json"));
		okhttp3.Request req = req(request).patch(requestBody).build();
		return executeApiCall(req);
	}

	/**
	 * Make a PUT request and provide the status code, response body and response headers.
	 *
	 * @param request the request object
	 * @return the response object
	 * @throws URISyntaxException in case of a URI syntax error
	 * @throws IOException        in case of a network error
	 */
	public Response put(Request request) throws URISyntaxException, IOException {
		RequestBody requestBody = RequestBody.create(request.getBody(), MediaType.get("application/json"));
		okhttp3.Request req = req(request).put(requestBody).build();
		return executeApiCall(req);
	}

	/**
	 * Make a DELETE request and provide the status code and response headers.
	 *
	 * @param request the request object
	 * @return the response object
	 * @throws URISyntaxException in case of a URI syntax error
	 * @throws IOException        in case of a network error
	 */
	public Response delete(Request request) throws URISyntaxException, IOException {
		RequestBody requestBody = RequestBody.create(request.getBody(), MediaType.get("application/json"));
		okhttp3.Request req = req(request).delete(requestBody).build();
		return executeApiCall(req);
	}

	/**
	 * Makes a call to the client API.
	 *
	 * @param request the request method object
	 * @return the response object
	 * @throws IOException in case of a network error
	 */
	private Response executeApiCall(okhttp3.Request request) throws IOException {
		okhttp3.Response serverResponse = httpClient.newCall(request).execute();
		try {
			return getResponse(serverResponse);
		} finally {
			serverResponse.close();
		}
	}

	/**
	 * A thin wrapper around the HTTP methods.
	 *
	 * @param request the request object
	 * @return the response object
	 * @throws IOException in case of a network error
	 */
	public Response api(Request request) throws IOException {
		try {
			if (request.getMethod() == null) {
				throw new IOException("We only support GET, PUT, PATCH, POST and DELETE.");
			}
			switch (request.getMethod()) {
				case GET:
					return get(request);
				case POST:
					return post(request);
				case PUT:
					return put(request);
				case PATCH:
					return patch(request);
				case DELETE:
					return delete(request);
				default:
					throw new IOException("We only support GET, PUT, PATCH, POST and DELETE.");
			}
		} catch (IOException ex) {
			throw ex;
		} catch (URISyntaxException ex) {
			StringWriter errors = new StringWriter();
			ex.printStackTrace(new PrintWriter(errors));
			throw new IOException(errors.toString());
		}
	}

	/**
	 * Closes the http client.
	 *
	 * @throws IOException in case of a network error
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * Closes and finalizes the http client.
	 *
	 * @throws Throwable in case of an error
	 */
	@Override
	public void finalize() throws Throwable {
		try {
			close();
		} catch (IOException e) {
			throw new Throwable(e.getMessage());
		} finally {
			super.finalize();
		}
	}
}
