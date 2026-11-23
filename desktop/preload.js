const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("kkn", {
  simpanPdf: (html, nama) => ipcRenderer.invoke("simpan-pdf", html, nama),
});
