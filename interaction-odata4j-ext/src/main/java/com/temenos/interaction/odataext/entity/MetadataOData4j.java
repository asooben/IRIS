package com.temenos.interaction.odataext.entity;

/*
 * #%L
 * interaction-odata4j-ext
 * %%
 * Copyright (C) 2012 - 2014 Temenos Holdings N.V.
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.StringUtils;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.PrefixedNamespace;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDataServices.Builder;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.terms.AbstractOdataAnnotation;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexGroup;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermIdField;
import com.temenos.interaction.core.entity.vocabulary.terms.TermListType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermRestriction;
import com.temenos.interaction.core.entity.vocabulary.terms.TermRestriction.Restriction;
import com.temenos.interaction.core.entity.vocabulary.terms.TermSemanticType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.core.hypermedia.CollectionResourceState;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.odataext.ODataHelper;


/**
 * This class converts a EntityMetadata structure to odata4j's EdmDataServices.
 * <p />
 * This class is responsible for providing EdmMetadata for all the resources we are working on.
 * To achieve this it will convert Resource/Entities present within ServiceDocument upon creation
 * and save it as EdmDataServices and load non-ServiceDocument resource on demand at runtime and 
 * maintain in its local cache
 * 
 * Note:
 *  IRIS expects the following naming conventions
 *  
 * 	Entity name: 	Car
 *  Entity type: 	Car
 *  Entity set: 	Cars
 */
public class MetadataOData4j {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataOData4j.class);

	private static final String MULTI_NAV_PROP_TO_ENTITY = "MULTI_NAV_PROP";

	private ConcurrentMap<String, EdmEntitySet> nonSrvDocEdmEntitySetMap;
	private ConcurrentMap<String, EdmComplexType> nonSrvDocEdmComplexTypeMap;
	private Metadata metadata;
	private ResourceStateMachine hypermediaEngine;
	private ResourceState serviceDocument;
	private String SERVICE_DOCUMENT = "ServiceDocument";
	private ODataVersion odataVersion = ODataVersion.V1;
	private EdmDataServicesAdapter edmDataServicesAdapter;
	private EdmDataServices edmDataServices;

	/**
	 * Construct the odata metadata ({@link EdmDataServices}) by looking up a resource 
	 * called 'ServiceDocument' and add an EntitySet to the metadata for any collection
	 * resource with a transition from this 'ServiceDocument' resource.
	 * @param metadata metadata
	 * @param hypermediaEngine containing ResourceStateMachine with ServiceDocument as initial state only
	 */
	public MetadataOData4j(Metadata metadata, ResourceStateMachine hypermediaEngine) {
		serviceDocument = hypermediaEngine.getResourceStateByName(SERVICE_DOCUMENT);
		if (serviceDocument == null)
			throw new RuntimeException("No "+  SERVICE_DOCUMENT + " found.");

		if (serviceDocument instanceof CollectionResourceState)
			throw new RuntimeException("Initial state must be an individual resource state");

		this.metadata = metadata;
		this.hypermediaEngine = hypermediaEngine;
		this.nonSrvDocEdmEntitySetMap= new ConcurrentHashMap<String, EdmEntitySet>(); 
		this.nonSrvDocEdmComplexTypeMap = new ConcurrentHashMap<String, EdmComplexType>();
	}

	/**
	 * @return the odataVersion
	 */
	public ODataVersion getOdataVersion() {
		return odataVersion;
	}

	/**
	 * Change the version of OData that will be used. OData V2 is needed to permit annotations
	 * (used for Semantic Types)
	 * @param odataVersion the odataVersion to set.
	 */
	public void setOdataVersion(ODataVersion odataVersion) {
		LOGGER.debug("OData Version set to {}", odataVersion);
		this.odataVersion = odataVersion;
	}
	
	/**
	 * Returns all metadata - i.e. including meta data relating to resources not in the service document
	 * 
	 * @return
	 */
	public EdmDataServices getMetadata() {
		if (edmDataServicesAdapter == null) {
			edmDataServicesAdapter = new EdmDataServicesAdapter(this);
		}
		
		return edmDataServicesAdapter;
	}

	/**
	 * Returns EDM metadata ONLY - i.e. only meta data relating to resources in the service document
	 * @return edmdataservices object
	 */	
	EdmDataServices getEdmMetadata() {
		EdmDataServices result = null;
		
		synchronized(this) {
			if(edmDataServices == null) {
			    try {
			        edmDataServices = createOData4jMetadata(metadata, hypermediaEngine, serviceDocument);
			    } catch (Exception e) {
			        LOGGER.error("Error creating odata4j metadata for resources in service document", e);
			    }
			}
			
			result = edmDataServices;
		}
		
		return result;		
	}
	
	public EdmComplexType findEdmComplexType(String typeName) {
		EdmComplexType result = null;
	        try {
	            result = getEdmMetadata().findEdmComplexType(typeName);
	        } catch(Exception e) {
	            LOGGER.error("Error getting EDM entity complex type" + typeName, e);
	        }

		
		if(result == null && nonSrvDocEdmComplexTypeMap.containsKey(typeName)) {
			// Check if the type is a complex type in non service document meta data
			result = (EdmComplexType)nonSrvDocEdmComplexTypeMap.get(typeName);
		}
		
		if(result == null) {
			// Check if the type is non service document meta data  
			String tmpTypeName = typeName.substring(typeName.indexOf(".") + 1, typeName.indexOf("_"));
			
			EdmEntitySet edmEntitySet = getEdmEntitySetFromNonSrvDocResrc(getEdmEntitySetName(tmpTypeName));
			
			if(edmEntitySet != null) {
				result = (EdmComplexType)nonSrvDocEdmComplexTypeMap.get(typeName);
			}
		}
		
		return result;
	}
	
	
	public EdmType getEdmEntityTypeByTypeName(String typeName) {
	    EdmType result = null;
	    try {
	        // Check if type is in the service document / EDM meta data
	        result = (EdmEntityType)getEdmMetadata().findEdmEntityType(typeName);
	    } catch(Exception e) {
	        LOGGER.error("Error getting EDM entity type" + typeName, e);
	    }
	    
        if(result == null && nonSrvDocEdmComplexTypeMap.containsKey(typeName)) {
        	// Check if the type is a complex type in non service document meta data
            result = nonSrvDocEdmComplexTypeMap.get(typeName);
        }		
		
		if(result == null) {
			// Check if the type is non service document meta data  
			String tmpTypeName = typeName.substring(typeName.indexOf(".") + 1);
			EdmEntitySet edmEntitySet = getEdmEntitySetFromNonSrvDocResrc(getEdmEntitySetName(tmpTypeName));
			
			if(edmEntitySet != null) {
				try {
					result = edmEntitySet.getType();
				} catch(IllegalArgumentException e) {
					// Expected for cases where entity type does not have keys i.e. ServiceDocument, Metadata, etc..
					LOGGER.debug("Failed to get type for {}", typeName, e);
				}
			}
		}
				
		return result;
	}
	
	public EdmEntitySet getEdmEntitySetByType(EdmEntityType type) {
		EdmEntitySet edmEntitySet = null;
		try {
			edmEntitySet = getEdmMetadata().getEdmEntitySet(type);
		} catch (Exception e) {
			// Ignore.... as we will be try lazy loading after this
			LOGGER.debug("EntitySet for [{}] not found in EdmDataServices, try loading it seperately...", type, e);
		}
		// If its null
		if (edmEntitySet == null) {
			// Let's check if we have it in non-service doc resources
			edmEntitySet = getEdmEntitySetFromNonSrvDocResrc(getEdmEntitySetName(type.getName()));
		}
		
		return edmEntitySet;
		
	}
	
	/**
	 * required by GetEntitiesCommand
	 * @param entityName
	 * @return EdmEntitySet
	 * 
	 */
	public EdmEntitySet getEdmEntitySetByEntityName(String entityName) {
		EdmEntitySet edmEntitySet = null;
		try {
			edmEntitySet = ODataHelper.getEntitySet(entityName, getEdmMetadata());
		} catch (Exception e) {
			// Ignore.... as we will be try lazy loading after this
			LOGGER.debug("EntitySet for [{}] not found in EdmDataServices, try loading it seperately...", entityName, e);
		}
		// If its null
		if (edmEntitySet == null) {
			// Let's check if we have it in non-service doc resources
			edmEntitySet = getEdmEntitySetFromNonSrvDocResrc(getEdmEntitySetName(entityName));
		}
		return edmEntitySet;
	}
	
	/**
	 * 
	 * @param entityName
	 */
	public void unloadMetadata(String entityName) {		
		String entitySetName = getEdmEntitySetName(entityName);
		
		if(nonSrvDocEdmEntitySetMap.containsKey(entitySetName)) {
			// Non service document resource - Remove nonSrvDocEdmEntitySetMap entry so that it's meta data cannot be referenced
			nonSrvDocEdmEntitySetMap.remove(entitySetName);
		} else {
			/* This may be a service document resource, if it is - Unload the internal reference to the real EDM data services 
			 * so that it is completely rebuilt on the next request to it
			 */
			synchronized (this) {
				if (edmDataServices != null) {
					// EDM data services has already been initialized

					if (edmDataServices.findEdmEntitySet(entitySetName) != null) {
						// EDM data services, i.e. service document, contains entity set therefore it needs to be rebuilt
						edmDataServices = null;
					}
				}
			}
		}
	}

	/**
	 * required by producer
	 * @param entityName
	 * @return EdmEntitySet
	 * 
	 */
	public EdmEntitySet getEdmEntitySetByEntitySetName(String entitySetName) {
		EdmEntitySet edmEntitySet = getEdmEntitySetFromEdmDataServices(entitySetName);
		if (edmEntitySet == null ) {
			// this means the Entity is not part of ServiceDocument...lets load it
			edmEntitySet = getEdmEntitySetFromNonSrvDocResrc(entitySetName);
			// If still not found then we have to give up
			if (edmEntitySet == null ) 
				throw new NotFoundException("Fail to find/load Entity Set for [" + entitySetName + "]");
		}
		return edmEntitySet;
	}
	
	/**
	 * Method to return EdmEntitySet from nonSrvDoc Map
	 * @param entitySetName
	 * @return
	 */
	private EdmEntitySet getEdmEntitySetFromNonSrvDocResrc(String entitySetName) {
		// Find if we already have loaded this resource before
		if (nonSrvDocEdmEntitySetMap.get(entitySetName) != null)
			return nonSrvDocEdmEntitySetMap.get(entitySetName);
		// Try to load 
		return loadEdmEntitySetFromEntityName(getEntityName(entitySetName));
	}
	
	/**
	 * Method to load EdmEntitySet if not loaded as yet
	 * @param entityName
	 * @return
	 */
	private EdmEntitySet loadEdmEntitySetFromEntityName(String entityName) {
		// Let's get the metadata for the resource
		EntityMetadata entityMetadata = metadata.getEntityMetadata(entityName);
		if (entityMetadata == null) 
			throw new NotFoundException("Fail to find/load Entity Metadata for [" + entityName + "]");
		// Lets build the EdmEntitySet form EntityMetadata
		Map<String, EdmComplexType.Builder> complexTypes = new HashMap<String, EdmComplexType.Builder>();
		EdmEntityType.Builder entityType = getEdmTypeBuilder(entityMetadata, complexTypes, false);
		
		if (entityType != null) {
			
			EdmEntitySet.Builder bEntitySetBuilder = EdmEntitySet.newBuilder().setName(getEdmEntitySetName(entityName)).setEntityType(entityType);
			EdmEntitySet edmEntitySet = bEntitySetBuilder.build();
			
			for(Map.Entry<String, EdmComplexType.Builder> entry: complexTypes.entrySet()) {
				this.nonSrvDocEdmComplexTypeMap.put(entry.getKey(), entry.getValue().build());
			}
			
			// Append to the map
			nonSrvDocEdmEntitySetMap.put(getEdmEntitySetName(entityName), edmEntitySet);
			
			return edmEntitySet;
		} 
		return null;
	}
	
	/**
	 * Method to search EdmEntitySet within EdmDataServices
	 * @param entitySetName
	 * @return
	 */
	private EdmEntitySet getEdmEntitySetFromEdmDataServices(String entitySetName) {
		EdmEntitySet ees = null;
		
		try {
			ees = getEdmMetadata().findEdmEntitySet(entitySetName);
		} catch (Exception e) {
			LOGGER.warn("Failed to find EDM entity set", e);
		}
		
		return ees;
	}
	
	/**
	 * Create EDM metadata from Resource State Machine containing ServiceDocument
	 * @param metadata
	 * @param hypermediaEngine
	 * @param serviceDocument
	 * @return
	 */
	public EdmDataServices createOData4jMetadata(Metadata metadata, ResourceStateMachine hypermediaEngine, ResourceState serviceDocument) {
		Builder mdBuilder = EdmDataServices.newBuilder();
		
		mdBuilder.setVersion(odataVersion);
		LOGGER.info("Using OData version {}", odataVersion);
		if (odataVersion==ODataVersion.V2)
			mdBuilder.addNamespaces(Collections.singletonList(new PrefixedNamespace(AbstractOdataAnnotation.NAMESPACE, AbstractOdataAnnotation.PREFIX)));
		
		List<EdmSchema.Builder> bSchemas = new ArrayList<EdmSchema.Builder>();
		EdmSchema.Builder bSchema = new EdmSchema.Builder();
		List<EdmEntityContainer.Builder> bEntityContainers = new ArrayList<EdmEntityContainer.Builder>();
		Map<String, EdmEntityType.Builder> bEntityTypeMap = new HashMap<String, EdmEntityType.Builder>();
		Map<String, EdmComplexType.Builder> bComplexTypeMap = new HashMap<String, EdmComplexType.Builder>();
		Map<String, EdmEntitySet.Builder> bEntitySetMap = new HashMap<String, EdmEntitySet.Builder>();
		Map<String, EdmFunctionImport.Builder> bFunctionImportMap = new HashMap<String, EdmFunctionImport.Builder>();
		List<EdmAssociation.Builder> bAssociations = new ArrayList<EdmAssociation.Builder>();
		
		// Process meta data present in the service document
		for (ResourceState state : hypermediaEngine.getStates()) {
			// Skip Service Document
			if (serviceDocument.equals(state))
				continue;
			
			EntityMetadata entityMetadata = null;
			try {
			    entityMetadata = metadata.getEntityMetadata(state.getEntityName());
			} catch (Exception e) {
	             LOGGER.warn("Failed to get metadata for state name '{}' / entity name '{}'", state.getName(), state.getEntityName(), e);
	             continue;
			}
			
			if(entityMetadata == null) {
			    LOGGER.warn("Failed to get metadata for state name '{}' / entity name '{}'", state.getName(), state.getEntityName());
			    continue;
			}
			
			// Always strictKeyCheck here because we will be building EdmDataServices from this
			EdmEntityType.Builder bEntityType = getEdmTypeBuilder(entityMetadata, bComplexTypeMap, true);
			if (bEntityType != null) {
				bEntityTypeMap.put(state.getEntityName(), bEntityType);					
			} else {
				LOGGER.warn("Entity name '{}' does not have type. entityMetadata={}", state.getEntityName(), entityMetadata);
			}
		}
		
		//The model name is available after processing the states, i.e. the namespace should be created afterwards
        String serviceName = metadata.getModelName();
        String namespace = serviceName + Metadata.MODEL_SUFFIX;
					
		// Add Navigation Properties		
		// build associations		
		Map<EdmEntityType.Builder, Map<String, EdmAssociation.Builder>> entityTypeToStateAssociations = new HashMap<EdmEntityType.Builder, Map<String, EdmAssociation.Builder>>();
		
		for (EdmEntityType.Builder bEntityType : bEntityTypeMap.values()) {
			Map<String, EdmAssociation.Builder> bAssociationMap = buildAssociations(namespace, bEntityType, bEntityTypeMap, hypermediaEngine, serviceDocument);
			entityTypeToStateAssociations.put(bEntityType, bAssociationMap);
			bAssociations.addAll(bAssociationMap.values());
		}
		
		Map<String,String> multiAssociation = new HashMap<String,String>();
		int multipleAssoc = 0;
		
		for (ResourceState source : hypermediaEngine.getStates()) {
			for (ResourceState target : source.getAllTargets()) {
				Collection<Transition> entityTransitions = source.getTransitions(target);
				if (entityTransitions != null) {
					for(Transition entityTransition : entityTransitions) {
						ResourceState sourceState = entityTransition.getSource();
						ResourceState targetState = entityTransition.getTarget();
						
						EdmEntityType.Builder bEntityType = bEntityTypeMap.get(sourceState.getEntityName());

						if (bEntityType != null 
								&& !entityTransition.getTarget().isPseudoState()
								&& !entityTransition.getTarget().equals(serviceDocument)
								&& !(entityTransition.getSource() instanceof CollectionResourceState)) {
							//We can have more than one navigation property for the same association
							String navPropertyName = targetState.getName();
							//We can have transitions to a resource state from multiple source states
							if (entityTransition.getLabel() != null) {
								navPropertyName = entityTransition.getLabel();
							} else if (multiAssociation.get(navPropertyName) != null) {
								navPropertyName = navPropertyName + "_" + multipleAssoc++;
							}
							multiAssociation.put(navPropertyName, targetState.getName());
							
							Map<String, EdmAssociation.Builder> bAssociationMap = entityTypeToStateAssociations.get(bEntityType);
							
							EdmAssociation.Builder relationship = bAssociationMap.get(targetState.getName());
							bEntityType.addNavigationProperties(EdmNavigationProperty
									.newBuilder(navPropertyName)
									.setRelationship(relationship)
									.setFromTo(relationship.getEnd1(), relationship.getEnd2()));
						}
					}
				}
			}
			
			if (source instanceof CollectionResourceState) {
				// Index EntitySets by Entity name
				EdmEntityType.Builder entityType = bEntityTypeMap.get(source.getEntityName());
				
				if (entityType == null) { 
					LOGGER.warn("Entity type not found for {}" + source.getEntityName());
				    continue;
				}
				
				Transition fromInitialState = serviceDocument.getTransition(source);
				EdmEntitySet.Builder bEntitySet = EdmEntitySet.newBuilder().setName(source.getName()).setEntityType(entityType);
				if (fromInitialState != null) {
					// Add entity set
					bEntitySetMap.put(source.getEntityName(), bEntitySet);
				} else {
					LOGGER.debug("Not adding entity set [{}] to metadata, no transition from initial state [{}]", source.getName(), serviceDocument.getName());
				}
			}			
		}
				
		Collection<ResourceState> allTargets = hypermediaEngine.getStates();
		
		for (ResourceState state : allTargets) {
			if (state instanceof CollectionResourceState) {
				Transition fromInitialState = serviceDocument.getTransition(state);
				if (fromInitialState == null) {
					EdmEntitySet.Builder bEntitySet = bEntitySetMap.get(state.getEntityName());
					
					if(bEntitySet == null) {
					    LOGGER.warn("Failed to find entity set for entity {}", state.getEntityName());
					} else {
    					// Add Function
    					EdmFunctionImport.Builder bFunctionImport = EdmFunctionImport.newBuilder()
    							.setName(state.getName())
    							.setEntitySet(bEntitySet)
    							.setHttpMethod(HttpMethod.GET)
    							.setIsCollection(true)
    							.setReturnType(bEntityTypeMap.get(state.getEntityName()));
    					bFunctionImportMap.put(state.getName(), bFunctionImport);
					}
				}
			}
		}

		EdmEntityContainer.Builder bEntityContainer = EdmEntityContainer.newBuilder()
				.setName(serviceName)
				.setIsDefault(true)
				.addEntitySets(new ArrayList<EdmEntitySet.Builder>(bEntitySetMap.values()))
				.addFunctionImports(new ArrayList<EdmFunctionImport.Builder>(bFunctionImportMap.values()));
		bEntityContainers.add(bEntityContainer);

		List<EdmEntityType.Builder> bEntityTypes = new ArrayList<EdmEntityType.Builder>();
		bEntityTypes.addAll(bEntityTypeMap.values());

		List<EdmComplexType.Builder> bComplexTypes = new ArrayList<EdmComplexType.Builder>();
		bComplexTypes.addAll(bComplexTypeMap.values());

		bSchema
		.setNamespace(namespace)
		.setAlias(serviceName)
		.addEntityTypes(bEntityTypes)
		.addComplexTypes(bComplexTypes)
		.addAssociations(bAssociations)
		.addEntityContainers(bEntityContainers);
		bSchemas.add(bSchema);

		mdBuilder.addSchemas(bSchemas);

		//Build the EDM metadata
		return mdBuilder.build();
	}

	/**
	 * Method to generate EdmEntityType.Builder from EntityMetadata
	 * @param entityMetadata
	 * @return
	 */
	private EdmEntityType.Builder getEdmTypeBuilder(EntityMetadata entityMetadata, Map<String, EdmComplexType.Builder> bComplexTypeMap, boolean strictKeyCheck) {
		String namespace = metadata.getModelName() + Metadata.MODEL_SUFFIX;
		bComplexTypeMap = bComplexTypeMap == null ? new HashMap<String, EdmComplexType.Builder>() : bComplexTypeMap;
		List<EdmProperty.Builder> bProperties = new ArrayList<EdmProperty.Builder>();
		List<String> keys = new ArrayList<String>();
		
		// Build Annotations for EntityType for Odata clients to find out which properties are FlterOnly and DisplayOnly
		// So for clients; 
		//		DisplayProperties 	= AllProperties - FilterOnlyProperties
		//		FilterProperties 	= AllProperties - DisplayOnlyProperties
		List<String> displayOnlyProps = new ArrayList<String>();
		List<String> filterOnlyProps = new ArrayList<String>();
		
		String complexTypePrefix = new StringBuilder(entityMetadata.getEntityName()).append("_").toString();
		for(String propertyName : entityMetadata.getPropertyVocabularyKeySet()) {
			LOGGER.debug("EdmTypeBuilder[{}] - {}", entityMetadata.getEntityName(), propertyName);
			//Entity properties, lets gather some information about the property
			String termComplex = entityMetadata.getTermValue(propertyName, TermComplexType.TERM_NAME);							// Is vocabulary a group (Complex Type)
			boolean termList = Boolean.parseBoolean(entityMetadata.getTermValue(propertyName, TermListType.TERM_NAME));	// Is vocabulary a List of (Complex Types)
			String termComplexGroup = entityMetadata.getTermValue(propertyName, TermComplexGroup.TERM_NAME);					// Is vocabulary belongs to a group (ComplexType) 
			boolean isNullable = entityMetadata.isPropertyNullable(propertyName);
			if (termComplex.equals("false")) {
				// This means we are dealing with plain property, either belongs to Entity or ComplexType (decide later, lets build it first)
				EdmType edmType = termValueToEdmType(entityMetadata.getTermValue(propertyName, TermValueType.TERM_NAME));
				EdmProperty.Builder ep = EdmProperty.newBuilder(entityMetadata.getSimplePropertyName(propertyName)).
						setType(edmType).
						setNullable(isNullable);

				// Adding Semantic Type
				if (annotationAllowed()) {
					// Add an annotation if a semantic type is defined for the property
					List<EdmAnnotation<?>> annotations = new LinkedList<EdmAnnotation<?>>();
					String semanticType = entityMetadata.getTermValue(propertyName, TermSemanticType.TERM_NAME);
					if (semanticType != null)
						annotations.add((EdmAnnotation.attribute(TermSemanticType.NAMESPACE, TermSemanticType.PREFIX, TermSemanticType.CSDL_NAME, semanticType)));
					ep.setAnnotations(annotations);
				}

				if (termComplexGroup == null) {
					if (annotationAllowed()) {
						if (entityMetadata.isPropertyDisplayOnly(propertyName))
							displayOnlyProps.add(propertyName);
						else if (entityMetadata.isPropertyFilterOnly(propertyName))
							filterOnlyProps.add(propertyName);
					}
					// Property belongs to an Entity Type, simply add it
					bProperties.add(ep);
				} else {
					// Property belongs to a group (complex type), first make sure we have a group 
					// so add a group with Entity name space and group name
					addComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), bComplexTypeMap);
					// And then add the property into complex type
					String complexTypeName = new StringBuilder(complexTypePrefix).append(entityMetadata.getSimplePropertyName(termComplexGroup)).toString();
					addPropertyToComplexType(namespace, complexTypeName, ep, bComplexTypeMap);
					if (annotationAllowed()) {
						String fullyQualifiedName = new StringBuilder(complexTypeName).append(".").append(entityMetadata.getSimplePropertyName(propertyName)).toString();
						if (entityMetadata.isPropertyDisplayOnly(propertyName))
							displayOnlyProps.add(fullyQualifiedName);
						else if (entityMetadata.isPropertyFilterOnly(propertyName))
							filterOnlyProps.add(fullyQualifiedName);
					}
				}
			} else {
				// This means vocabulary is a group (complex type), so add it in a map
				String complexPropertyName = complexTypePrefix + entityMetadata.getSimplePropertyName(propertyName);
				addComplexType(namespace, complexPropertyName, bComplexTypeMap);
				if (termComplexGroup != null) {
					// This mean group (complex type) belongs to a group (complex type), so make sure add the parent group and add
					// nested group as group property
					addComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), bComplexTypeMap);
					addComplexTypeToComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), complexPropertyName, isNullable, termList, bComplexTypeMap);
				} else {
					// This means group (complex type) belongs to an Entity, so simply build and add as a Entity prop
					EdmProperty.Builder ep;
					if (termList) {
						ep = EdmProperty.newBuilder(complexPropertyName).
								setType(bComplexTypeMap.get(namespace + "." + complexPropertyName)).
								setCollectionKind(CollectionKind.Bag).								
								setNullable(isNullable);
					} else {
						ep = EdmProperty.newBuilder(complexPropertyName).
								setType(bComplexTypeMap.get(namespace + "." + complexPropertyName)).
								setNullable(isNullable);
					}
					bProperties.add(ep);
				}
			}	

			//Entity keys
			if(entityMetadata.getTermValue(propertyName, TermIdField.TERM_NAME).equals("true")) {
				if(termComplex.equals("true")) {
					keys.add(complexTypePrefix + entityMetadata.getSimplePropertyName(propertyName));
				}
				else {
					keys.add(propertyName);
				}
			}
		}

		// Add entity type - sometimes we would like to create entityType even if it does not have key
		// strictKeyCheck will help us in this regard where we can skip the type generated for 
		// EdmDataServices but we can keep the one generated for an individual entity
		if (keys.size() > 0 || !strictKeyCheck) {
			EdmEntityType.Builder bEntityType = EdmEntityType.newBuilder().setNamespace(namespace).setAlias(entityMetadata.getEntityName()).setName(entityMetadata.getEntityName()).addKeys(keys).addProperties(bProperties);
			
			if (annotationAllowed()) {
				// Append additional metadata as annotations
				List<EdmAnnotation<?>> edmEntityTypeAnnotations = new LinkedList<EdmAnnotation<?>>();
				if (displayOnlyProps.size() > 0) 
					edmEntityTypeAnnotations.add((EdmAnnotation.attribute(TermRestriction.NAMESPACE, TermRestriction.PREFIX, Restriction.DISPLAYONLY.getValue(), getPropertiesAsCSV(displayOnlyProps))));
				if (filterOnlyProps.size() > 0)
					edmEntityTypeAnnotations.add((EdmAnnotation.attribute(TermRestriction.NAMESPACE, TermRestriction.PREFIX, Restriction.FILTEREONLY.getValue(), getPropertiesAsCSV(filterOnlyProps))));
				
				bEntityType.setAnnotations(edmEntityTypeAnnotations);
			}
		
			return bEntityType;
		} else {
			LOGGER.error("Unable to add EntityType for [{}] - no ID column defined", entityMetadata.getEntityName());
			return null;
		}
	}
	
	private Map<String, EdmAssociation.Builder> buildAssociations(String namespace, EdmEntityType.Builder entityType, Map<String, EdmEntityType.Builder> bEntityTypeMap, ResourceStateMachine hypermediaEngine, ResourceState serviceDocument) {
		// Obtain the relation between entities and write navigation properties
		Map<String, EdmAssociation.Builder> bAssociationMap = new HashMap<String, EdmAssociation.Builder>();
		//Map<Association name, Entity relation>
		Map<String, EdmAssociation.Builder> relations = new HashMap<String, EdmAssociation.Builder>();

		String entityName = entityType.getName();
		Collection<Transition> entityTransitions = hypermediaEngine.getTransitionsById().values();
		if (entityTransitions != null) {
			//Find out which target entities have more than one transition from this state
			Set<String> targetStateNames = new HashSet<String>();
			Map<String, String> multipleNavPropsToEntity = new HashMap<String, String>();		//Map<TargetEntityName, TargetStateName>
			for(Transition entityTransition : entityTransitions) {
				if (entityTransition.getSource().getEntityName().equals(entityName) 
						&& entityTransition.getTarget() != null
						&& !entityTransition.getTarget().isPseudoState()
						&& !entityTransition.getTarget().equals(serviceDocument)) {
					String targetEntityName = entityTransition.getTarget().getEntityName();
					String targetStateName = entityTransition.getTarget().getName();
					String lastTargetStateName = multipleNavPropsToEntity.get(targetEntityName);
					if(lastTargetStateName == null) {
						multipleNavPropsToEntity.put(targetEntityName, targetStateName);
						targetStateNames.add(entityTransition.getTarget().getName());
					}
					else if(!targetStateName.equals(lastTargetStateName)) {		//Disregard transitions from multiple source states
						multipleNavPropsToEntity.put(targetEntityName, MULTI_NAV_PROP_TO_ENTITY);		//null indicates to generate multiple navigation properties 
					}
				}
			}

			//Create navigation properties from transitions
			Set<String> npNames = new HashSet<String>();
			for(Transition entityTransition : entityTransitions) {
				ResourceState sourceState = entityTransition.getSource();
				ResourceState targetState = entityTransition.getTarget();
				if (targetState == null)
					continue;
				String npName = targetState.getName();
				if (sourceState.getEntityName().equals(entityName) 
						&& !entityTransition.getTarget().isPseudoState()
						&& !entityTransition.getTarget().equals(serviceDocument)
						&& !npNames.contains(npName)
						&& !(entityTransition.getSource() instanceof CollectionResourceState)) {		//We can have transitions to a resource state from multiple source states
					// Use the entity names to define the relation
					String relationName;
					if(multipleNavPropsToEntity.get(targetState.getEntityName()).equals(MULTI_NAV_PROP_TO_ENTITY)) {
						//More than one transition => use separate associations "sourceEntityName_navPropName"
						relationName = sourceState.getEntityName() + "_" + targetState.getName();
					}
					else {
						//Only one transition => use single association "sourceEntityName_targetEntityName"
						relationName = sourceState.getEntityName() + "_" + targetState.getEntityName();
						String invertedRelationName = targetState.getEntityName() + "_" + sourceState.getEntityName();
						if(relations.containsKey(invertedRelationName)) {
							relationName = invertedRelationName;					
						}
					}

					//Multiplicity
					EdmMultiplicity multiplicitySource = targetState instanceof CollectionResourceState ? EdmMultiplicity.ONE : EdmMultiplicity.MANY;
					EdmMultiplicity multiplicityTarget = targetState instanceof CollectionResourceState ? EdmMultiplicity.MANY : EdmMultiplicity.ONE;

					// Association
					EdmAssociationEnd.Builder sourceRole = EdmAssociationEnd.newBuilder()
							.setRole(relationName + "_Source")
							.setType(entityType)
							.setMultiplicity(multiplicitySource);
					EdmEntityType.Builder targetEntityType = bEntityTypeMap.get(targetState.getEntityName());
					assert(targetEntityType != null);
					EdmAssociationEnd.Builder targetRole = EdmAssociationEnd.newBuilder()
							.setRole(relationName + "_Target")
							.setType(targetEntityType)
							.setMultiplicity(multiplicityTarget);
					EdmAssociation.Builder bAssociation = EdmAssociation.newBuilder()
							.setNamespace(namespace)
							.setName(relationName)
							.setEnds(sourceRole, targetRole);

					bAssociationMap.put(targetState.getName(), bAssociation);
					if (!relations.containsKey(relationName)) {
						relations.put(relationName, bAssociation);
					}
				}
			}
		}
		return bAssociationMap;
	}

	/**
	 * Convert a Metadata vocabulary TermValueType to EdmType 
	 * @param type TermValueType
	 * @return EdmType
	 */
	public static EdmType termValueToEdmType(String type) {
		EdmType edmType;
		if(type.equals(TermValueType.NUMBER)) {
			edmType = EdmSimpleType.DOUBLE;
		}
		else if(type.equals(TermValueType.INTEGER_NUMBER)) {
			edmType = EdmSimpleType.INT64;
		}
		else if(type.equals(TermValueType.TIMESTAMP) ||
				type.equals(TermValueType.DATE)) {
			edmType = EdmSimpleType.DATETIME;
		}
		else if(type.equals(TermValueType.TIME)) {
			edmType = EdmSimpleType.TIME;
		}
		else if(type.equals(TermValueType.BOOLEAN)) {
			edmType = EdmSimpleType.BOOLEAN;
		}
		else {
			edmType = EdmSimpleType.STRING;
		}
		return edmType;
	}

	/**
	 * Build the complex type if and only if same name group is not found in the map
	 * @param complexTypeName
	 * @param bComplexTypeMap
	 */
	private void addComplexType(String nameSpace, String complexTypeName, 
			Map<String, EdmComplexType.Builder> bComplexTypeMap) {
		String complexTypeFullName = new StringBuilder(nameSpace)
		.append(".").append(complexTypeName)
		.toString();
		if (bComplexTypeMap.get(complexTypeFullName) == null ) {
			EdmComplexType.Builder cb = 
					EdmComplexType.newBuilder().setNamespace(nameSpace)
					.setName(complexTypeName);
			bComplexTypeMap.put(complexTypeFullName, cb);
		}
	}

	/**
	 * Adding the property to complex type if and only if not already added as a property of the complex type
	 * @param serviceNameSpace
	 * @param complexTypeName
	 * @param edmPropertyBuilder
	 * @param bComplexTypeMap
	 */
	private void addPropertyToComplexType(String nameSpace, String complexTypeName, EdmProperty.Builder edmPropertyBuilder, Map<String, EdmComplexType.Builder> bComplexTypeMap) {
		String complexTypeFullName = new StringBuilder(nameSpace)
		.append(".").append(complexTypeName)
		.toString();
		if (bComplexTypeMap.get(complexTypeFullName).findProperty(edmPropertyBuilder.getName()) == null) {
			List<EdmProperty.Builder> bl = new ArrayList<EdmProperty.Builder>();
			bl.add(edmPropertyBuilder);
			bComplexTypeMap.get(complexTypeFullName).addProperties(bl);
		}
	}

	/**
	 * Adding the ComplexType to Complex Type if and only is not already added as a property of the complex type
	 * @param serviceNameSpace
	 * @param complexTypeName
	 * @param edmPropertyBuilder
	 * @param bComplexTypeMap
	 */
	private void addComplexTypeToComplexType(String nameSpace, String complexTypeName, String nestedComplexType, boolean isNullable, boolean isList, Map<String, EdmComplexType.Builder> bComplexTypeMap) {
		String complexTypeFullName = new StringBuilder(nameSpace).append(".").append(complexTypeName).toString();
		String nestComplexTypeFullName = new StringBuilder(nameSpace).append(".").append(nestedComplexType).toString();
		if (bComplexTypeMap.get(complexTypeFullName).findProperty(nestedComplexType) == null) {
			List<EdmProperty.Builder> bl = new ArrayList<EdmProperty.Builder>();
			EdmProperty.Builder ep;
			if (isList) {
				ep = EdmProperty.newBuilder(nestedComplexType)
						.setType(bComplexTypeMap.get(nestComplexTypeFullName))
						.setCollectionKind(CollectionKind.Bag)
						.setNullable(isNullable);
			} else {
				ep = EdmProperty.newBuilder(nestedComplexType)
						.setType(bComplexTypeMap.get(nestComplexTypeFullName))
						.setNullable(isNullable);
			}
			bl.add(ep);
			bComplexTypeMap.get(complexTypeFullName).addProperties(bl);
		}
	}
	
	/**
	 * By Odata convention EntitySetName should be plural of an EntityName. If EntityName 
	 * provided in null or empty, this method would return empty string
	 * @param entityName
	 * @return
	 */
	private String getEdmEntitySetName(String entityName) {
		if (entityName != null && !entityName.isEmpty())
			return entityName + "s";
		return "";
	}
	
	/**
	 * By Odata convention EntitySetName is a plural of an EntityName, this method would
	 * return entity name by removing the trailing 's' from the provided entitySetName, 
	 * otherwise will return empty string
	 * @param entitySetName
	 * @return
	 */
	private String getEntityName(String entitySetName) {
		if (entitySetName != null && !entitySetName.isEmpty())
			return entitySetName.substring(0, entitySetName.length() -1);
		return "";
	}
	
	/**
	 * XML representation for debugging
	 */
	@Override
	public String toString() {
		java.io.StringWriter wr = new java.io.StringWriter();
		org.odata4j.format.xml.EdmxFormatWriter.write(getEdmMetadata(), wr);
		return wr.toString();
	}
	
	/*
	 * Method to check if we should embed annotation in the metadata
	 */
	private boolean annotationAllowed() {
		return !ODataVersion.V1.equals(this.odataVersion);
	}
	
	/**
	 * Convert List<String> to single , seperated string
	 * @param properties
	 * @return
	 */
	private String getPropertiesAsCSV(List<String> properties) {
		return StringUtils.join(properties.toArray(), ",");
	}
}
