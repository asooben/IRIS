package com.temenos.interaction.core.hypermedia.link;

import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.hypermedia.*;
import com.temenos.interaction.core.web.RequestContext;
import org.odata4j.core.OEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


/**
 * Created by ikarady on 05/04/2016.
 */
public interface LinkGenerator {

    public Collection<Link> createLink(MultivaluedMap<String, String> pathParameters,
            MultivaluedMap<String, String> queryParameters, InteractionContext ctx);

}
