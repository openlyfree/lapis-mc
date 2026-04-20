package io.github.openlyfree.loader;

import java.io.InputStream;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/v2")
@RegisterRestClient(configKey = "fabric-api")
public interface FabricService {

  public record FabricGameVersion(
      String version,
      boolean stable) {
  }

  public record FabricLoaderVersion(
      String version,
      String maven,
      int build,
      boolean stable,
      String separator) {
  }

  public record FabricInstallerVersion(
      String url,
      String maven,
      String version,
      boolean stable) {
  }

  @GET
  @Path("/versions/game")
  public List<FabricGameVersion> getGameVersions();

  @GET
  @Path("/versions/loader")
  public List<FabricLoaderVersion> getLoaderVersions();

  @GET
  @Path("/versions/installer")
  public List<FabricInstallerVersion> getInstallerVersions();

  @GET
  @Path("/versions/loader/{gameVersion}/{loaderVersion}/{installerVersion}/server/jar")
  InputStream downloadJar(@PathParam("gameVersion") String gameVersion,
      @PathParam("loaderVersion") String loaderVersion, @PathParam("installerVersion") String installerVersion);

}
