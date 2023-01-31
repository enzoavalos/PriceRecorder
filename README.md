# PriceRecorder
App to record your list of best prices!

## About the project
Price Recorder is a fully functioning Android app built in its entirety with Kotlin and a combination of Fragments and Jetpack Compose, following Android design and development best practices. It was originally intended as a personal side project to apply my newly acquired knowledge of Android development and to learn more about it in-depth and hands-on.

## Motivation
The idea behind the project was born from the observed necessity of various acquainted families, mine included, of a certain socioeconomic status to store and constantly update the price of daily-use products in different establishments to ensure the maximum savings possible, due to the current unstable and ever-changing economic situation in my home country.

## Features
- Adding new products with their corresponding information
- Edit and manage existing products
- Product filtering and search
- Register with your google account
- Create cloud backup for future restoration

## Screenshots


## Installation
From Android 5.0 onwards, you can install and test this app on your own device. In order to download it click the following [link](https://github.com/enzoavalos/PriceRecorder/releases/download/v1.0.0/pricerecorder.apk).   
The download will begin automatically, and after completion, you will be required to install the app. And that's it; you are good to go.

## Technologies and libraries used
- Firebase storage and authentication
- Room database
- Jetpack Compose
- Zxing barcode scanning library
- Exif interface
- Coil image loading library

## Characteristics
- Originally implemented using views system and later migrated to Jetpack Compose
- MVVM and Repository patterns implemented
- Room persistence library for storing data in local DB (CRUD, SQL queries, checkpoints for backup and restore)
- Firebase authentication with google
- Firebase storage to both store and retrieve backup information
- Permissions management (read/write external storage and more)
- Possibility to both pick images from gallery and take pictures
- Image manipulation using exif info
- Barcode and qr scanning implemented
- Usage of custom notifications for both upload and download
- Asynchronous tasks through Kotlin coroutines
- Use of shared preferences interface to store user setting preferences
