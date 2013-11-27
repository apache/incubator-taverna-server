/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.Namespaces.XLINK;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.web.PortMapper;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * A class that makes it simpler to work with an element with a {@link URI} in
 * an <tt>href</tt> attribute. Done with JAXB.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "Location")
public class Uri {
	static Log log = getLog("Taverna.Server.UriRewriter");
	private static final String SECURE_SCHEME = "https";
	/**
	 * This type is characterised by an attribute that is the reference to some
	 * other element.
	 */
	@XmlAttribute(name = "href", namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI ref;

	/** Make a reference that points nowhere. */
	public Uri() {
	}

	/**
	 * Make a reference to the given location.
	 * 
	 * @param ref
	 *            Where to point to.
	 */
	public Uri(@NonNull URI ref) {
		this.ref = secure(ref);
	}

	/**
	 * Make a reference from the factory with the given parameters.
	 * 
	 * @param ub
	 *            The configured factory.
	 * @param strings
	 *            The parameters to the factory.
	 */
	public Uri(@NonNull UriBuilder ub, String... strings) {
		ref = secure(ub).build((Object[]) strings);
	}

	/**
	 * Make a reference from the factory with the given parameters.
	 * 
	 * @param ui
	 *            The factory factory.
	 * @param path
	 *            The path to configure the factory with.
	 * @param strings
	 *            The parameters to the factory.
	 */
	public Uri(@NonNull UriInfo ui, @NonNull String path, String... strings) {
		this(ui, true, path, strings);
	}

	/**
	 * Make a reference from the factory with the given parameters.
	 * 
	 * @param ui
	 *            The factory factory.
	 * @param secure
	 *            Whether the URI should be required to use HTTPS.
	 * @param path
	 *            The path to configure the factory with.
	 * @param strings
	 *            The parameters to the factory.
	 */
	public Uri(@NonNull UriInfo ui, boolean secure, @NonNull String path,
			String... strings) {
		UriBuilder ub = ui.getAbsolutePathBuilder();
		if (secure) {
			ub = secure(ub);
		}
		ref = ub.path(path).build((Object[]) strings);
	}

	public static UriBuilder secure(UriBuilder ub) {
		return Rewriter.getInstance().getSecuredUriBuilder(ub);
	}

	public static UriBuilder secure(UriInfo ui) {
		return secure(ui.getAbsolutePathBuilder());
	}

	public static URI secure(URI uri) {
		URI newURI = secure(fromUri(uri)).build();
		log.debug("rewrote " + uri + " to " + newURI);
		return newURI;
	}

	public static URI secure(URI base, String uri) {
		URI newURI = secure(fromUri(base.resolve(uri))).build();
		log.debug("rewrote " + uri + " to " + newURI);
		return newURI;
	}

	/**
	 * A bean that allows configuration of how to rewrite generated URIs to be
	 * secure.
	 * 
	 * @author Donal Fellows
	 */
	public static class Rewriter {
		private static Rewriter instance;
		private PortMapper portMapper;
		private boolean suppress;
		private String rewriteRE = "://[^/]+/[^/]+";
		private String rewriteTarget;

		static Rewriter getInstance() {
			if (instance == null)
				new Rewriter();
			return instance;
		}

		@Autowired
		@Required
		public void setPortMapper(PortMapper portMapper) {
			this.portMapper = portMapper;
		}

		/**
		 * Whether to suppress rewriting of URIs to be secure.
		 * 
		 * @param suppressSecurity
		 *            True if no rewriting should be done.
		 */
		public void setSuppressSecurity(boolean suppressSecurity) {
			suppress = suppressSecurity;
		}

		public void setRewriteRegexp(String rewriteRegexp) {
			this.rewriteRE = rewriteRegexp;
		}

		/**
		 * What to rewrite the host, port and web-app name to be.
		 * 
		 * @param rewriteTarget
		 *            What to rewrite to, or "<tt>NONE</tt>" for no rewrite.
		 */
		public void setRewriteTarget(String rewriteTarget) {
			if (rewriteTarget.isEmpty())
				this.rewriteTarget = null;
			else if (rewriteTarget.equals("NONE"))
				this.rewriteTarget = null;
			else if (rewriteTarget.startsWith("${"))
				this.rewriteTarget = null;
			else
				this.rewriteTarget = "://" + rewriteTarget;
		}

		private Integer lookupHttpsPort(URI uri) {
			if (portMapper != null)
				return portMapper.lookupHttpsPort(uri.getPort());
			return null;
		}

		@SuppressWarnings
		public Rewriter() {
			instance = this;
		}

		@PreDestroy
		@SuppressWarnings
		public void done() {
			instance = null;
			Uri.log = null;
		}

		@NonNull
		URI rewrite(@NonNull String url) {
			if (rewriteTarget != null)
				url = url.replaceFirst(rewriteRE, rewriteTarget);
			return URI.create(url);
		}

		@NonNull
		public UriBuilder getSecuredUriBuilder(@NonNull UriBuilder uribuilder) {
			if (suppress)
				return uribuilder.clone();
			UriBuilder ub = new RewritingUriBuilder(uribuilder);
			Integer secPort = null;
			try {
				secPort = lookupHttpsPort(ub.build());
			} catch (Exception e) {
				/*
				 * Do not log this; we know why it happens and don't actually
				 * care to do anything about it. All it does is fill up the log
				 * with pointless scariness!
				 */

				// log.debug("failed to extract current URI port", e);
			}
			if (secPort == null || secPort.intValue() == -1)
				return ub.scheme(SECURE_SCHEME);
			return ub.scheme(SECURE_SCHEME).port(secPort);
		}

		/**
		 * {@link UriBuilder} that applies a rewrite rule to the URIs produced
		 * by the wrapped builder.
		 * 
		 * @author Donal Fellows
		 */
		class RewritingUriBuilder extends UriBuilder {
			private UriBuilder wrapped;

			RewritingUriBuilder(UriBuilder builder) {
				wrapped = builder.clone();
			}

			private URI rewrite(URI uri) {
				return Rewriter.this.rewrite(uri.toString());
			}

			@Override
			public UriBuilder clone() {
				return new RewritingUriBuilder(wrapped);
			}

			@Override
			public URI buildFromMap(Map<String, ?> values)
					throws IllegalArgumentException, UriBuilderException {
				return rewrite(wrapped.buildFromMap(values));
			}

			@Override
			public URI buildFromEncodedMap(Map<String, ? extends Object> values)
					throws IllegalArgumentException, UriBuilderException {
				return rewrite(wrapped.buildFromEncodedMap(values));
			}

			@Override
			public URI build(Object... values) throws IllegalArgumentException,
					UriBuilderException {
				return rewrite(wrapped.build(values));
			}

			@Override
			public URI build(Object[] values, boolean encodeSlashInPath)
					throws IllegalArgumentException, UriBuilderException {
				return rewrite(wrapped.build(values, encodeSlashInPath));
			}

			@Override
			public URI buildFromEncoded(Object... values)
					throws IllegalArgumentException, UriBuilderException {
				return rewrite(wrapped.buildFromEncoded(values));
			}

			@Override
			public URI buildFromMap(Map<String, ?> values,
					boolean encodeSlashInPath) throws IllegalArgumentException,
					UriBuilderException {
				return rewrite(wrapped.buildFromEncoded(values,
						encodeSlashInPath));
			}

			@Override
			public UriBuilder uri(URI uri) throws IllegalArgumentException {
				wrapped.uri(uri);
				return this;
			}

			@Override
			public UriBuilder uri(String uriTemplate)
					throws IllegalArgumentException {
				wrapped.uri(uriTemplate);
				return this;
			}

			@Override
			public String toTemplate() {
				return wrapped.toTemplate();
			}

			@Override
			public UriBuilder scheme(String scheme)
					throws IllegalArgumentException {
				wrapped.scheme(scheme);
				return this;
			}

			@Override
			public UriBuilder schemeSpecificPart(String ssp)
					throws IllegalArgumentException {
				wrapped.schemeSpecificPart(ssp);
				return this;
			}

			@Override
			public UriBuilder userInfo(String ui) {
				wrapped.userInfo(ui);
				return this;
			}

			@Override
			public UriBuilder host(String host) throws IllegalArgumentException {
				wrapped.host(host);
				return this;
			}

			@Override
			public UriBuilder port(int port) throws IllegalArgumentException {
				wrapped.port(port);
				return this;
			}

			@Override
			public UriBuilder replacePath(String path) {
				wrapped.replacePath(path);
				return this;
			}

			@Override
			public UriBuilder path(String path) throws IllegalArgumentException {
				wrapped.path(path);
				return this;
			}

			@Override
			public UriBuilder path(
					@java.lang.SuppressWarnings("rawtypes") Class resource)
					throws IllegalArgumentException {
				wrapped.path(resource);
				return this;
			}

			@Override
			public UriBuilder path(
					@java.lang.SuppressWarnings("rawtypes") Class resource,
					String method) throws IllegalArgumentException {
				wrapped.path(resource, method);
				return this;
			}

			@Override
			public UriBuilder path(Method method)
					throws IllegalArgumentException {
				wrapped.path(method);
				return this;
			}

			@Override
			public UriBuilder segment(String... segments)
					throws IllegalArgumentException {
				wrapped.segment(segments);
				return this;
			}

			@Override
			public UriBuilder replaceMatrix(String matrix)
					throws IllegalArgumentException {
				wrapped.replaceMatrix(matrix);
				return this;
			}

			@Override
			public UriBuilder matrixParam(String name, Object... values)
					throws IllegalArgumentException {
				wrapped.matrixParam(name, values);
				return this;
			}

			@Override
			public UriBuilder replaceMatrixParam(String name, Object... values)
					throws IllegalArgumentException {
				wrapped.replaceMatrixParam(name, values);
				return this;
			}

			@Override
			public UriBuilder replaceQuery(String query)
					throws IllegalArgumentException {
				wrapped.replaceQuery(query);
				return this;
			}

			@Override
			public UriBuilder queryParam(String name, Object... values)
					throws IllegalArgumentException {
				wrapped.queryParam(name, values);
				return this;
			}

			@Override
			public UriBuilder replaceQueryParam(String name, Object... values)
					throws IllegalArgumentException {
				wrapped.replaceQueryParam(name, values);
				return this;
			}

			@Override
			public UriBuilder fragment(String fragment) {
				wrapped.fragment(fragment);
				return this;
			}
		}
	}
}