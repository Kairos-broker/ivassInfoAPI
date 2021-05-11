package com.cristigutzu.ivassInfoAPI;

import com.cristigutzu.ivassInfoAPI.privateAPI.business.Processor;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

@Path("/")
public class MainEndpoint {
    @GET
    @Path("/resource/{id}")
    @Produces({"application/json"})
    public Response get(@PathParam("id") String id, @Context Request request) {
        Processor proc = new Processor(id);
        CacheControl cc = new CacheControl();
        cc.setMaxAge(2147483647);
        EntityTag etag = new EntityTag(Integer.toString(proc.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
        if (builder == null) {
            builder = Response.ok(proc.getData());
            builder.tag(etag);
        }
        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path("/seller/{dealerIVASSId}/{usernameSeller}")
    @Produces({"application/json"})
    public Response get(@PathParam("dealerIVASSId") String dealerIVASSId, @PathParam("usernameSeller") String usernameSeller, @Context Request request) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(2147483647);
        EntityTag etag = new EntityTag(Integer.toString((dealerIVASSId + usernameSeller).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
        if (builder == null) {
            builder = Response.ok(Processor.getSellerData(dealerIVASSId, usernameSeller));
            builder.tag(etag);
        }
        builder.cacheControl(cc);
        return builder.build();
    }
}
