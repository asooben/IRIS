package com.temenos.interaction.core.hypermedia.link;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.hypermedia.*;
import com.temenos.interaction.core.web.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.odata4j.core.*;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmEntitySet;

import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by ikarady on 07/04/2016.
 */
public class TestMultiLinkGenerator {

    @Before
    public void setup() {
        // initialise the thread local request context with requestUri and baseUri
        RequestContext ctx = new RequestContext("/baseuri", "/requesturi", null);
        RequestContext.setRequestContext(ctx);
    }

    @Test
    public void testCreateLinkForCollectionEntity() {
        Link result = null;
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("test", "{Contact.Email}");
        CollectionResourceState customerState = new CollectionResourceState("customer", "customer", new ArrayList<Action>(), "/customer()", null, null);
        CollectionResourceState contactState = new CollectionResourceState("contact", "contact", new ArrayList<Action>(), "/contact()", null, null);
        customerState.addTransition(new Transition.Builder().method("GET").target(contactState).uriParameters(uriParameters).flags(Transition.FOR_EACH).build());
        OCollection<?> contactColl = OCollections.newBuilder(null)
                .add(createComplexObject("Email","johnEmailAddr","Tel","12345"))
                .add(createComplexObject("Email","smithEmailAddr","Tel","66778")).build();
        ResourceStateMachine engine = new ResourceStateMachine(customerState, getOEntityTransformer(contactColl));

        OProperty<?> contactProp =  OProperties.collection("source_Contact", null, contactColl);
        List<OProperty<?>> contactPropList = new ArrayList<OProperty<?>>();
        contactPropList.add(contactProp);
        OEntity entity = OEntities.createRequest(EdmEntitySet.newBuilder().build(), contactPropList, null);
        Transition t = customerState.getTransitions().get(0);
        LinkGenerator linkGenerator = new MultiLinkGenerator(engine, t, entity, "Contact.Email");
        Collection<Link> links = linkGenerator.createLink(null, null, null);
        Iterator<Link> iterator = links.iterator();

        assertEquals(2, links.size());

        result = iterator.next();
        assertEquals("/baseuri/contact()?test=johnEmailAddr", result.getHref());
        assertEquals("collection", result.getRel());

        result = iterator.next();
        assertEquals("/baseuri/contact()?test=smithEmailAddr", result.getHref());
        assertEquals("collection", result.getRel());
    }

    @Test
    public void testCreateLinkForCollectionEntityTwoLevel() {
        Link result = null;
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("test", "{Contact.Address.PostCode}");
        CollectionResourceState customerState = new CollectionResourceState("customer", "customer", new ArrayList<Action>(), "/customer()", null, null);
        CollectionResourceState contactState = new CollectionResourceState("contact", "contact", new ArrayList<Action>(), "/contact()", null, null);
        customerState.addTransition(new Transition.Builder().method("GET").target(contactState).uriParameters(uriParameters).flags(Transition.FOR_EACH).build());

        //Inner collection
        OCollection<?> postCodeColl = OCollections.newBuilder(null).add(createComplexObject("PostCode", "ABCD")).add(createComplexObject("PostCode", "EFGH")).build();

        //Outer Collection
        OProperty<?> addressCollProperty =  OProperties.collection("Address", null, postCodeColl);
        List<OProperty<?>> addressPropList = new ArrayList<OProperty<?>>();
        addressPropList.add(addressCollProperty);
        OComplexObject addressDetails = OComplexObjects.create(EdmComplexType.newBuilder().build(), addressPropList);
        OCollection<?> addressColl = OCollections.newBuilder(null).add(addressDetails).build();

        ResourceStateMachine engine = new ResourceStateMachine(customerState, getOEntityTransformer(addressColl));

        OProperty<?> contactProp =  OProperties.collection("source_Contact", null, addressColl);
        List<OProperty<?>> contactPropList = new ArrayList<OProperty<?>>();
        contactPropList.add(contactProp);
        OEntity entity = OEntities.createRequest(EdmEntitySet.newBuilder().build(), contactPropList, null);
        Transition t = customerState.getTransitions().get(0);
        LinkGenerator linkGenerator = new MultiLinkGenerator(engine, t, entity, "Contact.Address.PostCode");
        Collection<Link> links = linkGenerator.createLink(null, null, null);
        Iterator<Link> iterator = links.iterator();

        assertEquals(2, links.size());

        result = iterator.next();
        assertEquals("/baseuri/contact()?test=ABCD", result.getHref());
        assertEquals("collection", result.getRel());

        result = iterator.next();
        assertEquals("/baseuri/contact()?test=EFGH", result.getHref());
        assertEquals("collection", result.getRel());
    }

    @Test
    public void testCreateLinkForCollectionEntityMultiParams() {
        Link result = null;
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("test", "{personName} and {Contact.Email}");
        CollectionResourceState customerState = new CollectionResourceState("customer", "customer", new ArrayList<Action>(), "/customer()", null, null);
        CollectionResourceState contactState = new CollectionResourceState("contact", "contact", new ArrayList<Action>(), "/contact()", null, null);
        customerState.addTransition(new Transition.Builder().method("GET").target(contactState).uriParameters(uriParameters).flags(Transition.FOR_EACH).build());
        OCollection<?> contactColl = OCollections.newBuilder(null)
                .add(createComplexObject("Email","johnEmailAddr","Tel","12345"))
                .add(createComplexObject("Email","smithEmailAddr","Tel","66778")).build();
        ResourceStateMachine engine = new ResourceStateMachine(customerState, getOEntityTransformer(contactColl));

        OProperty<?> contactProp =  OProperties.collection("source_Contact", null, contactColl);
        List<OProperty<?>> contactPropList = new ArrayList<OProperty<?>>();
        contactPropList.add(contactProp);
        OEntity entity = OEntities.createRequest(EdmEntitySet.newBuilder().build(), contactPropList, null);
        Transition t = customerState.getTransitions().get(0);
        LinkGenerator linkGenerator = new MultiLinkGenerator(engine, t, entity, "Contact.Email");
        MultivaluedMap<String, String> pathParameters = new MultivaluedMapImpl<String>();
        pathParameters.add("personName", "John");
        Collection<Link> links = linkGenerator.createLink(pathParameters, null, null);
        Iterator<Link> iterator = links.iterator();

        assertEquals(2, links.size());

        result = iterator.next();
        assertEquals("/baseuri/contact()?test=John+and+johnEmailAddr", result.getHref());
        assertEquals("collection", result.getRel());

        result = iterator.next();
        assertEquals("/baseuri/contact()?test=John+and+smithEmailAddr", result.getHref());
        assertEquals("collection", result.getRel());
    }

    private OComplexObject createComplexObject(String... values)
    {
        List<OProperty<?>> propertyList = new ArrayList<OProperty<?>>();
        for(int i=0; i<values.length; i+=2)
        {
            OProperty<String> property = OProperties.string(values[i], values[i+1]);
            propertyList.add(property);
        }
        OComplexObject complexObj = OComplexObjects.create(EdmComplexType.newBuilder().build(),propertyList);
        return complexObj;
    }

    private Transformer getOEntityTransformer(OCollection<?> collection)
    {
        Map<String, Object> entityProperties = new HashMap<String, Object>();
        entityProperties.put("source_Contact", collection);
        Transformer transformerMock = mock(Transformer.class);
        when(transformerMock.transform(anyObject())).thenReturn(entityProperties);
        return transformerMock;
    }

    public static class TestNote {
        String noteId;

        public TestNote(String noteId) {
            this.noteId = noteId;
        }

        public String getNoteId() {
            return noteId;
        }
    }

}
