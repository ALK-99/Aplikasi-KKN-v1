const { app, BrowserWindow, Menu, Tray, shell, ipcMain, dialog } = require("electron");
const path = require("path");
const fs = require("fs");
const os = require("os");
const https = require("https");
const { spawn } = require("child_process");

const IKON = path.join(__dirname, "icon.png");
let jendela = null, tray = null, benarKeluar = false;

// Hanya satu jendela aplikasi; membuka lagi akan memunculkan yang sudah ada.
if (!app.requestSingleInstanceLock()) { app.quit(); }
app.on("second-instance", () => tampilkan());

function tampilkan() {
  if (!jendela) return buka();
  if (jendela.isMinimized()) jendela.restore();
  jendela.show(); jendela.focus();
}

function buka() {
  const win = jendela = new BrowserWindow({
    icon: IKON,
    show: !process.argv.includes("--tersembunyi"),
    width: 1360, height: 860, minWidth: 980, minHeight: 640,
    title: "KKN Al-Mushlih 2027",
    backgroundColor: "#0d0f1e",
    webPreferences: { contextIsolation: true, sandbox: true, backgroundThrottling: false, preload: path.join(__dirname, "preload.js") },
  });
  Menu.setApplicationMenu(null);
  win.loadFile(path.join(__dirname, "app", "index.html"));
  // Tombol X hanya menyembunyikan ke baki sistem supaya pengingat tetap jalan.
  win.on("close", (e) => { if (!benarKeluar) { e.preventDefault(); win.hide(); } });
  win.on("closed", () => { jendela = null; });
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
    const pdf = await tersembunyi.webContents.printToPDF({ pageSize: "A4", printBackground: true, preferCSSPageSize: true });
    fs.writeFileSync(tujuan.filePath, pdf);
    return true;
  } finally { tersembunyi.destroy(); }
});

ipcMain.handle("versi", () => app.getVersion());

// Mulai otomatis bersama Windows (tersembunyi di baki sistem).
ipcMain.handle("mulai-otomatis-baca", () => app.getLoginItemSettings().openAtLogin);
ipcMain.handle("mulai-otomatis-atur", (e, aktif) => {
  app.setLoginItemSettings({ openAtLogin: !!aktif, args: ["--tersembunyi"] });
  return app.getLoginItemSettings().openAtLogin;
});

// Unduh pemasang versi baru dari GitHub lalu jalankan. Hanya alamat resmi repo ini yang diterima.
const BOLEH_URL = /^https:\/\/github\.com\/ALK-99\/Aplikasi-KKN-v1\/releases\/download\/pc-v[\d.]+\/[\w.\-]*Setup[\w.\-]*\.exe$/i;
function unduh(url, tujuan, sisaPindah = 5) {
  return new Promise((ok, gagal) => {
    https.get(url, { headers: { "User-Agent": "KKN-PC" } }, (res) => {
      if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location && sisaPindah > 0) {
        res.resume();
        const baru = new URL(res.headers.location, url);
        if (baru.protocol !== "https:") return gagal(new Error("Alamat tidak aman"));
        return unduh(baru.href, tujuan, sisaPindah - 1).then(ok, gagal);
      }
      if (res.statusCode !== 200) { res.resume(); return gagal(new Error("Unduhan gagal (" + res.statusCode + ")")); }
      const f = fs.createWriteStream(tujuan);
      res.pipe(f);
      f.on("finish", () => f.close(ok));
      f.on("error", gagal);
    }).on("error", gagal);
  });
}
ipcMain.handle("pasang-pembaruan", async (e, url) => {
  if (!BOLEH_URL.test(String(url))) throw new Error("Alamat pembaruan tidak dikenali");
  const berkas = path.join(os.tmpdir(), "KKN-Pembaruan-Setup.exe");
  await unduh(url, berkas);
  spawn(berkas, [], { detached: true, stdio: "ignore" }).unref();
  benarKeluar = true;
  setTimeout(() => app.quit(), 500);
  return true;
});

app.whenReady().then(() => {
  buka();
  try {
    tray = new Tray(IKON);
    tray.setToolTip("KKN Al-Mushlih 2027");
    tray.setContextMenu(Menu.buildFromTemplate([
      { label: "Buka aplikasi", click: tampilkan },
      { label: "Keluar sepenuhnya", click: () => { benarKeluar = true; app.quit(); } },
    ]));
    tray.on("click", tampilkan);
  } catch (e) { /* ikon baki tidak wajib */ }
});
app.on("before-quit", () => { benarKeluar = true; });
app.on("window-all-closed", () => { /* tetap hidup di baki sistem */ });
