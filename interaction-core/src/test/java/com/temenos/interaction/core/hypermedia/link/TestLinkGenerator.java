package com.temenos.interaction.core.hypermedia.link;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.hypermedia.*;
import com.temenos.interaction.core.web.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by ikarady on 06/04/2016.
 */
public class TestLinkGenerator {

    @Before
    public void setup() {
        // initialise the thread local request context with requestUri and baseUri
        RequestContext ctx = new RequestContext("/baseuri", "/requesturi", null);
        RequestContext.setRequestContext(ctx);
    }

    @Test
    public void testCreateLinkHrefSimple() {
        ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class));
        Transition t = new Transition.Builder().source(mock(ResourceState.class)).target(mockTarget("/test"))
                .build();
        LinkGenerator linkGenerator = new LinkGenerator(engine, t, null);
        Collection<Link> links = linkGenerator.createLink(null, null, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test", result.getHref());
    }

    @Test
    public void testCreateLinkHrefReplaceUsingEntity() {
        ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class), new BeanTransformer());
        Transition t = new Transition.Builder().source(mock(ResourceState.class)).target(mockTarget("/test/{noteId}"))
                .build();
        Object entity = new TestNote("123");
        LinkGenerator linkGenerator = new LinkGenerator(engine, t, entity);
        Collection<Link> links = linkGenerator.createLink(null, null, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test/123", result.getHref());
    }

    @Test
    public void testCreateLinkHrefUriParameterTokensReplaceUsingEntity() {
        ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class), new BeanTransformer());
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("test", "{noteId}");
        Transition t = new Transition.Builder().source(mock(ResourceState.class)).target(mockTarget("/test"))
                .uriParameters(uriParameters)
                .build();

        Object entity = new TestNote("123");
        LinkGenerator linkGenerator = new LinkGenerator(engine, t, entity);
        Collection<Link> links = linkGenerator.createLink(null, null, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test?test=123", result.getHref());
    }

    @Test
    public void testCreateLinkHrefUriParameterTokensReplaceQueryParameters() {
        ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class), new BeanTransformer());
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("test", "{noteId}");
        Transition t = new Transition.Builder().source(mock(ResourceState.class)).target(mockTarget("/test"))
                .uriParameters(uriParameters)
                .build();
        MultivaluedMap<String,String> queryParameters = new MultivaluedMapImpl<String>();
        queryParameters.add("noteId", "123");
        LinkGenerator linkGenerator = new LinkGenerator(engine, t, null);
        Collection<Link> links = linkGenerator.createLink(null, queryParameters, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test?test=123", result.getHref());
    }

    @Test
    public void testCreateLinkHrefUriParameterTokensReplaceQueryParametersSpecial() {
        ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class), new BeanTransformer());
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("filter", "{$filter}");
        Transition t = new Transition.Builder().source(mock(ResourceState.class)).target(mockTarget("/test"))
                .uriParameters(uriParameters)
                .build();
        MultivaluedMap<String,String> queryParameters = new MultivaluedMapImpl<String>();
        queryParameters.add("$filter", "123");
        LinkGenerator linkGenerator = new LinkGenerator(engine, t, null);
        Collection<Link> links = linkGenerator.createLink(null, queryParameters, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test?filter=123", result.getHref());
    }

    @Test
    public void testCreateLinkHrefAllQueryParameters() {
        ResourceStateMachine engine = new ResourceStateMachine(mock(ResourceState.class), new BeanTransformer());
        Transition t = new Transition.Builder().source(mock(ResourceState.class)).target(mockTarget("/test")).
                build();
        MultivaluedMap<String,String> queryParameters = new MultivaluedMapImpl<String>();
        queryParameters.add("$filter", "123");
        LinkGenerator linkGenerator = new LinkGenerator(engine, t, null);
        linkGenerator.setAllQueryParameters(true);
        Collection<Link> links = linkGenerator.createLink(new MultivaluedMapImpl<String>(), queryParameters, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/test?$filter=123", result.getHref());
    }

    @Test
    public void testCreateLinkFromDynamicResource() {
        ResourceStateMachine engineMock = Mockito.mock(ResourceStateMachine.class);
        ResourceStateAndParameters resourceStateAndParameters = new ResourceStateAndParameters();
        resourceStateAndParameters.setState(mockTarget("/testDynamic"));
        resourceStateAndParameters.setParams(new ParameterAndValue[] {new ParameterAndValue("filter2", "564")});
        DynamicResourceState dynamicResourceStateMock = mockDynamicTarget(null);
        Map<String,String> uriParameters = new HashMap<String,String>();
        uriParameters.put("filter", "{nodeId}");
        Transition t = new Transition.Builder()
                .source(mock(ResourceState.class))
                .target(dynamicResourceStateMock)
                .uriParameters(uriParameters)
                .build();
        MultivaluedMap<String,String> queryParameters = new MultivaluedMapImpl<String>();
        queryParameters.add("nodeId", "123");
        Map<String, Object> transitionProperties = new HashMap<String, Object>();
        transitionProperties.put("filter", "{nodeId}");
        transitionProperties.put("nodeId", "123");
        Mockito.when(engineMock.resolveDynamicState(
                dynamicResourceStateMock,
                transitionProperties,
                null))
                .thenReturn(resourceStateAndParameters);
        Mockito.when(engineMock.getTransitionProperties(t, null, new MultivaluedMapImpl<String>(), queryParameters)).thenReturn(transitionProperties);

        LinkGenerator linkGenerator = new LinkGenerator(engineMock, t, null);
        linkGenerator.setAllQueryParameters(true);
        Collection<Link> links = linkGenerator.createLink(new MultivaluedMapImpl<String>(), queryParameters, null);
        Link result = (!links.isEmpty()) ? links.iterator().next() : null;
        assertEquals("/baseuri/testDynamic?filter=123&filter2=564", result.getHref());
    }

    private ResourceState mockTarget(String path) {
        ResourceState target = mock(ResourceState.class);
        when(target.getPath()).thenReturn(path);
        when(target.getRel()).thenReturn("collection");
        return target;
    }

    private DynamicResourceState mockDynamicTarget(String path) {
        DynamicResourceState target = mock(DynamicResourceState.class);
        when(target.getPath()).thenReturn(path);
        return target;
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
