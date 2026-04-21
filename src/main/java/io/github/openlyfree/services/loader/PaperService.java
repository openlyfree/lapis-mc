package io.github.openlyfree.services.loader;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@RegisterRestClient(configKey = "paper-api")
@Path("/v3/projects/paper")
public interface PaperService {

  public record PaperVersionsResponse(List<VersionEntry> versions) {
  }

  public record VersionEntry(
      Version version,
      List<Integer> builds) {
  }

  public record Version(
      String id,
      Support support,
      Java java) {
  }

  public record Support(
      String status,
      String end) {
  }

  public record Java(
      JavaVersion version,
      JavaFlags flags) {
  }

  public record JavaVersion(int minimum) {
  }

  public record JavaFlags(List<String> recommended) {
  }

  public record PaperBuildResponse(
      int id,
      OffsetDateTime time,
      String channel,
      List<Commit> commits,
      Map<String, Download> downloads) {
  }

  public record Commit(
      String sha,
      OffsetDateTime time,
      String message) {
  }

  public record Download(
      String name,
      Checksums checksums,
      long size,
      String url) {
  }

  public record Checksums(
      String sha256) {
  }

  @GET
  @Path("/versions")
  PaperVersionsResponse getVersions();

  @GET
  @Path("/versions/{version}/builds/latest")
  PaperBuildResponse getBuildData(@PathParam("version") String version);

  @GET
  @Path("{url}")
  InputStream downloadJar(@PathParam("url") String url);

}