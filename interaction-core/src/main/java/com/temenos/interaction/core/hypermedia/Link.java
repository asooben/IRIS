package com.temenos.interaction.core.hypermedia;

/*
 * #%L
 * interaction-core
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MultivaluedMap;

/**
 * A Link (see http://tools.ietf.org/html/rfc5988)
 * @author aphethean
 */
public class Link {

	private Transition transition;

	// id
	private String id;
	// title (5.4 Target Attributes)
	private String title;
	// rel (5.3 Relation Type)
	private String rel;
	// href (5.1 Target IRI)
	private String href;
	// type (5.4 Target Attributes)
	private String[] produces;

	// extensions
	private String method;
	private String[] consumes;
	private MultivaluedMap<String, String> extensions;
	
	//LinkId
	private String linkId;
	
	//Source entity value
	private String sourceEntityValue;

	/**
	 * Construct a simple link used for GET operations.
	 * @param title
	 * @param rel
	 * @param href
	 * @param type
	 * @param extensions
	 */
	public Link(String title, String rel, String href, String type,
			MultivaluedMap<String, String> extensions) {
		this(null, title, rel, href, null, (type != null ? type.split(" ")
				: null), HttpMethod.GET, null);
	}

	/**
	 * Construct a link from a transition.
	 * @param transition
	 */
	public Link(Transition transition, String rel, String href, String method) {
		this(transition, transition.getLabel() != null
				&& !transition.getLabel().equals("") ? transition.getLabel()
				: transition.getTarget().getName(), rel, href, null, null,
				method, null);
	}
	
	/**
     * Construct a link from a transition having a uri with a multivalue drill-down
     * @param transition
     */
    public Link(Transition transition, String rel, String href, String method, String sourceEntityValue) {
        this(transition, transition.getLabel() != null
                && !transition.getLabel().equals("") ? transition.getLabel()
                : transition.getTarget().getName(), rel, href, null, null,
                method, null, sourceEntityValue);
    }

	public Link(Transition transition, String title, String rel, String href,
			String[] consumes, String[] produces, String method,
			MultivaluedMap<String, String> extensions) {		
	    this(transition, title, rel, href, consumes, produces, method, extensions, null);
	}
	
	public Link(Transition transition, String title, String rel, String href,
            String[] consumes, String[] produces, String method,
            MultivaluedMap<String, String> extensions, String sourceEntityValue) {
        this.transition = transition;
        if (title == null && transition != null) {
            title = transition.getId();
        }
        id = transition != null ? transition.getId() : title;
        this.title = title;
        this.rel = rel;
        this.href = href;
        this.consumes = consumes;
        this.produces = produces;
        this.method = method;
        this.extensions = extensions;
        this.linkId = transition != null ? transition.getLinkId() : null;
        this.sourceEntityValue = sourceEntityValue;
    }

	public Transition getTransition() {
		return transition;
	}

	public String getId() {
		return id;
	}

	public String getRel() {
		return rel;
	}

	public String getMethod() {
		return method;
	}

	public String getHref() {
		return href;
	}
	
    public String getSourceEntityValue() {
        return sourceEntityValue;
    }
    
    public String getLinkId() {
		if(linkId == null) {
			linkId = transition != null ? transition.getLinkId() : null;	
		} 
		return linkId;
	}
	/**
	 * Obtain the transition, i.e. the link relative to the REST service.
	 * 
	 * @param basePath  Path to REST service
	 * @return relativePath of transition relative to REST service 
	 */
	public String getRelativeHref(String basePath) {
		String baseUri = HypermediaTemplateHelper.getTemplatedBaseUri(basePath, href);
		StringBuffer regex = new StringBuffer("(?<=" + baseUri + ")\\S+");
		Pattern p = Pattern.compile(regex.toString());
		Matcher m = p.matcher(href);
		while (m.find()) {
			return m.group();
		}
		return href;
	}

	public String[] getConsumes() {
		return consumes;
	}

	public String[] getProduces() {
		return produces;
	}

	public String getTitle() {
		return title;
	}

	public MultivaluedMap<String, String> getExtensions() {
		return extensions;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("<");
		buf.append(href).append(">");
		if (rel != null) {
			buf.append("; rel=\"").append(rel).append("\"");
		}
		if (produces != null && produces.length > 0) {
			buf.append("; type=\"").append(produces[0]);
			for (int i = 1; i < produces.length; i++)
				buf.append(",").append(produces[i]);
			buf.append("\"");
		}
		if (title != null) {
			buf.append("; title=\"").append(title).append("\"");
		}
		if (extensions != null) {
			for (String key : getExtensions().keySet()) {
				List<String> values = getExtensions().get(key);
				for (String val : values) {
					buf.append("; ").append(key).append("=\"").append(val)
							.append("\"");
				}
			}
		}
		
		if (linkId != null) {
			buf.append("; linkId=\"").append(linkId).append("\"");
		}
		
		if (sourceEntityValue != null) {
            buf.append("; sourceEntityValue=\"").append(sourceEntityValue).append("\"");
        }
		return buf.toString();
	}

	public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
		if (other instanceof Link) {
			Link otherLink = (Link) other;
			return this.toString().equals(otherLink.toString());
		}
		return false;
	}
	
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public static class Builder {
		private Transition transition;
		private String id;
		private String title;
		private String rel;
		private String href;
		private String[] produces;
		private String method;
		private String[] consumes;
		private MultivaluedMap<String, String> extensions;
		private String linkId;
		private String sourceEntityValue;

		public Builder transition(Transition transition) {
			this.transition = transition;
			return this;
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder rel(String rel) {
			this.rel = rel;
			return this;
		}

		public Builder href(String href) {
			this.href = href;
			return this;
		}

		public Builder produces(String[] produces) {
			this.produces = produces;
			return this;
		}

		public Builder method(String method) {
			this.method = method;
			return this;
		}

		public Builder consumes(String[] consumes) {
			this.consumes = consumes;
			return this;
		}

		public Builder extensions(MultivaluedMap<String, String> extensions) {
			this.extensions = extensions;
			return this;
		}

		public Builder linkId(String linkId) {
			this.linkId = linkId;
			return this;
		}
		
		public Builder sourceEntityValue(String sourceEntityValue)
		{
		    this.sourceEntityValue = sourceEntityValue;
		    return this;
		}

		public Link build() {
			return new Link(this);
		}
	}

	private Link(Builder builder) {
		this.transition = builder.transition;
		this.id = builder.id;
		this.title = builder.title;
		this.rel = builder.rel;
		this.href = builder.href;
		this.produces = builder.produces;
		this.method = builder.method;
		this.consumes = builder.consumes;
		this.extensions = builder.extensions;
		if(builder.linkId == null) {
			this.linkId = builder.transition != null ? builder.transition.getLinkId() : null;	
		} else {
			this.linkId = builder.linkId;
		}
		this.sourceEntityValue = builder.sourceEntityValue;		
	}
}
