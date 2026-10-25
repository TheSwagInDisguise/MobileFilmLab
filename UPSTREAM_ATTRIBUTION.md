# Upstream Attribution & Provenance

## Upstream Project Information
* **Upstream Project:** Recipe Lab (Sony PMCA)
* **Canonical Upstream Repository:** [https://github.com/voxivoid/recipe-lab-sony-pmca](https://github.com/voxivoid/recipe-lab-sony-pmca)
* **Author / Maintainer:** voxivoid
* **Upstream Commit SHA Reviewed:** `aab4b5703a2b77362b83682801c8e50145ddc6ee`
* **Date Reviewed:** October 25, 2026
* **Upstream Licence:** GNU General Public License v3.0 (GPL-3.0)

## Derivative Work Basis & Licensing
MobileFilmLab is an independent Android port and derivative work of the recipe/preset engine from the Sony Recipe Lab project.
In compliance with Section 5 of the GNU General Public License v3.0, MobileFilmLab is licensed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**.
Complete source code is publicly accessible at [https://github.com/TheSwagInDisguise/MobileFilmLab](https://github.com/TheSwagInDisguise/MobileFilmLab).

## Upstream Files and Data Adapted
1. `src/com/voxivoid/recipelab/Recipes.java`
   - Complete 77-entry recipe table (the Factory look plus 76 film-simulation approximations).
   - Upstream grouping and ordering across 12 categories: Sony (8), Fuji Sim (16), Fuji Film (5), Kodak (14), Cine (4), Ricoh GR (8), Leica (4), Hasselblad (1), Canon / Nikon (5), Pana / Olympus (4), Other Stocks (3), Ilford (5).
   - Parameter ranges: Creative Style (1..14), Saturation (-3..+3), Contrast (-3..+3), Sharpness (-3..+3), White Balance Kelvin (2500K..9900K), Amber-Blue fine tune (-7..+7), Green-Magenta fine tune (-7..+7), Picture Effects (0..13), Sub-parameters (0..4), EV bias (-15..+15 in 1/3 EV steps), and DRO (0..6).
2. `src/com/voxivoid/recipelab/RecipeFormatV1.java` & `src/com/voxivoid/recipelab/CustomRecipes.java`
   - YAML Format 1 custom recipe parsing and export logic adapted to Kotlin for memory-card / storage import and export.
3. `src/com/voxivoid/recipelab/Favourites.java`
   - Favourites persistence and fast-filtering logic.

## Adaptations and Hardware Translations for Android / Oppo
1. **Camera Architecture:** The Sony PMCA app writes settings directly into Sony camera body settings stores (CameraEx API). Android and Oppo Find X9 Ultra hardware do not expose direct equivalents for Sony Creative Style, Sony Picture Effects, or Sony's internal BIONZ X processing pipeline.
2. **Honest Approximation Pipeline:** MobileFilmLab implements an honest computational imaging pipeline using Android Camera2 and Kotlin image processing. Recipe parameters are mathematically translated into color matrices, S-curve tone curves, Kelvin color temperature shifts, tint matrices, and simulated optical monochrome filters (Yellow, Red, Green).
3. **No Proprietary Brand Claims:** MobileFilmLab makes no claim that outputs exactly reproduce proprietary manufacturer color science (Sony, Fujifilm, Kodak, Leica, Ricoh, Hasselblad, etc.). All looks are labeled as "Recipe Lab-derived looks" and "approximations."
4. **Original Android Interface:** No Sony trademarks, Sony camera artwork, A6000 UI, or upstream branding are reproduced. The branding and interface are completely original, designed specifically for the Oppo Find X9 Ultra.

## Disclaimer & Independence Notice
MobileFilmLab is independently developed and maintained. It is **not** affiliated with, endorsed by, or sponsored by Sony Corporation, the upstream Recipe Lab project, Fujifilm, Eastman Kodak Company, Leica Camera AG, Ricoh Imaging Company, or Hasselblad.
