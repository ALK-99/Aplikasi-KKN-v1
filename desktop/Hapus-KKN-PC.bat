@echo off
chcp 65001 >nul
title Hapus KKN Al-Mushlih 2027 (PC)
echo.
echo  Alat ini akan:
echo   1. menutup aplikasi KKN Al-Mushlih
echo   2. mencopot aplikasinya (bila terpasang lewat Setup)
echo   3. menghapus semua data lokal (login, cache, ingatan asisten)
echo  Data desa di server (Firebase) TIDAK terhapus.
echo.
set /p OK=Lanjutkan? Ketik Y lalu Enter: 
if /i not "%OK%"=="Y" goto selesai
taskkill /f /im "KKN Al-Mushlih 2027.exe" >nul 2>&1
taskkill /f /im "KKN-Al-Mushlih-PC-Portable*.exe" >nul 2>&1
for /d %%D in ("%LOCALAPPDATA%\Programs\*KKN*" "%LOCALAPPDATA%\Programs\*kkn*") do (
  for %%U in ("%%~D\Uninstall*.exe") do (
    echo Mencopot: %%~U
    start /wait "" "%%~U" /S
  )
)
rmdir /s /q "%APPDATA%\KKN Al-Mushlih 2027" >nul 2>&1
rmdir /s /q "%APPDATA%\kkn-almushlih-pc" >nul 2>&1
rmdir /s /q "%LOCALAPPDATA%\kkn-almushlih-pc-updater" >nul 2>&1
rmdir /s /q "%LOCALAPPDATA%\KKN Al-Mushlih 2027" >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "KKN Al-Mushlih 2027" /f >nul 2>&1
echo.
echo  Selesai. Berkas Setup atau Portable yang kamu unduh bisa dihapus manual dari folder Unduhan.
:selesai
echo.
pause
