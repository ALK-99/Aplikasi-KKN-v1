const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("kkn", {
  simpanPdf: (html, nama) => ipcRenderer.invoke("simpan-pdf", html, nama),
  versi: () => ipcRenderer.invoke("versi"),
  mulaiOtomatis: (aktif) => aktif === undefined ? ipcRenderer.invoke("mulai-otomatis-baca") : ipcRenderer.invoke("mulai-otomatis-atur", aktif),
  pasangPembaruan: (url) => ipcRenderer.invoke("pasang-pembaruan", url),
});
