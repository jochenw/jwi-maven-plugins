package com.github.jochenw.jmp.jwicrl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.jochenw.afw.core.util.FileUtils;
import com.github.jochenw.afw.core.util.HttpConnector;
import com.github.jochenw.afw.core.util.HttpConnector.HttpConnection;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.afw.core.util.Strings;


@Mojo(name="request")
public class JwiCrlMojo extends AbstractMojo {
	@Parameter(property="jwicrl.url", required=true)
	private String url;

	@Parameter(property="jwicrl.method", defaultValue="GET", required=true) 
	private String method;

	@Parameter(property="jwicrl.encoding", defaultValue="UTF-8", required=true)
	private String encoding;

	@Parameter(property="jwicrl.outputFile")
	private Path outputFile;

	@Parameter(property="jwicrl.skip", defaultValue="false", required=true)  
	private boolean skip;

	@Parameter()
	private Map<String,String> headers;

	@Parameter()
	private Map<String,String> queryParameters;

	@Parameter(property="jwicrl.auth.usr")
	private String authUser;

	@Parameter(property="jwicrl.auth.pwd")
	private String authPassword;

	@Parameter(property="jwicrl.trustAll", defaultValue="false", required=true)
	private boolean trustAll;

	@Parameter(property="jwicrl.trustStore")
	private Path trustStore;

	@Parameter(property="jwicrl.trustStorePwd")
	private String trustStorePwd;

	@Parameter(property="jwicrl.proxy")
	private String proxy;

    /**
     * The character encoding to use when reading and writing filtered resources.
     */
    @Parameter(defaultValue="${project.build.sourceEncoding}")
    protected String defaultEncoding;
    
	@Parameter(defaultValue="${project}", readonly=true, required=true)
	private MavenProject project;

	protected HttpConnector getHttpConnector() throws MojoFailureException {
		final HttpConnector connector = new HttpConnector();
		if (trustAll) {
			connector.setTrustingAllCertificates(true);
		}
		if (proxy != null) {
			connector.setProxy(proxy);
		}
		if (trustStore != null) {
			if (!Files.isRegularFile(trustStore)) {
				throw new MojoFailureException("Invalid parameter 'trustStore': Expected existing file, got " + trustStore);
			}
			connector.setTruststore(trustStore, trustStorePwd);
		}
		return connector;
	}

	protected String getEncoding() {
		if (encoding != null) {
			final String trimmedEncoding = encoding.trim();
			if (trimmedEncoding.length() > 0) {
				getLog().debug("Encoding: Using configured value " + trimmedEncoding);
				return trimmedEncoding;
			}
		}
		if (defaultEncoding != null) {
			final String trimmedDefaultEncoding = defaultEncoding.trim();
			getLog().debug("Encoding: Using default value from build: " + trimmedDefaultEncoding);
			return trimmedDefaultEncoding;
		}
		final String systemEncoding = System.getProperty("file.encoding");
		if (systemEncoding != null) {
			getLog().debug("Encoding: Using system default: " + systemEncoding);
			return systemEncoding;
		}
		getLog().debug("Encoding: Using default value: UTF-8");
		return "UTF-8";
	}

	protected String urlEncoded(String pValue, String pEncoding) throws MojoExecutionException {
		try {
			return URLEncoder.encode(pValue, pEncoding);
		} catch (UnsupportedEncodingException uee) {
			throw new MojoExecutionException(uee);
		}
	}

	protected URL getRequestUrl(String pEncoding) throws MojoExecutionException {
		final String urlStr;
		if (queryParameters == null  ||  queryParameters.isEmpty()) {
			urlStr = url;
		} else {
			final StringBuilder sb = new StringBuilder();
			for (Map.Entry<String,String> en : queryParameters.entrySet()) {
				if (sb.indexOf("?") == -1) {
					// First parameter
					sb.append("?");
				} else {
					sb.append("&");
				}
				sb.append(urlEncoded(en.getKey(), pEncoding));
				sb.append("=");
				sb.append(urlEncoded(en.getValue(), pEncoding));
			};
			urlStr = sb.toString();
		}
		try {
			return Strings.asUrl(urlStr);
		} catch (MalformedURLException mue) {
			throw new MojoExecutionException(mue);
		}
	}

	protected boolean hasBody() {
		return "GET".equalsIgnoreCase(method)  ||  "HEAD".equalsIgnoreCase(method);
	}

	protected void setHeaders(HttpURLConnection pUrlConnection, String pEncoding) {
		if (headers != null  &&  !headers.isEmpty()) {
			for (Map.Entry<String,String> en : headers.entrySet()) {
				pUrlConnection.addRequestProperty(en.getKey(), en.getValue());
			}
		}
		if (authUser != null) {
			final String authStr = authUser + ":" + authPassword;
			final Charset charset = Charset.forName(pEncoding);
			
			final byte[] authBytes = authStr.getBytes(charset);
			final String authBase64 = new String(Base64.getMimeEncoder(0, "\n".getBytes()).encode(authBytes), charset);
			pUrlConnection.addRequestProperty("Authorization", "Basic " + authBase64);
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping, parameter skip=true");
			return;
		}
		final HttpConnector httpConnector = getHttpConnector();
		final String encoding = getEncoding();
		final URL url = getRequestUrl(encoding);
		getLog().debug("Url: " + url);
		try (HttpConnection httpConnection = httpConnector.connect(url)) {
			final HttpURLConnection urlConnection = httpConnection.getUrlConnection();
			setHeaders(urlConnection, encoding);
			urlConnection.setDoOutput(hasBody());
			urlConnection.setDoInput(true);
			final int statusCode  = urlConnection.getResponseCode();
			final String statusMessage = urlConnection.getResponseMessage();
			if (statusCode >= 200  &&  statusCode < 300) {
				getLog().debug("Response: " + statusCode + ", " + statusMessage);
				if (outputFile != null) {
					getLog().debug("Writing response to " + outputFile);
					FileUtils.createDirectoryFor(outputFile);
					try (OutputStream out = Files.newOutputStream(outputFile)) {
						Streams.copy(urlConnection.getInputStream(), out);
					}
				} else {
					getLog().debug("Writing response to System.out");
					Streams.copy(urlConnection.getInputStream(), System.out);
				}
			} else {
				final String errorMsg = "Response error: " + statusCode + ", " + statusMessage;
				getLog().error(errorMsg);
				if (getLog().isDebugEnabled()) {
					getLog().error("Writing response to System.err");
					Streams.copy(urlConnection.getErrorStream(), System.err);
				}
				throw new MojoExecutionException(errorMsg);
			}
		} catch (IOException ioe) {
			throw new MojoExecutionException(ioe);
		}
	}

}
