const { app, BrowserWindow, Menu, shell } = require("electron");
const path = require("path");

function buka() {
  const win = new BrowserWindow({
    width: 1280, height: 800, minWidth: 900, minHeight: 600,
    title: "KKN Al-Mushlih 2027",
    backgroundColor: "#0f1120",
    webPreferences: { contextIsolation: true, sandbox: true },
  });
  Menu.setApplicationMenu(null);
  win.loadFile(path.join(__dirname, "app", "index.html"));
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: "deny" }; });
}
app.whenReady().then(buka);
app.on("window-all-closed", () => app.quit());
