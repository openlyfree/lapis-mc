function getServers() {
  fetch("/api/server")
    .then((response) => response.json())
    .then((data) => {
      const serverBar = document.getElementById("serverBar");
      if (serverBar) {
        serverBar.innerHTML = "";
        data.forEach((server: { name: string }) => {
          const serverElement = document.createElement("div");
          serverElement.textContent = server.name;
          serverBar.appendChild(serverElement);
        });
      }
    })
    .catch((error) => console.error("Error fetching servers:", error));1
}

export { getServers };