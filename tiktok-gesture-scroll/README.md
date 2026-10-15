# TikTok Gesture Scroll (Android)

Starter project Android Kotlin: CameraX + MediaPipe Hand Landmarker + Accessibility Service.
Project ini adalah source code awal, bukan APK yang sudah dikompilasi.

## Persiapan model tangan
Aplikasi membutuhkan file model MediaPipe bernama `hand_landmarker.task`.
Unduh model resmi dari:
https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task

Simpan file tersebut ke:
`app/src/main/assets/hand_landmarker.task`

## Cara membuka
1. Buka folder proyek di Android Studio versi yang mendukung Android Gradle Plugin 8.6.
2. Tunggu Gradle selesai mengunduh dependensi.
3. Tambahkan model di langkah di atas.
4. Jalankan pada HP Android fisik.
5. Izinkan kamera.
6. Buka Pengaturan Aksesibilitas dan aktifkan layanan **Gesture Scroll**.
7. Kembali ke aplikasi dan tekan **Mulai Auto Scroll**.
8. Buka TikTok, arahkan kamera depan agar tangan terlihat, lalu gerakkan tangan perlahan naik/turun.

## Catatan
- Accessibility Service dapat melakukan gerakan scroll pada layar; aktifkan hanya jika kamu memahami izin ini.
- Implementasi contoh menggunakan perubahan posisi tangan sebagai pemicu. Pencahayaan, jarak, orientasi kamera, dan perangkat dapat memengaruhi hasil.
- Contoh ini perlu pengujian dan kemungkinan penyesuaian sebelum dipakai sehari-hari.
- Pada beberapa perangkat, `PreviewView.bitmap` dapat kosong atau kurang ideal untuk analisis real-time; untuk aplikasi produksi, konversi langsung dari `ImageProxy` ke MediaPipe Image disarankan.
- Tidak ada data kamera yang dikirim ke server oleh kode contoh ini.
