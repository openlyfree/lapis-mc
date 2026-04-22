package io.github.openlyfree.models;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;

@Entity
public class Server extends PanacheEntity {
  public Server() {}
  
  public String name;
  @Transient
  public int pid;
  @Enumerated(EnumType.STRING)
  public LoaderType loaderType;
  @Enumerated(EnumType.STRING)
  public ServerState state;
  public String version; // maybe a version class later?

  public enum LoaderType {
    FABRIC, PAPER// in future custom type maybe?
  }

  public enum ServerState {
    UP, DOWN, INIT, ERR
  }

  public interface Repo extends PanacheRepository<Server> {

  }
}