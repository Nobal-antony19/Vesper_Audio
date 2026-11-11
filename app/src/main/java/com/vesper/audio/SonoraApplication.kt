package com.vesper.audio

import android.app.Application
import com.vesper.audio.data.MusicRepository
import com.vesper.audio.data.local.SonoraDatabase
import com.vesper.audio.data.local.SonoraLocalRepository

class SonoraApplication : Application() {

    lateinit var localRepository: SonoraLocalRepository
        private set

    lateinit var musicRepository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = SonoraDatabase.getInstance(this)
        localRepository = SonoraLocalRepository(database.sonoraDao())
        musicRepository = MusicRepository(this, localRepository)
    }
}
