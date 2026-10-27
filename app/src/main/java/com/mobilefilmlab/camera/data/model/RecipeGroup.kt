package com.mobilefilmlab.camera.data.model

enum class RecipeGroup(val displayName: String, val sortOrder: Int) {
    OPPO("Oppo", -1),
    SONY("Sony", 0),
    FUJI_SIM("Fuji Sim", 1),
    FUJI_FILM("Fuji Film", 2),
    KODAK("Kodak", 3),
    CINE("Cine", 4),
    RICOH_GR("Ricoh GR", 5),
    LEICA("Leica", 6),
    HASSELBLAD("Hasselblad", 7),
    CANON_NIKON("Canon / Nikon", 8),
    PANA_OLYMPUS("Pana / Olympus", 9),
    OTHER_STOCKS("Other Stocks", 10),
    ILFORD("Ilford", 11),
    CUSTOM("Custom", 12);

    companion object {
        fun fromUpstreamIndex(index: Int): RecipeGroup = when (index) {
            0 -> SONY
            1 -> FUJI_SIM
            2 -> FUJI_FILM
            3 -> KODAK
            4 -> CINE
            5 -> RICOH_GR
            6 -> LEICA
            7 -> HASSELBLAD
            8 -> CANON_NIKON
            9 -> PANA_OLYMPUS
            10 -> OTHER_STOCKS
            11 -> ILFORD
            else -> CUSTOM
        }
    }
}
