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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.ODataVersion;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.exceptions.NotFoundException;

import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.MetadataParser;
import com.temenos.interaction.core.entity.vocabulary.Term;
import com.temenos.interaction.core.entity.vocabulary.TermFactory;
import com.temenos.interaction.core.hypermedia.Action;
import com.temenos.interaction.core.hypermedia.CollectionResourceState;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.core.hypermedia.UriSpecification;

public class TestMetadataOData4j {
	public final static String METADATA_XML_FILE = "TestMetadataParser.xml";
	public final static String METADATA_AIRLINE_XML_FILE = "AirlinesMetadata.xml";
	public final static String METADATA_CUSTOMER_NON_EXP_XML_FILE = "CustomerNonExpandedMetadata.xml";
	public final static String METADATA_LISTTOBAG_XML_FILE="issue126_metadata.xml";
	private static final String METADATA_NO_MODEL_NAME = "metadataNoModelName.xml";
	
	private static String AIRLINE_NAMESPACE = "FlightResponderModel";
	private static Metadata metadataAirline;
	private static MetadataOData4j metadataOdata4j;
	private static MetadataOData4j metadataAirlineOdata4j;
	private static MetadataOData4j metadataCustomerNonExpandableModelOdata4j;
	private static MetadataOData4j metadataListToBagOdata4j;
	private static MetadataOData4j metadataOdata4jNoModelName;
	
	@BeforeClass
	public static void setup()
	{
		//Read the metadata file
		TermFactory termFactory = new TermFactory() {
			public Term createTerm(String name, String value) throws Exception {
				if(name.equals("TEST_ENTITY_ALIAS")) {
					Term mockTerm = mock(Term.class);
					when(mockTerm.getValue()).thenReturn(value);
					when(mockTerm.getName()).thenReturn(name);
					return mockTerm;
				}
				else {
					return super.createTerm(name, value);
				}
			}			
		};
		
		// Create mock state machine with Customer entity sets
		ResourceState defaultServiceRoot = new ResourceState("SD", "ServiceDocument", new ArrayList<Action>(), "/");
		defaultServiceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("Customer", "Customer", new ArrayList<Action>(), "/Customer")).build());
		defaultServiceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("CustomerWithTermList", "CustomerWithTermList", new ArrayList<Action>(), "/CustomerWithTermList")).build());
		defaultServiceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("EntityWithRestriction", "EntityWithRestriction", new ArrayList<Action>(), "/EntityWithRestriction")).build());
		ResourceStateMachine defaultHypermediaEngine = new ResourceStateMachine(defaultServiceRoot);
		
		
		//Read the TestMetadataParser file
		MetadataParser parser = new MetadataParser(termFactory);
		InputStream is = parser.getClass().getClassLoader().getResourceAsStream(METADATA_XML_FILE);
		Metadata metadata = parser.parse(is);
		Assert.assertNotNull(metadata);

		// Convert TestMetadataParser to odata4j metadata
		metadataOdata4j = new MetadataOData4j(metadata, defaultHypermediaEngine);
		metadataOdata4j.setOdataVersion(ODataVersion.V2);
				
		// Create mock state machine with Flight, Airport and FlightSchedule entity sets
		ResourceState serviceRoot = new ResourceState("SD", "ServiceDocument", new ArrayList<Action>(), "/");
		serviceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("FlightSchedule", "FlightSchedule", new ArrayList<Action>(), "/FlightSchedule")).build());
		serviceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("Flight", "Flight", new ArrayList<Action>(), "/Flight")).build());
		serviceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("Airport", "Airport", new ArrayList<Action>(), "/Airline")).build());
		ResourceStateMachine hypermediaEngine = new ResourceStateMachine(serviceRoot);
		
		
		//Read the airline metadata file
		MetadataParser parserAirline = new MetadataParser();
		InputStream isAirline = parserAirline.getClass().getClassLoader().getResourceAsStream(METADATA_AIRLINE_XML_FILE);
		metadataAirline = parserAirline.parse(isAirline);
		Assert.assertNotNull(metadataAirline);
		
		//Convert airline metadata to odata4j metadata
		metadataAirlineOdata4j = new MetadataOData4j(metadataAirline, hypermediaEngine);
		metadataAirlineOdata4j.setOdataVersion(ODataVersion.V2);
		
				
		// Create mock state machine with Customer entity sets
		ResourceState nonExpandableServiceRoot = new ResourceState("SD", "ServiceDocument", new ArrayList<Action>(), "/");
		nonExpandableServiceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("Customer", "Customer", new ArrayList<Action>(), "/Customer")).build());
		nonExpandableServiceRoot.addTransition(new Transition.Builder().target(new CollectionResourceState("CustomerWithTermList", "CustomerWithTermList", new ArrayList<Action>(), "/CustomerWithTermList")).build());
		ResourceStateMachine nonExpandableHypermediaEngine = new ResourceStateMachine(nonExpandableServiceRoot);
		
		//Read the Complex metadata file
		MetadataParser parserCustomerComplex = new MetadataParser();
		InputStream isCustomer = parserCustomerComplex.getClass().getClassLoader().getResourceAsStream(METADATA_CUSTOMER_NON_EXP_XML_FILE);
		Metadata complexMetadata = parserCustomerComplex.parse(isCustomer);
		Assert.assertNotNull(complexMetadata);
		
		// Convert metadata to odata4j metadata
		metadataCustomerNonExpandableModelOdata4j = new MetadataOData4j(complexMetadata, nonExpandableHypermediaEngine);
		
		// Create mock state machine with Customer entity sets to test List type converted to bag
		ResourceState convertListToBag = new ResourceState("SD", "ServiceDocument", new ArrayList<Action>(), "/");
		convertListToBag.addTransition(new Transition.Builder().target(new CollectionResourceState("Customer", "Customer", new ArrayList<Action>(), "/Customer")).build());
		convertListToBag.addTransition(new Transition.Builder().target(new CollectionResourceState("CustomerWithTermList", "CustomerWithTermList", new ArrayList<Action>(), "/CustomerWithTermList")).build());
		ResourceStateMachine listToBagHypermediaEngine = new ResourceStateMachine(convertListToBag);
		
		//Read the Complex metadata file
		MetadataParser parserCustomerComplexList = new MetadataParser();
		InputStream isCustomerTypeBag = parserCustomerComplexList.getClass().getClassLoader().getResourceAsStream(METADATA_LISTTOBAG_XML_FILE);
		Metadata complexBagMetadata = parserCustomerComplexList.parse(isCustomerTypeBag);
		Assert.assertNotNull(complexBagMetadata);
		
		// Convert metadata to odata4j metadata
		metadataListToBagOdata4j = new MetadataOData4j(complexBagMetadata,listToBagHypermediaEngine);
		metadataListToBagOdata4j.setOdataVersion(ODataVersion.V2);
		
		//Setup mock data for No Model Name test
        setupMockDataForNoModelNameTest(termFactory);
	}
	
	@Test(expected = RuntimeException.class)
	public void testAssertIndividualInitialState() {
		CollectionResourceState serviceRoot = new CollectionResourceState("SD", "ServiceDocument", new ArrayList<Action>(), "/");
		ResourceStateMachine hypermediaEngine = new ResourceStateMachine(serviceRoot);
		new MetadataOData4j(metadataAirline, hypermediaEngine);
	}
	
	@Test
	public void testCustomerEntity()
	{	
		EdmDataServices edmDataServices = metadataOdata4j.getMetadata();
		EdmType type = edmDataServices.findEdmEntityType("CustomerServiceTestModel.Customer");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.Customer"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals("Customer", entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("name").isNullable());			//ID fields must not be nullable
		Assert.assertEquals(false, entityType.findProperty("dateOfBirth").isNullable());
		Assert.assertEquals(false, entityType.findProperty("Customer_address").getType().isSimple());//address should be Complex
		Assert.assertEquals(CollectionKind.NONE, entityType.findProperty("Customer_address").getCollectionKind());//address should not be of CollectionKind
		Assert.assertEquals(null, entityType.findProperty("streetType"));					// This should not be part of EntityType
		
		
		EdmComplexType addressType = edmDataServices.findEdmComplexType("CustomerServiceTestModel.Customer_address");
		Assert.assertEquals(true, addressType.findProperty("town").getType().isSimple());
		Assert.assertEquals(true, addressType.findProperty("postCode").getType().isSimple());
		Assert.assertEquals(false, addressType.findProperty("Customer_street").getType().isSimple());
		Assert.assertEquals(CollectionKind.NONE, addressType.findProperty("Customer_street").getCollectionKind());//street should not be of CollectionKind
		
		EdmComplexType streetType = edmDataServices.findEdmComplexType("CustomerServiceTestModel.Customer_street");
		Assert.assertEquals(true, streetType.findProperty("streetType").isNullable());
		Assert.assertEquals(true, streetType.findProperty("streetType").getType().isSimple());
	}
	
	@Test
	public void testGetNonSrvDocEntitySetByEntitySetName() {
		// Now try to get the EntitySet which is not part of the ServiceDocument ResourceStateMachine
		String nonSrcDocEntityName = "NonSrvDocEntity"; 
		String nonSrcDocEntitySetName = "NonSrvDocEntitys";
		EdmEntitySet nonSrcDocEntitySet = null;

		try {
			// Get the EdmDataServices and verify its not there
			EdmDataServices edmDataServices = metadataOdata4j.getMetadata();
			nonSrcDocEntitySet = edmDataServices.getEdmEntitySet(nonSrcDocEntitySetName);
		} catch (NotFoundException nfe) {
		}
						
		EdmType type = nonSrcDocEntitySet.getType();
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.NonSrvDocEntity"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals(nonSrcDocEntityName, entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("name").isNullable());			//ID fields must not be nullable
		Assert.assertEquals(false, entityType.findProperty("dateOfBirth").isNullable());
		Assert.assertEquals(false, entityType.findProperty("NonSrvDocEntity_address").getType().isSimple());//address should be Complex
		Assert.assertEquals(CollectionKind.NONE, entityType.findProperty("NonSrvDocEntity_address").getCollectionKind());//address should not be of CollectionKind
		Assert.assertEquals(null, entityType.findProperty("streetType"));					// This should not be part of EntityType
		
		EdmComplexType addressType = (EdmComplexType) entityType.findProperty("NonSrvDocEntity_address").getType();
		Assert.assertEquals(true, addressType.findProperty("town").getType().isSimple());
		Assert.assertEquals(true, addressType.findProperty("postCode").getType().isSimple());
		Assert.assertEquals(false, addressType.findProperty("NonSrvDocEntity_street").getType().isSimple());
		Assert.assertEquals(CollectionKind.NONE, addressType.findProperty("NonSrvDocEntity_street").getCollectionKind());//street should not be of CollectionKind
		
		EdmComplexType streetType = (EdmComplexType) addressType.findProperty("NonSrvDocEntity_street").getType();
		Assert.assertEquals(true, streetType.findProperty("streetType").isNullable());
		Assert.assertEquals(true, streetType.findProperty("streetType").getType().isSimple());
	}
	
	@Test
	public void testGetNonSrvDocEntitySetByEntityName() {
		// Now try to get the EntitySet which is not part of the ServiceDocument ResourceStateMachine
		String nonSrcDocEntityName = "NonSrvDocEntity";
		EdmEntitySet nonSrcDocEntitySet = metadataOdata4j.getEdmEntitySetByEntityName(nonSrcDocEntityName);
		Assert.assertNotNull(nonSrcDocEntitySet);	// This time it should load, prepare and return
		
		EdmType type = nonSrcDocEntitySet.getType();
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.NonSrvDocEntity"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals(nonSrcDocEntityName, entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("name").isNullable());			//ID fields must not be nullable
		Assert.assertEquals(false, entityType.findProperty("dateOfBirth").isNullable());
		Assert.assertEquals(false, entityType.findProperty("NonSrvDocEntity_address").getType().isSimple());//address should be Complex
		Assert.assertEquals(CollectionKind.NONE, entityType.findProperty("NonSrvDocEntity_address").getCollectionKind());//address should not be of CollectionKind
		Assert.assertEquals(null, entityType.findProperty("streetType"));					// This should not be part of EntityType
		
		EdmComplexType addressType = (EdmComplexType) entityType.findProperty("NonSrvDocEntity_address").getType();
		Assert.assertEquals(true, addressType.findProperty("town").getType().isSimple());
		Assert.assertEquals(true, addressType.findProperty("postCode").getType().isSimple());
		Assert.assertEquals(false, addressType.findProperty("NonSrvDocEntity_street").getType().isSimple());
		Assert.assertEquals(CollectionKind.NONE, addressType.findProperty("NonSrvDocEntity_street").getCollectionKind());//street should not be of CollectionKind
		
		EdmComplexType streetType = (EdmComplexType) addressType.findProperty("NonSrvDocEntity_street").getType();
		Assert.assertEquals(true, streetType.findProperty("streetType").isNullable());
		Assert.assertEquals(true, streetType.findProperty("streetType").getType().isSimple());
	}
	
	@Test
	public void testFindEdmComplexType() {
		EdmComplexType complexType = metadataOdata4j.findEdmComplexType("CustomerServiceTestModel.NonSrvDocEntity_street");
		Assert.assertEquals(true, complexType.findProperty("streetType").isNullable());
		Assert.assertEquals(true, complexType.findProperty("streetType").getType().isSimple());
	}
	
	
	@Test
	public void testCustomerWithListTypeTAGEntity()
	{	
		EdmDataServices edmDataServices = metadataOdata4j.getMetadata();
		EdmType type = edmDataServices.findEdmEntityType("CustomerServiceTestModel.CustomerWithTermList");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.CustomerWithTermList"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals("CustomerWithTermList", entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("name").isNullable());			//ID fields must not be nullable
		Assert.assertEquals(false, entityType.findProperty("dateOfBirth").isNullable());
		Assert.assertEquals(false, entityType.findProperty("CustomerWithTermList_address").getType().isSimple());//address should be Complex
		Assert.assertEquals(CollectionKind.Bag, entityType.findProperty("CustomerWithTermList_address").getCollectionKind());//address should be List of Complex -- Changed collection kind from list to bag due to issue no. 126
		Assert.assertEquals(null, entityType.findProperty("streetType"));					// This should not be part of EntityType
		
		
		EdmComplexType addressType = edmDataServices.findEdmComplexType("CustomerServiceTestModel.CustomerWithTermList_address");
		Assert.assertEquals(true, addressType.findProperty("town").getType().isSimple());
		Assert.assertEquals(true, addressType.findProperty("postCode").getType().isSimple());
		Assert.assertEquals(false, addressType.findProperty("CustomerWithTermList_street").getType().isSimple());
		Assert.assertEquals(CollectionKind.NONE, addressType.findProperty("CustomerWithTermList_street").getCollectionKind());	// street should be complex but not list
		
		EdmComplexType streetType = edmDataServices.findEdmComplexType("CustomerServiceTestModel.CustomerWithTermList_street");
		Assert.assertEquals(true, streetType.findProperty("streetType").isNullable());
		Assert.assertEquals(true, streetType.findProperty("streetType").getType().isSimple());
	}

	@Test
	public void testCustomerComplexEntity() {
		EdmDataServices edmDataServices = metadataCustomerNonExpandableModelOdata4j.getMetadata();
		EdmType type = edmDataServices.findEdmEntityType("CustomerServiceTestModel.Customer");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.Customer"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals("Customer", entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("name").isNullable());			//ID fields must not be nullable
		Assert.assertEquals(false, entityType.findProperty("Customer_address").getType().isSimple());//address should be Complex
		Assert.assertEquals(CollectionKind.NONE, entityType.findProperty("Customer_address").getCollectionKind());//address should be List of Complex
		Assert.assertEquals(false, entityType.findProperty("dateOfBirth").isNullable());
		Assert.assertEquals(null, entityType.findProperty("streetType"));					// This should not be part of EntityType
	}
	
	//NonExpandableMetadata()
	@Test
	public void testCustomerWithTermListWithNonExpandableMetadata() {
		EdmDataServices edmDataServices = metadataCustomerNonExpandableModelOdata4j.getMetadata();
		EdmType type = edmDataServices.findEdmEntityType("CustomerServiceTestModel.CustomerWithTermList");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.CustomerWithTermList"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals("CustomerWithTermList", entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("name").isNullable());			//ID fields must not be nullable
		Assert.assertEquals(false, entityType.findProperty("CustomerWithTermList_address").getType().isSimple());//address should be Complex
		Assert.assertEquals(CollectionKind.Bag, entityType.findProperty("CustomerWithTermList_address").getCollectionKind()); //Changed the test case due to issue126 fix
		Assert.assertEquals(false, entityType.findProperty("dateOfBirth").isNullable());
		Assert.assertEquals(null, entityType.findProperty("streetType"));					// This should not be part of EntityType
	}
	
	@Test
	//Issue126 verify metadata files does not contain CollectionKind="List"
	public void testTermListTypeAsBag() {
		EdmDataServices edmDataServices = metadataListToBagOdata4j.getMetadata();
		EdmType type = edmDataServices.findEdmEntityType("CustomerServiceTestModel.CustomerWithTermList");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("CustomerServiceTestModel.CustomerWithTermList"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals("CustomerWithTermList", entityType.getName());
		Assert.assertEquals(CollectionKind.Bag, entityType.findProperty("CustomerWithTermList_address").getCollectionKind());//address should be Bag of ComplexType	
		EdmProperty proprty_type = entityType.findProperty("CustomerWithTermList_address");
		EdmComplexType proprty_type_complex = (EdmComplexType) proprty_type.getType();
		Assert.assertEquals(CollectionKind.Bag, proprty_type_complex.findProperty("CustomerWithTermList_street").getCollectionKind());//street should be Bag of ComplexType
	}
	
	
	@Test
	public void testAirlineSchemaCount()
	{	
		EdmDataServices edmDataServices = metadataAirlineOdata4j.getMetadata();
		Assert.assertEquals(1, edmDataServices.getSchemas().size());
	}
	
	@Test
	public void testAirlineEntityTypes()
	{	
		EdmDataServices edmDataServices = metadataAirlineOdata4j.getMetadata();
		EdmType type = edmDataServices.findEdmEntityType(AIRLINE_NAMESPACE + ".FlightSchedule");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.getFullyQualifiedTypeName().equals("FlightResponderModel.FlightSchedule"));
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		Assert.assertEquals("FlightSchedule", entityType.getName());
		Assert.assertEquals(false, entityType.findProperty("flightScheduleID").isNullable());
	}

	@Test
	public void testAirlineEntitySets()
	{	
		EdmDataServices edmDataServices = metadataAirlineOdata4j.getMetadata();
		Assert.assertEquals(1, edmDataServices.getSchemas().size());
		Assert.assertEquals(1, edmDataServices.getSchemas().get(0).getEntityContainers().size());
		Assert.assertEquals(3, edmDataServices.getSchemas().get(0).getEntityContainers().get(0).getEntitySets().size());
		EdmEntitySet entitySetFlightSchedule = edmDataServices.findEdmEntitySet("FlightSchedule");
		Assert.assertEquals("FlightSchedule", entitySetFlightSchedule.getName());
	}
	
	@Test
	public void testManyToOneMandatoryNavProperty() {
		// create mock resource interaction (which should result in creation of mandatory Navigation Property in EdmDataService metadata)
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/");
		
		// flights and airports
		CollectionResourceState flights = new CollectionResourceState("Flight", "Flights", new ArrayList<Action>(), "/Flights");
		ResourceState flight = new ResourceState("Flight", "flight", new ArrayList<Action>(), "/Flights({id})");
		CollectionResourceState airports = new CollectionResourceState("Airport", "Airports", new ArrayList<Action>(), "/Airports");
		ResourceState airport = new ResourceState("Airport", "airport", new ArrayList<Action>(), "/Airports({id})");
		// a flight must have a departure airport
		ResourceState flightDepartureAirport = new ResourceState("Airport", "departureAirport", new ArrayList<Action>(), "/Flights({id})/departureAirport");
		
		
		initial.addTransition(new Transition.Builder().target(flights).build());
		flights.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(flight).build());
		initial.addTransition(new Transition.Builder().target(airports).build());
		airports.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airport).build());
		flight.addTransition(new Transition.Builder().target(flightDepartureAirport).build());
		flights.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(flightDepartureAirport).build());
		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		// entity types (one with the mandatory nav property)
		EdmEntityType entityType = (EdmEntityType) edmMetadata.findEdmEntityType(AIRLINE_NAMESPACE + ".Flight");
		EdmNavigationProperty flightNavProperty = entityType.findNavigationProperty("departureAirport");
		assertNotNull(flightNavProperty);
		assertEquals("Flight_Airport", flightNavProperty.getRelationship().getName());
		assertEquals("Flight_Airport_Source", flightNavProperty.getFromRole().getRole());
		assertEquals("Flight_Airport_Target", flightNavProperty.getToRole().getRole());
		// check association
		assertEquals("*", flightNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("1", flightNavProperty.getToRole().getMultiplicity().getSymbolString());
		
		// associations
		assertNotNull(edmMetadata.getAssociations());
		int noAssociations = 0;
		for (EdmAssociation association : edmMetadata.getAssociations()) {
			noAssociations++;
			if ("Flight_Airport".equals(association.getName())) {
			} else {
				fail("Unexpected association");
			}
		}
		assertEquals(1, noAssociations);
		
		// entity sets (from ResourceStateMachine)
		assertNotNull(edmMetadata.getEntitySets());
		int noEntitySets = 0;
		for (EdmEntitySet entitySet : edmMetadata.getEntitySets()) {
			noEntitySets++;
			if ("Flights".equals(entitySet.getName())) {
			} else if ("Airports".equals(entitySet.getName())) {
			} else {
				fail("Unexpected entity set");
			}
		}
		assertEquals(2, noEntitySets);
	}
	
	@Test
	public void testManyToManyNavProperty() throws Exception {
		// create mock resource interaction (which should result in creation of Navigation Property in EdmDataService metadata)
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/");
		CollectionResourceState flights = new CollectionResourceState("Flight", "Flights", new ArrayList<Action>(), "/Flights");
		CollectionResourceState airports = new CollectionResourceState("Airport", "Airports", new ArrayList<Action>(), "/Airports");
		CollectionResourceState flightSchedules = new CollectionResourceState("FlightSchedule", "FlightSchedules", new ArrayList<Action>(), "/FlightSchedules");
		ResourceState flight = new ResourceState("Flight", "flight", new ArrayList<Action>(), "/Flights({id})");
		CollectionResourceState airportFlights = new CollectionResourceState("Airport", "AirportFlights", new ArrayList<Action>(), "/Airports({id})/Flights");
		initial.addTransition(new Transition.Builder().target(flights).build());
		initial.addTransition(new Transition.Builder().target(airports).build());
		initial.addTransition(new Transition.Builder().target(flightSchedules).build());
		flight.addTransition(new Transition.Builder().method("GET").target(airportFlights).build());
		flights.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airportFlights).build());
		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		
		// entities (from Metadata)
		assertNotNull(edmMetadata.getEntitySets());
		int noEntities = 0;
		for (EdmEntityType entityType : edmMetadata.getEntityTypes()) {
			noEntities++;
			if ("Flight".equals(entityType.getName())) {
			} else if ("Airport".equals(entityType.getName())) {
			} else if ("FlightSchedule".equals(entityType.getName())) {
			} else {
				fail("Unexpected entity");
			}
		}
		assertEquals(3, noEntities);

		// entity sets (from ResourceStateMachine)
		assertNotNull(edmMetadata.getEntitySets());
		int noEntitySets = 0;
		for (EdmEntitySet entitySet : edmMetadata.getEntitySets()) {
			noEntitySets++;
			if ("Flights".equals(entitySet.getName())) {
			} else if ("Airports".equals(entitySet.getName())) {
			} else if ("FlightSchedules".equals(entitySet.getName())) {
			} else {
				fail("Unexpected entity set");
			}
		}
		assertEquals(3, noEntitySets);

		// function imports (from ResourceStateMachine where transition to Collection not from initial state)
		assertNotNull(edmMetadata.findEdmFunctionImport("AirportFlights"));
	}

	/*
	 * Test to check the navigation property to a collection resource.
	 * e.g. /Airports(JFK) --> /FlightSchedules
	 * => nav property should be target EntitySet name:  FlightSchedules
	 */
	@Test
	public void testSingleOneToManyNavProperty() {
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/", null, new UriSpecification("ROOT", "/"));
		CollectionResourceState airports = new CollectionResourceState("Airport", "airports", new ArrayList<Action>(), "/Airports");
		ResourceState airport = new ResourceState("Airport", "airport", new ArrayList<Action>(), "/Airports('{id}')", null, new UriSpecification("airport", "/Airports('{id}')"));
		CollectionResourceState flightSchedules = new CollectionResourceState("FlightSchedule", "FlightSchedules", new ArrayList<Action>(), "/FlightSchedules({filter})", null, null);

		initial.addTransition(new Transition.Builder().method("GET").target(airports).build());
		airports.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airport).build());
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).build());

		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		EdmEntityType entityType = (EdmEntityType) edmMetadata.findEdmEntityType(AIRLINE_NAMESPACE + ".Airport");

		EdmNavigationProperty flightScheduleNavProperty = entityType.findNavigationProperty("FlightSchedules");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());
	}
	
	/*
	 * Test to check the navigation property to a filtered collection resource.
	 * e.g. /Airports(JFK) title="departures" --> /FlightSchedules?$filter=departureAirportCode eq '{code}'
	 * => nav property should be link title:  departures
	 */
	@Test
	public void testSingleOneToManyFilteredNavProperty() {
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/", null, new UriSpecification("ROOT", "/"));
		CollectionResourceState airports = new CollectionResourceState("Airport", "airports", new ArrayList<Action>(), "/Airports");
		ResourceState airport = new ResourceState("Airport", "airport", new ArrayList<Action>(), "/Airports('{id}')", null, new UriSpecification("airport", "/Airports('{id}')"));
		CollectionResourceState flightSchedules = new CollectionResourceState("FlightSchedule", "FlightSchedules", new ArrayList<Action>(), "/FlightSchedules({filter})", null, null);

		initial.addTransition(new Transition.Builder().method("GET").target(airports).build());
		airports.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airport).build());
		Map<String, String> uriLinkageProperties = new HashMap<String, String>();
		uriLinkageProperties.put("filter", "departureAirportCode eq '{code}'");
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).uriParameters(uriLinkageProperties).label("departures").build());

		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		EdmEntityType entityType = (EdmEntityType) edmMetadata.findEdmEntityType(AIRLINE_NAMESPACE + ".Airport");

		EdmNavigationProperty flightScheduleNavProperty = entityType.findNavigationProperty("departures");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());
	}
	
	/*
	 * Test to check the navigation property to a filtered collection resource when the transition does not have a label.
	 * e.g. /Airports(JFK) --> /FlightSchedules?$filter=departureAirportCode eq '{code}'
	 * => nav property should be filter expression:  departureAirportCode eq '{code}'
	 */
	@Test
	public void testSingleOneToManyFilteredNavPropertyWithoutLabel() {
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/", null, new UriSpecification("ROOT", "/"));
		CollectionResourceState airports = new CollectionResourceState("Airport", "airports", new ArrayList<Action>(), "/Airports");
		ResourceState airport = new ResourceState("Airport", "airport", new ArrayList<Action>(), "/Airports('{id}')", null, new UriSpecification("airport", "/Airports('{id}')"));
		CollectionResourceState flightSchedules = new CollectionResourceState("FlightSchedule", "FlightSchedules", new ArrayList<Action>(), "/FlightSchedules({filter})", null, null);

		initial.addTransition(new Transition.Builder().method("GET").target(airports).build());
		airports.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airport).build());
		Map<String, String> uriLinkageProperties = new HashMap<String, String>();
		uriLinkageProperties.put("filter", "departureAirportCode eq '{code}'");
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).uriParameters(uriLinkageProperties).build());

		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		EdmEntityType entityType = (EdmEntityType) edmMetadata.findEdmEntityType(AIRLINE_NAMESPACE + ".Airport");

		EdmNavigationProperty flightScheduleNavProperty = entityType.findNavigationProperty("FlightSchedules");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());
	}
	
	/*
	 * Test to check multiple navigation properties to a collection resource (must be filtered to avoid duplicate transition).
	 * e.g. /Airports(JFK) title="departures" --> /FlightSchedules?$filter=departureAirportCode eq '{code}'
	 *      /Airports(JFK) title="arrivals" --> /FlightSchedules?$filter=arrivalAirportCode eq '{code}'
	 * => nav properties should be link titles:  departures / arrivals
	 */
	@Test
	public void testMultipleOneToManyNavProperties() {
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/", null, new UriSpecification("ROOT", "/"));
		CollectionResourceState airports = new CollectionResourceState("Airport", "airports", new ArrayList<Action>(), "/Airports");
		ResourceState airport = new ResourceState("Airport", "airport", new ArrayList<Action>(), "/Airports('{id}')", null, new UriSpecification("airport", "/Airports('{id}')"));
		CollectionResourceState flightSchedules = new CollectionResourceState("FlightSchedule", "FlightSchedules", new ArrayList<Action>(), "/FlightSchedules({filter})", null, null);

		initial.addTransition(new Transition.Builder().method("GET").target(airports).build());
		airports.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airport).build());
		Map<String, String> uriLinkageProperties = new HashMap<String, String>();
		uriLinkageProperties.put("filter", "arrivalAirportCode eq '{code}'");
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).uriParameters(uriLinkageProperties).label("arrivals").build());
		uriLinkageProperties.put("filter", "departureAirportCode eq '{code}'");
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).uriParameters(uriLinkageProperties).label("departures").build());

		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		EdmEntityType entityType = (EdmEntityType) edmMetadata.findEdmEntityType(AIRLINE_NAMESPACE + ".Airport");

		//Navigation property to many entities is usually the EntitySet name (e.g. FlightSchedules). However, in this case it is NOT the EntitySet name
		//but a transition ID identifying the link (the link title at the moment, e.g. departures). It does not fully comply with OData but this one does not cater for multiple links to the same target.  
		EdmNavigationProperty flightScheduleNavProperty = entityType.findNavigationProperty("departures");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());

		flightScheduleNavProperty = entityType.findNavigationProperty("arrivals");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());
	}
	
	/*
	 * Test to check multiple navigation properties to a collection resource (must be filtered to avoid duplicate transition)
	 * when the transitions do not have labels.
	 * e.g. /Airports(JFK) --> /FlightSchedules?$filter=departureAirportCode eq '{code}'
	 *      /Airports(JFK) --> /FlightSchedules?$filter=arrivalAirportCode eq '{code}'
	 * => nav properties should be the filter expressions:  departureAirportCode eq '{code}' / arrivalAirportCode eq '{code}'
	 */
	@Test
	public void testMultipleOneToManyNavPropertiesWithoutLabel() {
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", new ArrayList<Action>(), "/", null, new UriSpecification("ROOT", "/"));
		CollectionResourceState airports = new CollectionResourceState("Airport", "airports", new ArrayList<Action>(), "/Airports");
		ResourceState airport = new ResourceState("Airport", "airport", new ArrayList<Action>(), "/Airports('{id}')", null, new UriSpecification("airport", "/Airports('{id}')"));
		CollectionResourceState flightSchedules = new CollectionResourceState("FlightSchedule", "FlightSchedules", new ArrayList<Action>(), "/FlightSchedules({filter})", null, null);

		initial.addTransition(new Transition.Builder().method("GET").target(airports).build());
		airports.addTransition(new Transition.Builder().flags(Transition.FOR_EACH).method("GET").target(airport).build());
		Map<String, String> uriLinkageProperties = new HashMap<String, String>();
		uriLinkageProperties.put("filter", "arrivalAirportCode eq '{code}'");
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).uriParameters(uriLinkageProperties).build());
		uriLinkageProperties.put("filter", "departureAirportCode eq '{code}'");
		airport.addTransition(new Transition.Builder().method("GET").target(flightSchedules).uriParameters(uriLinkageProperties).build());

		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadataAirline, rsm);
		EdmDataServices edmMetadata = metadataOData4j.getMetadata();
		
		assertNotNull(edmMetadata);
		EdmEntityType entityType = (EdmEntityType) edmMetadata.findEdmEntityType(AIRLINE_NAMESPACE + ".Airport");

		//Navigation property to many entities is usually the EntitySet name (e.g. FlightSchedules)
		EdmNavigationProperty flightScheduleNavProperty = entityType.findNavigationProperty("FlightSchedules");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());

		flightScheduleNavProperty = entityType.findNavigationProperty("FlightSchedules_0");
		assertNotNull(flightScheduleNavProperty);
		assertEquals("Airport_FlightSchedule", flightScheduleNavProperty.getRelationship().getName());
		assertEquals("Airport_FlightSchedule_Source", flightScheduleNavProperty.getFromRole().getRole());
		assertEquals("Airport_FlightSchedule_Target", flightScheduleNavProperty.getToRole().getRole());
		assertEquals("1", flightScheduleNavProperty.getFromRole().getMultiplicity().getSymbolString());
		assertEquals("*", flightScheduleNavProperty.getToRole().getMultiplicity().getSymbolString());
	}
	
	
	/*
	 * helper function to return mock EdmEntitySet
	 */
	private EdmEntitySet createMockEdmEntitySet() {
		// Create an entity set
		List<EdmProperty.Builder> eprops = new ArrayList<EdmProperty.Builder>();
		EdmProperty.Builder ep = EdmProperty.newBuilder("id").setType(EdmSimpleType.STRING);
		eprops.add(ep);
		EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace("InteractionTest").setName("Flight").addKeys(Arrays.asList("id")).addProperties(eprops);
		EdmEntitySet.Builder eesb = EdmEntitySet.newBuilder().setName("Flight").setEntityType(eet);
		return eesb.build();
	}
	
	/**
	 * test for getEdmEntitySet function
	 */
	@Test
	public void testGetEdmEntitySet() {
		EdmEntitySet ees = createMockEdmEntitySet();
		MetadataOData4j mockMetadataOData4j = mock(MetadataOData4j.class);
		when(mockMetadataOData4j.getEdmEntitySetByEntityName("Flight")).thenReturn(ees);		
		assertEquals(ees, mockMetadataOData4j.getEdmEntitySetByEntityName("Flight"));
		assertEquals("Flight", mockMetadataOData4j.getEdmEntitySetByEntityName("Flight").getName());	
	}
	
	/**
	 * test for getEdmEntitySet function for missing entity
	 */
	@Test (expected=NotFoundException.class)
	public void testNullEdmEntitySet() {
		ResourceState initial = new ResourceState("ROOT", "ServiceDocument", 
						new ArrayList<Action>(), "/", null, 
						new UriSpecification("ROOT", "/"));
		ResourceStateMachine rsm = new ResourceStateMachine(initial);
		Metadata metadata = mock(Metadata.class);
		when(metadata.getEntityMetadata(Mockito.anyString())).thenReturn(null);
		MetadataOData4j metadataOData4j = new MetadataOData4j(metadata, rsm);
		assertEquals("AnyEntity", metadataOData4j.getEdmEntitySetByEntityName("AnyEntity").getName());
	}
	
	/**
	 * test for getEdmEntitySetName
	 */
	@Test
	public void testGetEdmEntitySetName() {
		EdmEntitySet ees = createMockEdmEntitySet();
		MetadataOData4j mockMetadataOData4j = mock(MetadataOData4j.class);
		when(mockMetadataOData4j.getEdmEntitySetByEntitySetName("Flight")).thenReturn(ees);		
		assertEquals(ees, mockMetadataOData4j.getEdmEntitySetByEntitySetName("Flight"));
	}
	
	
	/**
	 * test for getEdmEntitySetName for missing entity set name
	 */
	@Test (expected=NotFoundException.class)
	public void testNullEdmEntitySetName() {
		EdmEntitySet ees = createMockEdmEntitySet();
		MetadataOData4j mockMetadataOData4j = mock(MetadataOData4j.class);
		when(mockMetadataOData4j.getEdmEntitySetByEntitySetName("Dummy")).
		thenThrow(new NotFoundException("EntitySet for entity type Dummy has not been found"));		
		assertEquals(ees, mockMetadataOData4j.getEdmEntitySetByEntitySetName("Dummy"));
	}
	
	/**
	 * test to verify if we now make an exception and allowed
	 * only ServiceDocument to be able to create its EntitySet
	 * i.e. Only entity with no key 
	 */
	public void testAddingServiceDocumentAsEntityType() {
		EdmEntitySet srvEES = metadataOdata4j.getEdmEntitySetByEntityName("ServiceDocument");
		assertNotNull(srvEES);
	}
	
	@Test
	public void testAirlineEntitySemanticTypes()
	{	
		EdmDataServices edmDataServices = metadataAirlineOdata4j.getMetadata();
		
		EdmType type = edmDataServices.findEdmEntityType(AIRLINE_NAMESPACE + ".Airport");
		Assert.assertNotNull(type);
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		NamespacedAnnotation<?> ann = entityType.findProperty("country").findAnnotation("http://iris.temenos.com/odata-extensions", "semanticType");
		Assert.assertEquals(EdmAnnotationAttribute.class, ann.getClass());
		EdmAnnotationAttribute annEl = (EdmAnnotationAttribute)ann;
		Assert.assertNotNull(annEl.getNamespace().getPrefix());
		Assert.assertNotNull(annEl.getNamespace().getUri());
		Assert.assertEquals("Geography:Country", ann.getValue());
	}
		
	@Test
	public void testEntityWithDisplayAndFilterOnlyProperties()
	{	
		EdmDataServices edmDataServices = metadataOdata4j.getMetadata();
		
		EdmType type = edmDataServices.findEdmEntityType("CustomerServiceTestModel.EntityWithRestriction");
		Assert.assertNotNull(type);
		Assert.assertTrue(type instanceof EdmEntityType);
		EdmEntityType entityType = (EdmEntityType) type;
		// Verify DisplayOnly properties
		NamespacedAnnotation<?> ann = entityType.findAnnotation("http://iris.temenos.com/odata-extensions", "displayOnly");
		Assert.assertEquals(EdmAnnotationAttribute.class, ann.getClass());
		EdmAnnotationAttribute annEl = (EdmAnnotationAttribute)ann;
		Assert.assertNotNull(annEl.getNamespace().getPrefix());
		Assert.assertNotNull(annEl.getNamespace().getUri());
		Assert.assertTrue(ann.getValue().toString().contains("sector"));
		Assert.assertTrue(ann.getValue().toString().contains("EntityWithRestriction_address.number"));
		Assert.assertTrue(ann.getValue().toString().contains("EntityWithRestriction_street.streetType"));
		
		// Filter Only properties
		ann = entityType.findAnnotation("http://iris.temenos.com/odata-extensions", "filterOnly");
		Assert.assertEquals(EdmAnnotationAttribute.class, ann.getClass());
		annEl = (EdmAnnotationAttribute)ann;
		Assert.assertNotNull(annEl.getNamespace().getPrefix());
		Assert.assertNotNull(annEl.getNamespace().getUri());
		Assert.assertTrue(ann.getValue().toString().contains("industry"));
		Assert.assertTrue(ann.getValue().toString().contains("EntityWithRestriction_address.town"));
		Assert.assertTrue(ann.getValue().toString().contains("EntityWithRestriction_address.postCode"));
	}	
	
	@Test
    public void testCustomerEntityNoModelName() {
        EdmDataServices edmDataServices = metadataOdata4jNoModelName.getMetadata();
        EdmSchema schema = edmDataServices.findSchema("CustomerModelNameModel");
        Assert.assertNotNull(schema);
    }
    
    private static void setupMockDataForNoModelNameTest(TermFactory termFactory) {
        //Create mock state engine and read metadata file with no Model Name
        ResourceState mockStateEngine = new ResourceState("SD", "ServiceDocument", new ArrayList<Action>(), "/");
        mockStateEngine.addTransition(new Transition.Builder().target(new CollectionResourceState("Customer", "Customer", new ArrayList<Action>(), "/Customer")).build());
        ResourceStateMachine defaultHypermediaEngine = new ResourceStateMachine(mockStateEngine);
        
        //Read the TestMetadataParser file
        MetadataParser parser = new MetadataParser(termFactory);
        InputStream is = parser.getClass().getClassLoader().getResourceAsStream(METADATA_NO_MODEL_NAME);
        Metadata metadata = parser.parse(is);
        Assert.assertNotNull(metadata);

        // Convert TestMetadataParser to odata4j metadata
        metadataOdata4jNoModelName = new MetadataOData4j(metadata, defaultHypermediaEngine);
        metadataOdata4jNoModelName.setOdataVersion(ODataVersion.V2);
    }

}
