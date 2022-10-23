# PriceRecorder
App to record your list of best prices!

## About the project
The idea behind the project was born from the observed necessity of various acquainted families of a certain socioeconomic status to store the price of daily use products in different establishments to ensure the maximum saving possible, specially given the current unstable and constantly changing economic situation.

## Characteristics
- Firstly implemented using views system and later migrated to Jetpack Compose.
- MVVM and Repository patterns implemented.
- Room persistence library for storing data in local DB (CRUD, SQL queries, checkpoints for backup and restore).
- Firebase auth with google.
- Firebase storage to store and retrieve backup information.
- Permissions management (read/write external storage and more).
- Possibility to both pick images from gallery and take pictures.
- Image manipulation using exif info.
- Barcode and qr scanning implemented using Zxing library.
- Usage of custom notifications for both upload and download.
- Asynchronous tasks through Kotlin coroutines.