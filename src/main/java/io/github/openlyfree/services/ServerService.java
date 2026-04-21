package io.github.openlyfree.services;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.github.openlyfree.models.Server;
import io.github.openlyfree.services.loader.FabricService;
import io.github.openlyfree.services.loader.PaperService;
import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServerService {
  @Inject
  @RestClient
  FabricService fabric;
  @Inject
  @RestClient
  PaperService paper;

  ConcurrentHashMap<String, Process> running = new ConcurrentHashMap<>();

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

        yield Optional.ofNullable(paper.downloadJar(server.version));
      }
    };
  }

  public void downloadJar(Server server) {
    try (InputStream in = getDownJarStream(server).orElseThrow(() -> new RuntimeException("Version not found"))) {

      Path serverDir = Path.of("server_jars");
      if (Files.notExists(serverDir)) {
        Files.createDirectories(serverDir);
      }

      String fileName = String.format("%s_%s.jar",
          server.loaderType.name().toLowerCase(),
          server.version.replace(".", "_"));

      Files.copy(in, serverDir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      throw new RuntimeException("Failed to download JAR for " + server.name, e);
    }
  }

  @VirtualThreads
  public void runServer(Server server) {
    Path serverDir = Path.of("server_jars");
    String fileName = String.format("%s_%s.jar",
        server.loaderType.name().toLowerCase(),
        server.version.replace(".", "_"));
    Path jarPath = serverDir.resolve(fileName).toAbsolutePath();

    if (Files.notExists(jarPath))
      downloadJar(server);

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
      throw new RuntimeException("Failed to run server " + server.name, e);
    }
  }

  @VirtualThreads
  public void stopServer(Server name) {
    Process p = running.get(name.name);
    if (p != null) {
      p.destroy();
      running.remove(name.name);
    }
  }

}