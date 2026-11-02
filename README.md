# Event Expense Tracker (Android App)

Aplikasi Android untuk melacak biaya pengeluaran berbasis acara menggunakan Optical Character Recognition (OCR) dan model Machine Learning kustom.

## Fitur Utama
* **Manajemen Acara:** Pembuatan laporan rekapitulasi pengeluaran berbasis kegiatan.
* **Document Scanner:** Pemindaian nota fisik menggunakan Google ML Kit Document Scanner.
* **Ekstraksi Teks Otomatis:** Identifikasi nama barang dan harga dari teks mentah nota menggunakan TFLite.
* **Interactive Guided Tour:** Panduan *onboarding* interaktif menggunakan status Preferences DataStore.

## Tech Stack
* **Bahasa:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Database Lokal:** Realm Database
* **State Management:** Preferences DataStore & Coroutines Flow
* **Machine Learning:** Google ML Kit & TensorFlow Lite (BiLSTM-CRF & Viterbi Decoding)

## Cara Menjalankan Proyek
1. Clone repositori ini: `git clone https://github.com/sitifatiharh/event-expense-tracker`
2. Buka proyek menggunakan Android Studio Quail.
3. Hubungkan perangkat Android fisik atau jalankan emulator.
4. Lakukan *Sync Project with Gradle Files*.
5. Klik **Run** (Shift + F10).

## Penulis
**Siti Fatiha Rahmah**  
Mahasiswa Sarjana Komputer - Universitas Esa Unggul
