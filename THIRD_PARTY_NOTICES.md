# Third-Party Notices & Open Source Acknowledgements

MobileFilmLab incorporates or relies upon the following open-source software libraries and frameworks:

---

### 1. Recipe Lab (Sony PMCA)
* **Author:** voxivoid
* **Repository:** https://github.com/voxivoid/recipe-lab-sony-pmca
* **Licence:** GNU General Public License v3.0 (GPL-3.0)
* **Usage:** Recipe table definitions, parameter ranges, YAML Format 1 schema, and categories.

---

### 2. Android Jetpack & AndroidX Libraries
* **Copyright:** The Android Open Source Project
* **Licence:** Apache License, Version 2.0
* **Components:**
  * androidx.core:core-ktx
  * androidx.activity:activity-compose
  * androidx.compose.* (UI, Foundation, Material 3, Runtime)
  * androidx.lifecycle:lifecycle-runtime-ktx
  * androidx.lifecycle:lifecycle-viewmodel-compose
  * androidx.exifinterface:exifinterface
* **Licence Text:** http://www.apache.org/licenses/LICENSE-2.0

---

### 3. Kotlin & Kotlin Coroutines
* **Copyright:** JetBrains s.r.o.
* **Licence:** Apache License, Version 2.0
* **Components:**
  * org.jetbrains.kotlin:kotlin-stdlib
  * org.jetbrains.kotlinx:kotlinx-coroutines-android
  * org.jetbrains.kotlinx:kotlinx-coroutines-test
* **Licence Text:** http://www.apache.org/licenses/LICENSE-2.0

---

### 4. Google Gson
* **Copyright:** Google LLC
* **Licence:** Apache License, Version 2.0
* **Repository:** https://github.com/google/gson
* **Usage:** JSON recipe serialization and deserialization.

---

### 5. SnakeYAML
* **Copyright:** Andrey Somov
* **Licence:** Apache License, Version 2.0
* **Repository:** https://bitbucket.org/snakeyaml/snakeyaml
* **Usage:** Upstream Recipe Lab YAML Format 1 custom recipe parser.

---

### 6. JUnit 4
* **Licence:** Eclipse Public License 1.0 (EPL-1.0)
* **Repository:** https://github.com/junit-team/junit4
* **Usage:** Automated unit tests for recipe validation and crop logic.

---

### 7. ZXing Core
* **Copyright:** ZXing authors
* **Licence:** Apache License, Version 2.0
* **Repository:** https://github.com/zxing/zxing
* **Usage:** Local recipe QR PNG encoding and scanning of user-selected QR images. No network scanner service.

---

### 8. Robolectric (tests only)
* **Licence:** MIT
* **Repository:** https://github.com/robolectric/robolectric
* **Usage:** Android EXIF/bitmap/SQLite/native-graphics regression tests on the JVM. Not shipped in the APK.
