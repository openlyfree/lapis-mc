package io.github.openlyfree.resources;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.openlyfree.services.ServerService;
import io.quarkus.websockets.next.Connection;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import jakarta.inject.Inject;

@WebSocket(path = "/api/server/{serverId}/console")
public class ServerConsoleSocket {

  @Inject
  ServerService serser;

  public static final ConcurrentHashMap<Long, Set<Connection>> SESSIONS = new ConcurrentHashMap<>();

  @OnOpen
  public void onOpen(@PathParam String serverId, Connection conn) {
    SESSIONS.computeIfAbsent(Long.parseLong(serverId), k -> ConcurrentHashMap.newKeySet()).add(conn);
    // Optionally send a "Connected" message
    conn.sendTextAndAwait("Connected to server " + serverId);
  }

  @OnClose
  public void onClose(@PathParam String serverId, Connection conn) {
    Set<Connection> set = SESSIONS.get(Long.parseLong(serverId));
    if (set != null)
      set.remove(conn);
  }

  @OnTextMessage
  public void onMessage(String message, @PathParam String serverId) {
    serser.sendCommand(Long.parseLong(serverId), message);
  }

  public static void broadcast(Long serverId, String message) {
    Set<Connection> sessions = SESSIONS.get(serverId);
    if (sessions != null) {
      sessions.forEach(c -> c.sendTextAndAwait(message));
    }
  }
}