package io.github.openlyfree.resources;

import java.util.List;

import io.github.openlyfree.models.Server;
import io.github.openlyfree.services.ServerService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;

@Path("api/server")
public class ServerResource {
  @Inject
  Server.Repo serverRepo;
  @Inject
  ServerService serverService;

  @GET
  public List<Server> listServers() {
    return serverRepo.listAll();
  }

  @POST
  @Transactional
  public Server createServer(Server server) {
    serverService.downloadJar(server);
    serverRepo.persist(server);
    return server;
  }

  @POST
  @Path("{serverId}/start")
  public Server.ServerState startServer(@PathParam("serverId") String serverId) {
    Server server = serverRepo.findById(Long.parseLong(serverId));
    if (server == null)
      throw new WebApplicationException("Server not found", 404);
    serverService.runServer(server);
    return server.state;
  }

  @POST
  @Path("{serverId}/stop")
  public Server.ServerState stopServer(@PathParam("serverId") String serverId) {
    Server server = serverRepo.findById(Long.parseLong(serverId));
    if (server == null)
      throw new WebApplicationException("Server not found", 404);
    serverService.stopServer(server);
    return server.state;
  }

  @DELETE
  @Path("{serverId}")
  @Transactional
  public void deleteServer(@PathParam("serverId") String serverId) {
    Server server = serverRepo.findById(Long.parseLong(serverId));
    if (server == null)
      throw new WebApplicationException("Server not found", 404);
    serverRepo.delete(server);
  }

}
