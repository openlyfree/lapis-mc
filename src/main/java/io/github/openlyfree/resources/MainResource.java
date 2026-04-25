package io.github.openlyfree.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class MainResource {
  @GET
  public String hello() {
    return "lapis mc";
  }
}
