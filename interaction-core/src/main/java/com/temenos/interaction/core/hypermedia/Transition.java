package com.temenos.interaction.core.hypermedia;

import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.temenos.interaction.core.hypermedia.expression.Expression;


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

public class Transition {

	/**
	 * Add transition to every item in collection
	 */
	public static final int FOR_EACH = 1;
	/**
	 * This transition is an auto transition.<br>
	 * This transition will result in the target state automatically being requested following
	 * the successful execution of the current state.
	 */
	public static final int AUTO = 2;
	/**
	 * Add a subresource
	 */
	public static final int EMBEDDED = 4;
	/**
	 * Add a transition used in an expression
	 */
	public static final int EXPRESSION = 8;
	/**
	 * This transition is a redirect transition.<br>
	 * A transition to this state from the same state as the auto target will result 
	 * in a 205 Reset Content HTTP status at runtime.
	 * A transition to this state from a different state to the auto target will result
	 * in a 303 Redirect HTTP status at runtime.
	 */
	public static final int REDIRECT = 16;
	
    /**
     * Add a sub resource to every item in collection
     */	
	public static final int FOR_EACH_EMBEDDED = 32;

	private ResourceState source, target;
	private ResourceLocator locator;
	private final TransitionCommandSpec command;
	private String label;

	// TransitionCommand parameters
	private String method;
	private int flags;
	// conditional link evaluation expression 
	private Expression evaluation;
	private Map<String, String> uriParameters;

	private String linkId;
	
	public String getLinkId() {
		return linkId;
	}
	
	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}
	
	public ResourceState getSource() {
		return source;
	}

	public void setSource(ResourceState source) {
		this.source = source;
	}
	
	public ResourceState getTarget() {
		return target;
	}

	public void setTarget(ResourceState target) {
		this.target = target;
	}

	public TransitionCommandSpec getCommand() {
		return command;
	}

	public String getLabel() {
		return label;
	}

	public String getId() {
		String labelText = "";
		if (label != null && !label.equals((target != null ? target.getName() : ""))) {
			labelText = "(" + label + ")";
		}
		if (source == null) {
			return target.getId() + ">" + command.getMethod() + labelText + ">"
					+ target.getId(); //transition to itself
		} else {
			return source.getId() + ">" + command.getMethod() + labelText + ">"
					+ (target != null ? target.getId() : "");
		}
	}

	/**
	 * Indicates whether this is a GET transition from a collection resource state
	 * to an entity resource state within the same entity.
	 * @return true/false
	 */
	public boolean isGetFromCollectionToEntityResource() {
		return source != null
				&& command.getMethod() != null
				&& command.getMethod().equals("GET")
				&& source.getEntityName().equals(target.getEntityName())
				&& source instanceof CollectionResourceState
				&& (target instanceof ResourceState && !(target instanceof CollectionResourceState));
	}
	
	/**
	 * @return the locator
	 */
	public ResourceLocator getLocator() {
		return locator;
	}

	/**
	 * @param locator the locator to set
	 */
	public void setLocator(ResourceLocator locator) {
		this.locator = locator;
	}	

	public boolean equals(Object other) {
		//check for self-comparison
		if (this == other)
			return true;
		if (!(other instanceof Transition))
			return false;
		Transition otherTrans = (Transition) other;
		
		// Don't compare transitions to avoid recursion
		return isSameStateName(source, otherTrans.source)
				&& isSameStateName(target, otherTrans.target)
				&& StringUtils.equals(label, otherTrans.label)
				&& ObjectUtils.equals(command, otherTrans.command)
				&& StringUtils.equals(linkId, otherTrans.linkId);
	}

	private static boolean isSameStateName(ResourceState state1, ResourceState state2) {
		boolean same = false;
		
		if(state1 == null && state2 == null) {
			same = true;
		} else {
			if(state1 != null && state2 != null) {
				same = StringUtils.equals(state1.getName(), state2.getName());
			}
		}
		
		return same;
	}
	
	public boolean isType(int type) {
		return (command.getFlags() & type) == type;
	}
	
	public int hashCode() {
		return (source != null ? source.getName().hashCode() : 0)
				+ (target != null ? target.getName().hashCode() : 0)
				+ (label != null ? label.hashCode() : 0) 
				+ (command != null ? command.hashCode() : 0)
				+ (linkId != null ? linkId.hashCode() : 0);
	}

	public String toString() {
		return getId();
	}

	/*
	 * Builder pattern generated with fastcode eclipse plugin, you can just regenerate this part
	 */

	public static class Builder {
		private ResourceState source;
		private ResourceState target;
		private ResourceLocator locator;
		private String label;
		private String method;
		private int flags;
		private Expression evaluation;
		private Map<String, String> uriParameters;
		private String linkId;

		public Builder source(ResourceState source) {
			this.source = source;
			return this;
		}

		public Builder target(ResourceState target) {
			this.target = target;
			return this;
		}

		public Builder locator(ResourceLocator locator) {
			this.locator = locator;
			return this;
		}

		public Builder label(String label) {
			this.label = label;
			return this;
		}

		public Builder method(String method) {
			this.method = method;
			return this;
		}

		public Builder flags(int flags) {
			this.flags = flags;
			return this;
		}

		public Builder evaluation(Expression evaluation) {
			this.evaluation = evaluation;
			return this;
		}

		public Builder uriParameters(Map<String, String> uriParameters) {
			this.uriParameters = uriParameters;
			return this;
		}

		public Builder linkId(String linkId) {
			this.linkId = linkId;
			return this;
		}

		public Transition build() {
			return new Transition(this);
		}
	}

	private Transition(Builder builder) {
		this.source = builder.source;
		this.target = builder.target;
		this.locator = builder.locator;
		this.label = builder.label;
		this.method = builder.method;
		this.flags = builder.flags;
		this.evaluation = builder.evaluation;
		this.uriParameters = builder.uriParameters;
		this.linkId = builder.linkId;

		// this one's a bit special
		this.command = new TransitionCommandSpec(method,
				flags, evaluation, uriParameters, linkId);
	}	
	
}
