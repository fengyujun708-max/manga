package com.mangaverse.app.backups.external

enum class ExternalBackupApp {
    MIHON,
    KOMIKKU,
    VENERA,
    ;

    val family: ExternalBackupFamily
        get() = ExternalBackupFamily.MANGA
}

enum class ExternalBackupFamily {
    MANGA,
}
