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

const HOST_AI = ["generativelanguage.googleapis.com", "openrouter.ai"];
ipcMain.handle("http-kirim", (e, url, header, badan, batasMs) => new Promise((selesai) => {
  let u; try { u = new URL(url); } catch (x) { return selesai({galat: "URL tidak valid"}); }
  if (u.protocol !== "https:" || !HOST_AI.includes(u.hostname)) return selesai({galat: "Alamat tidak diizinkan"});
  const req = https.request({hostname: u.hostname, path: u.pathname + u.search, method: "POST", headers: Object.assign({}, header, {"Content-Length": Buffer.byteLength(badan || "")}), timeout: Math.min(+batasMs || 60000, 120000)}, (r) => {
    const bag = []; r.on("data", (c) => bag.push(c)); r.on("end", () => selesai({status: r.statusCode, teks: Buffer.concat(bag).toString("utf8")}));
    r.on("error", (x) => selesai({galat: x.message}));
  });
  req.on("timeout", () => { req.destroy(); selesai({galat: "Waktu habis"}); });
  req.on("error", (x) => selesai({galat: x.message}));
  req.end(badan || "");
}));
// Hapus data lokal (login, cache, ingatan asisten, konfigurasi) dan, bila diminta, copot aplikasi.
ipcMain.handle("hapus-data", async (e, copot) => {
  try { app.setLoginItemSettings({ openAtLogin: false }); } catch (x) {}
  const data = app.getPath("userData");
  const ses = jendela ? jendela.webContents.session : null;
  if (ses) { try { await ses.clearStorageData(); await ses.clearCache(); } catch (x) {} }
  const bersihkan = () => { // setelah aplikasi tertutup, hapus sisa folder data
    try { spawn("cmd.exe", ["/c", `ping -n 4 127.0.0.1 >nul & rmdir /s /q "${data}"`], { detached: true, stdio: "ignore", windowsHide: true }).unref(); } catch (x) {}
  };
  benarKeluar = true;
  if (!copot) { app.relaunch(); app.exit(0); return "ok"; } // storage sudah dikosongkan di atas; tanpa rmdir agar tidak berbalapan dengan instance baru
  const pencopot = path.join(path.dirname(process.execPath), `Uninstall ${app.getName()}.exe`);
  const alternatif = fs.existsSync(path.dirname(process.execPath)) ? fs.readdirSync(path.dirname(process.execPath)).find((f) => /^Uninstall.*\.exe$/i.test(f)) : null;
  const berkas = fs.existsSync(pencopot) ? pencopot : alternatif ? path.join(path.dirname(process.execPath), alternatif) : null;
  if (berkas && !process.env.PORTABLE_EXECUTABLE_FILE) {
    spawn(berkas, [], { detached: true, stdio: "ignore" }).unref(); bersihkan(); app.exit(0); return "dicopot";
  }
  bersihkan(); setTimeout(() => app.exit(0), 300); return "manual"; // versi portabel: hapus berkas .exe sendiri
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
