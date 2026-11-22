package com.kkn.almufid.sekretaris.ui.developer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.ui.components.AvatarGradien
import com.kkn.almufid.sekretaris.ui.components.BatangProgres
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.KosongState
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.components.TagKecil
import com.kkn.almufid.sekretaris.ui.components.inisialDari
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.theme.*

private val ISI_HALAMAN = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 32.dp)

/**
 * One Eye: mata developer atas seluruh data peserta. Tiga tab (Ringkasan,
 * Peserta, Aktivitas) dan satu halaman rinci per peserta.
 */
@Composable
fun OneEyeScreen(vm: DeveloperViewModel) {
    val data by vm.data.collectAsState()
    val status by vm.status.collectAsState()
    val peserta = remember(data) { susunPeserta(data) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var dipilih by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.muat() }
    BackHandler(enabled = dipilih != null) { dipilih = null }

    val terpilih = peserta.firstOrNull { it.profil.uid == dipilih }
    Column(Modifier.fillMaxSize()) {
        if (terpilih == null) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Ringkasan", "Peserta", "Aktivitas").forEachIndexed { i, t ->
                    PilChip(t, tab == i, { tab = i })
                }
            }
            when (tab) {
                0 -> TabRingkasan(data, peserta, status.sedangMuat, Modifier.weight(1f))
                1 -> TabPeserta(peserta, Modifier.weight(1f)) { dipilih = it }
                else -> TabAktivitas(data, Modifier.weight(1f))
            }
        } else {
            DetailPeserta(terpilih, data, status.admin, vm, Modifier.weight(1f)) { dipilih = null }
        }
    }
}

// ---------------------------------------------------------------- Ringkasan

@Composable
private fun TabRingkasan(
    data: DataDeveloper, peserta: List<RingkasanPeserta>, memuat: Boolean, modifier: Modifier
) {
    val sekarang = remember(data) { System.currentTimeMillis() }
    val n = data.profil.size
    val aktif7 = data.profil.count { it.terakhirAktifMillis >= sekarang - 7 * SATU_HARI }
    val diblokir = data.profil.count { it.akunDiblokir }
    val tugasSelesai = data.tugas.count { it.selesai }
    val saldo = data.kas.filter { it.masuk }.sumOf { it.jumlah } - data.kas.filter { !it.masuk }.sumOf { it.jumlah }
    val terlambat = data.tugas.filter { !it.selesai && it.deadlineMillis + SATU_HARI < sekarang }
    val persenBaca = if (data.pengumuman.isEmpty() || n == 0) "-"
        else "${(data.pengumuman.sumOf { it.dibacaOleh.size } * 100 / (data.pengumuman.size * n)).coerceAtMost(100)}%"
    val persenPolling = if (data.polling.isEmpty() || n == 0) "-"
        else "${(data.polling.sumOf { it.suara.size } * 100 / (data.polling.size * n)).coerceAtMost(100)}%"

    val perDesa = remember(peserta) { kelompokkan(peserta) { it.desa } }
    val perDivisi = remember(peserta) { kelompokkan(peserta) { it.divisi } }
    val teratas = remember(peserta) { peserta.filter { it.adalahPeserta }.sortedByDescending { it.skor }.take(5) }

    val aktifPeserta = peserta.filter { it.adalahPeserta && !it.profil.akunDiblokir }
    val belumJurnal = aktifPeserta.filter { it.jurnal.isEmpty() }.map { it.profil.namaLengkap }
    val terbaru = data.pengumuman.firstOrNull()
    val belumBaca = if (terbaru == null) emptyList()
        else aktifPeserta.filter { it.profil.uid !in terbaru.dibacaOleh }.map { it.profil.namaLengkap }
    val pasif = aktifPeserta.filter { it.profil.terakhirAktifMillis in 1 until (sekarang - 7 * SATU_HARI) }
        .map { it.profil.namaLengkap }

    LazyColumn(modifier, contentPadding = ISI_HALAMAN, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (memuat && n == 0) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VioletTerang)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka("$n/20", "Peserta", Modifier.weight(1f))
                KartuAngka("$aktif7", "Aktif 7 hari", Modifier.weight(1f))
                KartuAngka("$diblokir", "Diblokir", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka("${data.jurnal.size}", "Jurnal bersama", Modifier.weight(1f))
                KartuAngka("$tugasSelesai/${data.tugas.size}", "Tugas selesai", Modifier.weight(1f))
                KartuAngka(formatRupiah(saldo), "Saldo kas", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka(persenBaca, "Pengumuman dibaca", Modifier.weight(1f))
                KartuAngka(persenPolling, "Ikut polling", Modifier.weight(1f))
                KartuAngka("${terlambat.size}", "Tugas terlambat", Modifier.weight(1f))
            }
        }

        item { JudulBagian("Per desa", "Jumlah peserta, jurnal, dan tugas selesai") }
        item { KelompokKartu(perDesa) }
        item { JudulBagian("Per divisi") }
        item { KelompokKartu(perDivisi) }

        item { JudulBagian("Peringkat keaktifan", "Jurnal ×3, tugas selesai ×2, pengumuman dibaca, suara polling") }
        item {
            KartuGelap(Modifier.fillMaxWidth()) {
                if (teratas.isEmpty()) Text("Belum ada data.", fontSize = 12.sp, color = TeksRedup)
                teratas.forEachIndexed { i, r ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${i + 1}", fontWeight = FontWeight.ExtraBold, color = if (i == 0) Gold else TeksRedup, modifier = Modifier.width(24.dp))
                        Text(
                            r.profil.namaLengkap.ifBlank { "(tanpa nama)" }, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text("${r.skor} poin", fontSize = 12.sp, color = Mint, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { JudulBagian("Perlu perhatian") }
        if (belumJurnal.isEmpty() && belumBaca.isEmpty() && pasif.isEmpty() && terlambat.isEmpty()) {
            item {
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text("Semua aman. Tidak ada yang perlu ditindaklanjuti.", fontSize = 13.sp, color = Mint)
                }
            }
        }
        if (belumJurnal.isNotEmpty()) item {
            CatatanPerhatian("Belum pernah menulis jurnal bersama (${belumJurnal.size})", ringkasNama(belumJurnal))
        }
        if (terlambat.isNotEmpty()) item {
            CatatanPerhatian(
                "Tugas lewat tenggat (${terlambat.size})",
                ringkasNama(terlambat.map { "${it.judul} — ${it.untukNama}" }, 5)
            )
        }
        if (belumBaca.isNotEmpty()) item {
            CatatanPerhatian("Belum membaca pengumuman terbaru (${belumBaca.size})", ringkasNama(belumBaca))
        }
        if (pasif.isNotEmpty()) item {
            CatatanPerhatian("Tidak membuka app lebih dari 7 hari (${pasif.size})", ringkasNama(pasif))
        }
    }
}

@Composable
private fun CatatanPerhatian(judul: String, isi: String) {
    KartuGelap(Modifier.fillMaxWidth()) {
        Text(judul, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(4.dp))
        Text(isi, fontSize = 12.sp, color = TeksSoft, lineHeight = 17.sp)
    }
}

@Composable
private fun KelompokKartu(daftar: List<KelompokStat>) {
    val puncak = (daftar.maxOfOrNull { it.jurnal } ?: 0).coerceAtLeast(1)
    KartuGelap(Modifier.fillMaxWidth()) {
        if (daftar.isEmpty()) Text("Belum ada data.", fontSize = 12.sp, color = TeksRedup)
        daftar.forEachIndexed { i, k ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(k.nama, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${k.peserta} orang", fontSize = 11.sp, color = TeksRedup)
            }
            Spacer(Modifier.height(4.dp))
            BatangProgres(k.jurnal.toFloat() / puncak)
            Spacer(Modifier.height(4.dp))
            Text("${k.jurnal} jurnal · tugas ${k.tugasSelesai}/${k.tugasTotal}", fontSize = 11.sp, color = TeksRedup)
        }
    }
}

// ------------------------------------------------------------------ Peserta

@Composable
private fun TabPeserta(peserta: List<RingkasanPeserta>, modifier: Modifier, onPilih: (String) -> Unit) {
    var cari by rememberSaveable { mutableStateOf("") }
    var urut by rememberSaveable { mutableStateOf(0) }
    val sekarang = remember(peserta) { System.currentTimeMillis() }
    val tampil = remember(peserta, cari, urut) {
        val q = cari.trim().lowercase()
        val saring = peserta.filter {
            q.isEmpty() || listOf(it.profil.namaLengkap, it.profil.nim, it.profil.desa, it.profil.divisi, it.profil.email)
                .any { f -> f.lowercase().contains(q) }
        }
        when (urut) {
            1 -> saring.sortedByDescending { it.skor }
            2 -> saring.sortedByDescending { it.profil.terakhirAktifMillis }
            else -> saring.sortedBy { it.profil.namaLengkap.lowercase() }
        }
    }
    LazyColumn(modifier, contentPadding = ISI_HALAMAN, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = cari, onValueChange = { cari = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari nama, NIM, desa, divisi") }
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Nama", "Paling aktif", "Terakhir aktif").forEachIndexed { i, t ->
                    PilChip(t, urut == i, { urut = i })
                }
            }
        }
        if (tampil.isEmpty()) item { KosongState("Tidak ada peserta yang cocok.") }
        items(tampil) { r ->
            val p = r.profil
            KartuGelap(Modifier.fillMaxWidth().tekanPegas { onPilih(p.uid) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarGradien(inisialDari(p.namaLengkap), 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.namaLengkap.ifBlank { "(tanpa nama)" }, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOf(p.nim, p.desa, p.divisi).filter { it.isNotBlank() }.joinToString(" · "),
                            fontSize = 11.sp, color = TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Jurnal ${r.jurnal.size} · Tugas ${r.tugasSelesai}/${r.tugas.size} · Baca ${r.pengumumanDibaca}/${r.pengumumanTotal}",
                            fontSize = 11.sp, color = TeksSoft
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (p.akunDiblokir) TagKecil("Diblokir", Merah)
                        else if (!r.adalahPeserta) TagKecil("Developer", Gold)
                        Text(lalu(p.terakhirAktifMillis, sekarang), fontSize = 10.sp, color = TeksRedup)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- Detail rinci

@Composable
private fun DetailPeserta(
    r: RingkasanPeserta, data: DataDeveloper, admin: Boolean, vm: DeveloperViewModel,
    modifier: Modifier, onTutup: () -> Unit
) {
    val p = r.profil
    val adalahSaya = p.uid == vm.uidSaya
    val belumDibaca = data.pengumuman.filter { p.uid !in it.dibacaOleh }

    LazyColumn(modifier, contentPadding = ISI_HALAMAN, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "← Kembali ke daftar", color = VioletTerang, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.tekanPegas(onTutup).padding(vertical = 4.dp)
            )
        }
        item {
            KartuGelap(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarGradien(inisialDari(p.namaLengkap), 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.namaLengkap.ifBlank { "(tanpa nama)" }, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            (if (Peran.dariString(p.peran) == Peran.DEVELOPER) "Developer" else "Peserta") +
                                if (p.akunDiblokir) " · Diblokir" else " · Aktif",
                            fontSize = 12.sp, color = if (p.akunDiblokir) Merah else Mint
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                BarisInfo("NIM", p.nim)
                BarisInfo("Email", p.email)
                BarisInfo("Desa", p.desa)
                BarisInfo("Divisi", p.divisi)
                BarisInfo("Terdaftar", if (p.dibuatPadaMillis > 0) formatTanggal(p.dibuatPadaMillis) else "-")
                BarisInfo("Terakhir aktif", lalu(p.terakhirAktifMillis))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka("${r.jurnal.size}", "Jurnal", Modifier.weight(1f))
                KartuAngka("${r.tugasSelesai}/${r.tugas.size}", "Tugas", Modifier.weight(1f))
                KartuAngka("${r.pengumumanDibaca}/${r.pengumumanTotal}", "Dibaca", Modifier.weight(1f))
                KartuAngka("${r.suara}/${r.pollingTotal}", "Polling", Modifier.weight(1f))
            }
        }

        if (admin) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!adalahSaya) {
                    Button(
                        onClick = { vm.aturBlokir(p.uid, !p.akunDiblokir) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (p.akunDiblokir) HijauTua else Merah)
                    ) { Text(if (p.akunDiblokir) "Buka blokir" else "Blokir") }
                }
                Button(
                    onClick = { vm.resetSandi(p.email) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) { Text("Kirim reset sandi") }
            }
        }

        item { JudulBagian("Jurnal bersama (${r.jurnal.size})") }
        if (r.jurnal.isEmpty()) item { CatatanKosong("Belum pernah membagikan jurnal.") }
        items(r.jurnal.take(15)) { j ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Text(j.judul.ifBlank { "(tanpa judul)" }, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(formatTanggal(j.tanggalMillis), fontSize = 11.sp, color = TeksRedup)
                if (j.isi.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(j.isi, fontSize = 12.sp, color = TeksSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item { JudulBagian("Tugas (${r.tugasSelesai}/${r.tugas.size} selesai)") }
        if (r.tugas.isEmpty()) item { CatatanKosong("Belum diberi tugas.") }
        items(r.tugas) { t ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.judul, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Tenggat ${formatTanggal(t.deadlineMillis).substringBeforeLast(",")}", fontSize = 11.sp, color = TeksRedup)
                    }
                    TagKecil(if (t.selesai) "Selesai" else "Belum", if (t.selesai) Mint else Gold)
                }
            }
        }

        item { JudulBagian("Suara polling", "Polling bersifat terbuka bagi developer") }
        if (data.polling.isEmpty()) item { CatatanKosong("Belum ada polling.") }
        items(data.polling) { pol ->
            val idx = pol.suara[p.uid]
            KartuGelap(Modifier.fillMaxWidth()) {
                Text(pol.pertanyaan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (idx == null) "Belum memilih" else "Memilih: ${pol.opsi.getOrNull(idx) ?: "?"}",
                    fontSize = 12.sp, color = if (idx == null) TeksRedup else Mint
                )
            }
        }

        item { JudulBagian("Pengumuman belum dibaca (${belumDibaca.size})") }
        if (belumDibaca.isEmpty()) item { CatatanKosong("Semua pengumuman sudah dibaca.") }
        items(belumDibaca) { a ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Text(a.judul, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(formatTanggal(a.tanggalMillis), fontSize = 11.sp, color = TeksRedup)
            }
        }
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = TeksRedup, modifier = Modifier.width(100.dp))
        Text(nilai.ifBlank { "-" }, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CatatanKosong(teks: String) {
    Text(teks, fontSize = 12.sp, color = TeksRedup)
}

// ---------------------------------------------------------------- Aktivitas

@Composable
private fun TabAktivitas(data: DataDeveloper, modifier: Modifier) {
    var cari by rememberSaveable { mutableStateOf("") }
    var jenisIdx by rememberSaveable { mutableStateOf(-1) } // -1 = semua jenis
    val semua = remember(data) { susunAktivitas(data) }
    val tampil = remember(semua, cari, jenisIdx) {
        val q = cari.trim().lowercase()
        semua.filter {
            (jenisIdx < 0 || it.jenis.ordinal == jenisIdx) &&
                (q.isEmpty() || (it.judul + " " + it.rincian + " " + it.oleh).lowercase().contains(q))
        }.take(200)
    }
    LazyColumn(modifier, contentPadding = ISI_HALAMAN, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = cari, onValueChange = { cari = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari di semua data") }
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilChip("Semua", jenisIdx < 0, { jenisIdx = -1 })
                JenisAktivitas.entries.forEach { j -> PilChip(j.label, jenisIdx == j.ordinal, { jenisIdx = j.ordinal }) }
            }
        }
        if (tampil.isEmpty()) item { KosongState("Tidak ada aktivitas yang cocok.") }
        items(tampil) { a ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagKecil(a.jenis.label)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${a.oleh} · ${formatTanggal(a.millis)}", fontSize = 11.sp, color = TeksRedup,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(a.judul, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (a.rincian.isNotBlank()) {
                    Text(a.rincian, fontSize = 12.sp, color = TeksSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
