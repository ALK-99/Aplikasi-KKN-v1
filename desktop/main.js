const { app, BrowserWindow, Menu, shell, ipcMain, dialog } = require("electron");
const path = require("path");
const fs = require("fs");

function buka() {
  const win = new BrowserWindow({
    width: 1360, height: 860, minWidth: 980, minHeight: 640,
    title: "KKN Al-Mushlih 2027",
    backgroundColor: "#0d0f1e",
    webPreferences: { contextIsolation: true, sandbox: true, preload: path.join(__dirname, "preload.js") },
  });
  Menu.setApplicationMenu(null);
  win.loadFile(path.join(__dirname, "app", "index.html"));
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: "deny" }; });
}

// Simpan HTML (surat) menjadi PDF A4 lewat dialog simpan.
ipcMain.handle("simpan-pdf", async (event, html, nama) => {
  const induk = BrowserWindow.fromWebContents(event.sender);
  const tujuan = await dialog.showSaveDialog(induk, { defaultPath: nama, filters: [{ name: "PDF", extensions: ["pdf"] }] });
  if (tujuan.canceled || !tujuan.filePath) return false;
  const tersembunyi = new BrowserWindow({ show: false, webPreferences: { sandbox: true } });
  try {
    await tersembunyi.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(html));
    const pdf = await tersembunyi.webContents.printToPDF({ pageSize: "A4", printBackground: true });
    fs.writeFileSync(tujuan.filePath, pdf);
    return true;
  } finally { tersembunyi.destroy(); }
});

app.whenReady().then(buka);
app.on("window-all-closed", () => app.quit());
