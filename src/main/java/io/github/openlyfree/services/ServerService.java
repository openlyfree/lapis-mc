package io.github.openlyfree.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.github.openlyfree.models.Server;
import io.github.openlyfree.resources.ServerConsoleSocket;
import io.github.openlyfree.services.loader.FabricService;
import io.github.openlyfree.services.loader.PaperService;
import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
public class ServerService {
  @Inject
  @RestClient
  FabricService fabric;
  @Inject
  @RestClient
  PaperService paper;
  @Inject
  Server.Repo serpo;

  public ConcurrentHashMap<String, Process> running = new ConcurrentHashMap<>();

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void updateState(Server server, Server.ServerState state) {
    server.state = state;
    serpo.update("state = ?1 where id = ?2", state, server.id);
  }

  private Optional<InputStream> getDownJarStream(Server server) {
    return switch (server.loaderType) {
      case FABRIC -> {
        if (!fabric.getGameVersions().stream()
            .anyMatch(v -> v.version().equals(server.version)))
          yield Optional.empty();

        String loader = fabric.getLoaderVersions().get(0).version();
        String installer = fabric.getInstallerVersions().get(0).version();

        yield Optional.ofNullable(fabric.downloadJar(server.version, loader, installer));
      }
      case PAPER -> {
        if (!paper.getVersions().versions().stream()
            .anyMatch(v -> v.version().id().equals(server.version)))
          yield Optional.empty();

        PaperService.PaperBuildResponse buildData = paper.getBuildData(server.version);
        PaperService.Download serverDownload = buildData.downloads().get("server:default");

        if (serverDownload == null || serverDownload.url() == null || serverDownload.url().isBlank()) {
          yield Optional.empty();
        }

        try {
          yield Optional.of(URI.create(serverDownload.url()).toURL().openStream());
        } catch (Exception e) {
          throw new WebApplicationException("Paper download URL could not be opened: " + e.getMessage(),
              Status.BAD_GATEWAY);
        }
      }
    };
  }

  @Transactional
  public void downloadJar(Server server) {

    updateState(server, Server.ServerState.INIT);
    try (InputStream in = getDownJarStream(server)
        .orElseThrow(() -> new NotFoundException("Version not found: " + server.version))) {

      Path serverDir = Path.of("server_jars");
      if (Files.notExists(serverDir)) {
        Files.createDirectories(serverDir);
      }

      String fileName = String.format("%s_%s.jar",
          server.loaderType.name().toLowerCase(),
          server.version.replace(".", "_"));

      Files.copy(in, serverDir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } catch (NotFoundException e) {
      updateState(server, Server.ServerState.ERR);
      throw e;
    } catch (WebApplicationException e) {
      updateState(server, Server.ServerState.ERR);
      if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
        throw new NotFoundException("No server artifact found for " + server.loaderType + " " + server.version, e);
      }
      throw new WebApplicationException("Failed to download server artifact from upstream", Status.BAD_GATEWAY);
    } catch (Exception e) {
      updateState(server, Server.ServerState.ERR);
      throw new RuntimeException("Failed to download JAR for " + server.name, e);
    }
    updateState(server, Server.ServerState.DOWN);
  }

  @VirtualThreads
  @Transactional
  public void runServer(Server server) {
    Path serverDir = Path.of("server_jars");
    String fileName = String.format("%s_%s.jar",
        server.loaderType.name().toLowerCase(),
        server.version.replace(".", "_"));
    Path jarPath = serverDir.resolve(fileName).toAbsolutePath();

    if (Files.notExists(jarPath)) {
      updateState(server, Server.ServerState.ERR);
      throw new IllegalAccessError("JAR file not found for server: " + server.name);
    }

    try {
      ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath.toString(), "nogui");
      var f = Path.of(".").resolve("servers/" + server.name.replace(" ", "_")).toFile();
      if (!f.exists())
        f.mkdirs();
      Files.writeString(f.toPath().resolve("eula.txt"), "eula=true", StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
      pb.directory(f);
      running.put(server.name, pb.start());
    } catch (Exception e) {
      updateState(server, Server.ServerState.ERR);
      throw new RuntimeException("Failed to run server " + server.name, e);
    }
    Thread.ofVirtual().start(() -> {
      try (var reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(running.get(server.name).getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          ServerConsoleSocket.broadcast(server.id, line);
        }
      } catch (IOException e) {
        ServerConsoleSocket.broadcast(server.id, "[Lapis] Console stream closed.");
      }
    });
    updateState(server, Server.ServerState.UP);
  }

  @VirtualThreads
  @Transactional
  public void stopServer(Server server) {
    Process p = running.get(server.name);
    if (p != null) {
      p.destroy();
      running.remove(server.name);
    }

    updateState(server, Server.ServerState.DOWN);
  }

  @PreDestroy
  void onStop() {
    running.values().forEach(p -> {
      try {
        p.getOutputStream().write("stop\n".getBytes());
        p.getOutputStream().flush();
      } catch (Exception e) {

        p.destroyForcibly();

      }
    });
  }

  public void sendCommand(long long1, String message) {
    Process p = running
        .get(serpo.findByIdOptional(long1).orElseThrow(() -> new IllegalArgumentException("Server not found")).name);
    if (p != null && p.isAlive()) {
      try {
        p.getOutputStream().write((message + "\n").getBytes());
        p.getOutputStream().flush();
      } catch (IOException e) {
        throw new RuntimeException("Failed to send command to server", e);
      }
    } else {
      throw new IllegalStateException("Server is not running");
    }
  }

}
