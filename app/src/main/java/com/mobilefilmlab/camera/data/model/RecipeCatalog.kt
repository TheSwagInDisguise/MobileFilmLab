package com.mobilefilmlab.camera.data.model

/**
 * Authoritative catalogue of all 77 built-in recipes ported from Recipe Lab (Sony PMCA).
 * Upstream Reference: https://github.com/voxivoid/recipe-lab-sony-pmca (commit aab4b57)
 */
object RecipeCatalog {

    val ALL_RECIPES: List<Recipe> = listOf(
        // ==================== 1. Sony (8 recipes) ====================
        Recipe(
            id = "sony_factory_st",
            name = "FACTORY (ST)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.STD,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_pt_portrait",
            name = "Sony PT (portrait)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.PORTRAIT,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_nt_neutral",
            name = "Sony NT (neutral)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.NEUTRAL,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_vv_vivid",
            name = "Sony VV (vivid)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.VIVID,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_vv2",
            name = "Sony VV2",
            group = RecipeGroup.SONY,
            style = CreativeStyle.VIVID,
            sat = 2, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_fl_film_like",
            name = "Sony FL (film-like)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_in_instant",
            name = "Sony IN (instant)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = -3, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = -1,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "sony_sh_soft_high_key",
            name = "Sony SH (soft high-key)",
            group = RecipeGroup.SONY,
            style = CreativeStyle.LIGHT,
            sat = -2, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 0,
            pe = PictureEffect.SOFT_HIGH_KEY, ev = 3, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 2. Fuji Sim (16 recipes) ====================
        Recipe(
            id = "fuji_provia",
            name = "Provia",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.STD,
            sat = 1, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_velvia",
            name = "Velvia",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.VIVID,
            sat = 3, con = 2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_astia",
            name = "Astia",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.PORTRAIT,
            sat = 0, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 0,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_classic_chrome",
            name = "Classic Chrome",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.STD,
            sat = -1, con = 2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 3, gm = 1,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_classic_negative",
            name = "Classic Negative",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.STD,
            sat = -3, con = 3, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 1,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_nostalgic_neg",
            name = "Nostalgic Neg",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.STD,
            sat = 1, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 3, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_reala_ace",
            name = "Reala Ace",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.STD,
            sat = 0, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_pro_neg_std",
            name = "Pro Neg Std",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.PORTRAIT,
            sat = -2, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_pro_neg_hi",
            name = "Pro Neg Hi",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.PORTRAIT,
            sat = -2, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_eterna",
            name = "Eterna",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = -2, sharp = -1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.LEVEL_3, sub = 0
        ),
        Recipe(
            id = "fuji_eterna_bleach_bypass",
            name = "Eterna Bleach Bypass",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = 3, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.OFF, sub = 0
        ),
        Recipe(
            id = "fuji_acros",
            name = "Acros",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.MONO,
            sat = 0, con = 1, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_acros_ye",
            name = "Acros +Ye (yellow filter)",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.MONO,
            sat = 0, con = 1, sharp = 1,
            wbMode = WbMode.KELVIN, kelvin = 4000, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_acros_r",
            name = "Acros +R (red filter)",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.MONO,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.KELVIN, kelvin = 2500, ab = 0, gm = 0,
            pe = PictureEffect.ROUGH_MONO, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_acros_g",
            name = "Acros +G (green filter)",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.MONO,
            sat = 0, con = 1, sharp = 1,
            wbMode = WbMode.KELVIN, kelvin = 5600, ab = 0, gm = 4,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fuji_sepia",
            name = "Sepia",
            group = RecipeGroup.FUJI_SIM,
            style = CreativeStyle.SEPIA,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 3. Fuji Film (5 recipes) ====================
        Recipe(
            id = "fujifilm_pro_400h",
            name = "Fuji Pro 400H",
            group = RecipeGroup.FUJI_FILM,
            style = CreativeStyle.LIGHT,
            sat = -2, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 1,
            pe = PictureEffect.OFF, ev = 2, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fujifilm_fortia_50",
            name = "Fuji Fortia 50",
            group = RecipeGroup.FUJI_FILM,
            style = CreativeStyle.VIVID,
            sat = 3, con = 2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fujifilm_superia_400",
            name = "Fuji Superia 400",
            group = RecipeGroup.FUJI_FILM,
            style = CreativeStyle.STD,
            sat = 1, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fujifilm_c200",
            name = "Fuji C200",
            group = RecipeGroup.FUJI_FILM,
            style = CreativeStyle.STD,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 1,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "fujifilm_natura_1600",
            name = "Fuji Natura 1600",
            group = RecipeGroup.FUJI_FILM,
            style = CreativeStyle.PORTRAIT,
            sat = -2, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 0,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 4. Kodak (14 recipes) ====================
        Recipe(
            id = "kodak_portra_160",
            name = "Kodak Portra 160",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.PORTRAIT,
            sat = -3, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = -1,
            pe = PictureEffect.OFF, ev = 2, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_portra_400",
            name = "Kodak Portra 400",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.PORTRAIT,
            sat = -1, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 3, gm = 1,
            pe = PictureEffect.OFF, ev = 2, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_portra_800",
            name = "Kodak Portra 800",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.PORTRAIT,
            sat = -2, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_gold_200",
            name = "Kodak Gold 200",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.STD,
            sat = 2, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 3, gm = 1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_ultra_max_400",
            name = "Kodak Ultra Max 400",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.STD,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_color_plus_200",
            name = "Kodak Color Plus 200",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.STD,
            sat = 1, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 2, gm = 1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_ektar_100",
            name = "Kodak Ektar 100",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.STD,
            sat = 3, con = 1, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_ektachrome_e100",
            name = "Kodak Ektachrome E100",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.CLEAR,
            sat = 3, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_kodachrome_64",
            name = "Kodachrome 64",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.DEEP,
            sat = 3, con = 2, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_vision3_500t",
            name = "Kodak Vision3 500T (daylight)",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.NEUTRAL,
            sat = -1, con = 0, sharp = 0,
            wbMode = WbMode.KELVIN, kelvin = 3200, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.LEVEL_3, sub = 0
        ),
        Recipe(
            id = "kodak_vision_200t",
            name = "Kodak Vision 200T (Asteroid City)",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.NEUTRAL,
            sat = -2, con = -3, sharp = 0,
            wbMode = WbMode.KELVIN, kelvin = 5000, ab = 2, gm = 3,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.LEVEL_3, sub = 0
        ),
        Recipe(
            id = "kodak_tri_x_400",
            name = "Kodak Tri-X 400",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.MONO,
            sat = 0, con = 2, sharp = 2,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_t_max",
            name = "Kodak T-Max",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.MONO,
            sat = 0, con = 2, sharp = 3,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "kodak_tri_x_1600",
            name = "Kodak Tri-X 1600 (pushed)",
            group = RecipeGroup.KODAK,
            style = CreativeStyle.MONO,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.ROUGH_MONO, ev = 1, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 5. Cine (4 recipes) ====================
        Recipe(
            id = "cine_cinestill_50d",
            name = "Cinestill 50D (Blue Velvet)",
            group = RecipeGroup.CINE,
            style = CreativeStyle.STD,
            sat = -2, con = -1, sharp = 0,
            wbMode = WbMode.KELVIN, kelvin = 5500, ab = -1, gm = -1,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "cine_cinestill_800t",
            name = "Cinestill 800T",
            group = RecipeGroup.CINE,
            style = CreativeStyle.NEUTRAL,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.KELVIN, kelvin = 3200, ab = 0, gm = -1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "cine_classic_cinema",
            name = "Classic Cinema",
            group = RecipeGroup.CINE,
            style = CreativeStyle.STD,
            sat = 0, con = -1, sharp = 0,
            wbMode = WbMode.KELVIN, kelvin = 6000, ab = 2, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.LEVEL_3, sub = 0
        ),
        Recipe(
            id = "cine_rec709_video",
            name = "Rec709 Video (flat-ish)",
            group = RecipeGroup.CINE,
            style = CreativeStyle.NEUTRAL,
            sat = -2, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.LEVEL_5, sub = 0
        ),

        // ==================== 6. Ricoh GR (8 recipes) ====================
        Recipe(
            id = "ricoh_gr_positive_film",
            name = "GR Positive Film",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.STD,
            sat = 3, con = 2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 2, gm = 0,
            pe = PictureEffect.OFF, ev = -1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_negative_film",
            name = "GR Negative Film",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.NEUTRAL,
            sat = -2, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -1, gm = 1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_bleach_bypass",
            name = "GR Bleach Bypass",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = 3, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.OFF, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_retro",
            name = "GR Retro",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.STD,
            sat = -3, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 3, gm = -1,
            pe = PictureEffect.RETRO_PHOTO, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_cross_process",
            name = "GR Cross Process",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.VIVID,
            sat = 2, con = 2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = -2, gm = 4,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_hi_contrast_bw",
            name = "GR Hi-Contrast B&W",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.MONO,
            sat = 0, con = 3, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.ROUGH_MONO, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_hard_monotone",
            name = "GR Hard Monotone",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.MONO,
            sat = 0, con = 2, sharp = 2,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ricoh_gr_soft_monotone",
            name = "GR Soft Monotone",
            group = RecipeGroup.RICOH_GR,
            style = CreativeStyle.MONO,
            sat = 0, con = -2, sharp = -1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 7. Leica (4 recipes) ====================
        Recipe(
            id = "leica_contemporary",
            name = "Leica Contemporary",
            group = RecipeGroup.LEICA,
            style = CreativeStyle.STD,
            sat = 1, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "leica_classic",
            name = "Leica Classic",
            group = RecipeGroup.LEICA,
            style = CreativeStyle.STD,
            sat = -1, con = 2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "leica_eternal",
            name = "Leica Eternal",
            group = RecipeGroup.LEICA,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.LEVEL_3, sub = 0
        ),
        Recipe(
            id = "leica_monochrom",
            name = "Leica Monochrom",
            group = RecipeGroup.LEICA,
            style = CreativeStyle.MONO,
            sat = 0, con = 2, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 8. Hasselblad (1 recipe) ====================
        Recipe(
            id = "hasselblad_hncs_natural",
            name = "Hasselblad HNCS Natural",
            group = RecipeGroup.HASSELBLAD,
            style = CreativeStyle.NEUTRAL,
            sat = -1, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 9. Canon / Nikon (5 recipes) ====================
        Recipe(
            id = "canon_standard",
            name = "Canon Standard",
            group = RecipeGroup.CANON_NIKON,
            style = CreativeStyle.STD,
            sat = 1, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = -1,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "canon_portrait",
            name = "Canon Portrait",
            group = RecipeGroup.CANON_NIKON,
            style = CreativeStyle.PORTRAIT,
            sat = 0, con = 0, sharp = -1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = -1,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "canon_faithful",
            name = "Canon Faithful",
            group = RecipeGroup.CANON_NIKON,
            style = CreativeStyle.NEUTRAL,
            sat = 0, con = 0, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "nikon_flat",
            name = "Nikon Flat",
            group = RecipeGroup.CANON_NIKON,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = -3, sharp = -1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.LEVEL_5, sub = 0
        ),
        Recipe(
            id = "nikon_vivid",
            name = "Nikon Vivid",
            group = RecipeGroup.CANON_NIKON,
            style = CreativeStyle.VIVID,
            sat = 1, con = 1, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 10. Pana / Olympus (4 recipes) ====================
        Recipe(
            id = "pana_l_monochrome_d",
            name = "Pana L.Monochrome D",
            group = RecipeGroup.PANA_OLYMPUS,
            style = CreativeStyle.MONO,
            sat = 0, con = 3, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "pana_l_classic_neo",
            name = "Pana L.ClassicNeo",
            group = RecipeGroup.PANA_OLYMPUS,
            style = CreativeStyle.NEUTRAL,
            sat = -3, con = -1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 2, gm = 0,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "olympus_pop_art",
            name = "Olympus Pop Art",
            group = RecipeGroup.PANA_OLYMPUS,
            style = CreativeStyle.VIVID,
            sat = 3, con = 3, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "olympus_pale_light",
            name = "Olympus Pale & Light",
            group = RecipeGroup.PANA_OLYMPUS,
            style = CreativeStyle.LIGHT,
            sat = -3, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 2, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 11. Other Stocks (3 recipes) ====================
        Recipe(
            id = "agfa_vista_200",
            name = "Agfa Vista 200",
            group = RecipeGroup.OTHER_STOCKS,
            style = CreativeStyle.STD,
            sat = 2, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 2, gm = -1,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "agfa_ultra_100",
            name = "Agfa Ultra 100",
            group = RecipeGroup.OTHER_STOCKS,
            style = CreativeStyle.VIVID,
            sat = 3, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "polaroid_instax",
            name = "Polaroid / Instax",
            group = RecipeGroup.OTHER_STOCKS,
            style = CreativeStyle.STD,
            sat = -3, con = -2, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 1, gm = -2,
            pe = PictureEffect.RETRO_PHOTO, ev = 1, dro = DroMode.AUTO, sub = 0
        ),

        // ==================== 12. Ilford (5 recipes) ====================
        Recipe(
            id = "ilford_hp5",
            name = "Ilford HP5",
            group = RecipeGroup.ILFORD,
            style = CreativeStyle.MONO,
            sat = 0, con = 1, sharp = 0,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 1, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ilford_fp4",
            name = "Ilford FP4",
            group = RecipeGroup.ILFORD,
            style = CreativeStyle.MONO,
            sat = 0, con = 1, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ilford_delta_100",
            name = "Ilford Delta 100",
            group = RecipeGroup.ILFORD,
            style = CreativeStyle.MONO,
            sat = 0, con = 1, sharp = 1,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ilford_delta_3200",
            name = "Ilford Delta 3200",
            group = RecipeGroup.ILFORD,
            style = CreativeStyle.MONO,
            sat = 0, con = 3, sharp = -2,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 2, dro = DroMode.AUTO, sub = 0
        ),
        Recipe(
            id = "ilford_pan_f_50",
            name = "Ilford Pan F 50",
            group = RecipeGroup.ILFORD,
            style = CreativeStyle.MONO,
            sat = 0, con = 2, sharp = 2,
            wbMode = WbMode.AUTO, kelvin = 0, ab = 0, gm = 0,
            pe = PictureEffect.OFF, ev = 0, dro = DroMode.AUTO, sub = 0
        )
    )

    fun getById(id: String): Recipe? = ALL_RECIPES.firstOrNull { it.id == id }

    fun getFactory(): Recipe = ALL_RECIPES.first()

    fun getByGroup(group: RecipeGroup): List<Recipe> = ALL_RECIPES.filter { it.group == group }
}
