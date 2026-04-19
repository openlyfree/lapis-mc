package io.github.openlyfree.loader;

import java.io.InputStream;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/v2")
@RegisterRestClient(configKey = "fabric-api")
public interface FabricService {

  @GET
  @Path("/versions/game")
  public String getGameVersions();

  @GET
  @Path("/versions/loader")
  public String getLoaderVersions();

  @GET
  @Path("/versions/installer")
  public String getInstallerVersions();

  @GET
  @Path("/versions/loader/{gameVersion}/{loaderVersion}/{installerVersion}/server/jar")
  InputStream downloadJar(@PathParam("gameVersion") String gameVersion,
      @PathParam("loaderVersion") String loaderVersion, @PathParam("installerVersion") String installerVersion);

}
