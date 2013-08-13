package org.activityinfo.server.endpoint.odk;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.activityinfo.shared.auth.AuthenticatedUser;
import org.activityinfo.shared.command.GetSchema;

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.view.Viewable;

@Path("/formList")
public class FormListResource extends ODKResource {
    @GET
    @Produces(MediaType.TEXT_XML)
    public Response formList(@InjectParam AuthenticatedUser user, @Context UriInfo info) throws Exception {
        // if (AuthenticatedUser.isAnonymous(user)) {
        // return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Activityinfo\"").build();
        // }
        authorize();
        LOGGER.finer("ODK formlist requested by " + user.getEmail());

        Map<String, Object> map = Maps.newHashMap();
        map.put("schema", dispatcher.execute(new GetSchema()));
        map.put("host", info.getBaseUri().toString());

        return Response.ok(new Viewable("/odk/formList.ftl", map)).build();
    }
}
