package com.subgraph.vega.impl.scanner.state;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import com.subgraph.vega.api.model.web.IWebPath;
import com.subgraph.vega.impl.scanner.VegaHttpRequest;

public class PathStateRequestBuilder {
	
	
	private final IWebPath path;
	private final List<NameValuePair> parameters;
	private final int parameterFuzzIndex;
	
	public PathStateRequestBuilder(IWebPath path) {
		this(path, null, -1);
	}
	
	public PathStateRequestBuilder(IWebPath path, List<NameValuePair> parameters, int index) {
		this.path = path;
		this.parameters = parameters;
		this.parameterFuzzIndex = index;
	}
	
	private boolean haveParameters() {
		return parameters != null && parameterFuzzIndex >= 0 && parameterFuzzIndex < parameters.size();
	}
	
	public NameValuePair getFuzzableParameter() {
		if(haveParameters())
			return parameters.get(parameterFuzzIndex);
		else 
			return null;
	}
	
	public String formatDefaultParameters() {
		return formatParametersWithFuzz(null, null, false);
	}
	
	public String formatParametersWithFuzz(String name, String value, boolean append) {
		if(!haveParameters())
			return null;
		
		final StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < parameters.size(); i++) {
			NameValuePair p = parameters.get(i);
			String n = p.getName();
			String v = p.getValue();
			if(i > 0)
				sb.append("&");
			
			if(parameterFuzzIndex == i) {
				if(name != null)
					n = name;
				if(value != null && v != null)
					v = (append) ? (v + value) : (value);
			}
						
			sb.append(n);
			if(v != null) {
				sb.append("=");
				sb.append(v);
			}
				
			
		}
		return sb.toString();
		
	}
	
	
	private HttpUriRequest createGetRequestWithQuery(String query) {
		final URI u = path.getUri();
		try {
			final URI reqURI = new URI(u.getScheme(), u.getAuthority(), u.getPath(), query, null);
			return VegaHttpRequest.createGET(reqURI);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public HttpUriRequest createGetRequestFuzzParam(String fuzzValue, boolean append) {
		final String query = formatParametersWithFuzz(null, fuzzValue, append);
		return createGetRequestWithQuery(query);
	}
	
	public HttpUriRequest createGetRequestFuzzParamName(String fuzzValue) {
		final String query = formatParametersWithFuzz(fuzzValue, null, false);
		return createGetRequestWithQuery(query);
	}
	
	private URI maybeAddTrailingSlash(URI uri) {
		if(uri.getPath().endsWith("/"))
			return uri;
		final String path = uri.getPath() + "/";
		try {
			return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), null);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private URI maybeRemoveTrailingSlash(URI uri) {
		if(!uri.getPath().endsWith("/"))
			return uri;
		String p = uri.getPath();
		while(p.length() > 0 && p.endsWith("/"))
			p = p.substring(0, p.length() - 1);
		try {
			return new URI(uri.getScheme(), uri.getAuthority(), p, uri.getQuery(), null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private URI getUriForPathType(IWebPath path) {
		switch(path.getPathType()) {
		case PATH_DIRECTORY:
			return maybeAddTrailingSlash(path.getUri());
		case PATH_PATHINFO:
			return maybeRemoveTrailingSlash(path.getUri());
		default:
			return path.getUri();
		}
	}

	public HttpUriRequest createGetRequest() {
		if(haveParameters()) {
			final String query = formatDefaultParameters();
			return createGetRequestWithQuery(query);
		} else {
			return new HttpGet(getUriForPathType(path));
		}
	} 
	
	public HttpUriRequest createGetRequestAppendPathSuffix(String suffix) {
		final URI u = path.getUri();
		final String newPath = createPathWithSuffix(u.getPath(), suffix);
		try {
			final URI reqURI = new URI(u.getScheme(), u.getAuthority(), newPath, u.getQuery(), null);
			return VegaHttpRequest.createGET(reqURI);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private String createPathWithSuffix(String oldPath, String suffix) {
		if(oldPath.endsWith("/") && suffix.startsWith("/"))
			return oldPath + suffix.substring(1);
		else if(oldPath.endsWith("/") || suffix.startsWith("/"))
			return oldPath + suffix;
		else
			return oldPath + "/" + suffix;
	}
	
	public HttpUriRequest createAlteredRequest(String value, boolean append) {
		if(haveParameters()) 
			return createGetRequestFuzzParam(value, append);
		else
			return createGetRequestAppendPathSuffix(value);
	}
	
	public HttpUriRequest createAlteredParameterNameRequest(String name) {
		return createGetRequestFuzzParamName(name);
	}

}
