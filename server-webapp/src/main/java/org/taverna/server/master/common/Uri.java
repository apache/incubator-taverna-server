/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.Namespaces.XLINK;

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
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
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
	static final Log log = getLog("Taverna.Server.UriRewriter");
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
		this.ref = Rewriter.rewrite(ref);
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
		ref = Rewriter.getSecuredUriBuilder(ub).build((Object[]) strings);
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
			ub = Rewriter.getSecuredUriBuilder(ub);
		}
		ref = ub.path(path).build((Object[]) strings);
	}

	public static UriBuilder secure(UriBuilder ub) {
		return Rewriter.getSecuredUriBuilder(ub);
	}

	public static UriBuilder secure(UriInfo ui) {
		return Rewriter.getSecuredUriBuilder(ui.getAbsolutePathBuilder());
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

		@SuppressWarnings
		public Rewriter() {
			instance = this;
		}

		@PreDestroy
		@SuppressWarnings
		public void done() {
			instance = null;
		}

		@NonNull
		static URI rewrite(@NonNull URI uri) {
			return rewrite(uri.toString());
		}

		@NonNull
		static URI rewrite(@NonNull String url) {
			if (instance != null && instance.rewriteTarget != null)
				url = url.replaceFirst(instance.rewriteRE,
						instance.rewriteTarget);
			return URI.create(url);
		}

		/**
		 * {@link UriBuilder} that applies a rewrite rule to the URIs produced
		 * by the wrapped builder.
		 * 
		 * @author Donal Fellows
		 */
		static class RewritingBuilder extends UriBuilderImpl {
			private UriBuilder wrapped;

			RewritingBuilder(UriBuilder builder) {
				wrapped = builder;
			}

			@Override
			public UriBuilder clone() {
				return new RewritingBuilder(wrapped.clone());
			}

			@Override
			public URI buildFromMap(Map<String, ? extends Object> values)
					throws IllegalArgumentException, UriBuilderException {
				return Rewriter.rewrite(wrapped.buildFromMap(values));
			}

			@Override
			public URI buildFromEncodedMap(Map<String, ? extends Object> values)
					throws IllegalArgumentException, UriBuilderException {
				return Rewriter.rewrite(wrapped.buildFromEncodedMap(values));
			}

			@Override
			public URI build(Object... values) throws IllegalArgumentException,
					UriBuilderException {
				return Rewriter.rewrite(wrapped.build(values));
			}

			@Override
			public URI buildFromEncoded(Object... values)
					throws IllegalArgumentException, UriBuilderException {
				return Rewriter.rewrite(wrapped.buildFromEncoded(values));
			}
		}

		@NonNull
		public static UriBuilder getSecuredUriBuilder(@NonNull UriBuilder uribuilder) {
			UriBuilder ub = new RewritingBuilder(uribuilder.clone());
			if (instance != null && instance.suppress)
				return ub;
			Integer secPort = null;
			if (instance != null && instance.portMapper != null)
				try {
					secPort = instance.portMapper.lookupHttpsPort(ub.build()
							.getPort());
				} catch (Exception e) {
					/*
					 * Do not log this; we know why it happens and don't
					 * actually care to do anything about it. All it does is
					 * fill up the log with pointless scariness!
					 */

					// log.debug("failed to extract current URI port", e);
				}
			if (secPort == null || secPort.intValue() == -1)
				return ub.scheme(SECURE_SCHEME);
			return ub.scheme(SECURE_SCHEME).port(secPort);
		}
	}
}