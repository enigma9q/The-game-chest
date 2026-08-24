package com.gamechest.core.loader

import com.gamechest.core.model.GamePack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class GamePackManager(
    private val gamesDirectory: File? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val installedPacks = mutableMapOf<String, GamePack>()

    init {
        // Register built-in packs
        val turboCircuit = TurboCircuitPack.createDefaultPack()
        installedPacks[turboCircuit.manifest.id] = turboCircuit

        val saveTheSheep = SaveTheSheepPack.createPack()
        installedPacks[saveTheSheep.manifest.id] = saveTheSheep
    }

    fun getAllPacks(): List<GamePack> {
        return installedPacks.values.toList()
    }

    fun getPack(id: String): GamePack? {
        return installedPacks[id]
    }

    suspend fun importPackFromJsonString(jsonString: String): Result<GamePack> = withContext(Dispatchers.IO) {
        try {
            val pack = json.decodeFromString<GamePack>(jsonString)
            installedPacks[pack.manifest.id] = pack
            Result.success(pack)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importPackFromZipStream(inputStream: InputStream): Result<GamePack> = withContext(Dispatchers.IO) {
        try {
            var manifestJson: String? = null
            var layoutJson: String? = null
            var packJson: String? = null

            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "pack.json", "game.json" -> {
                            packJson = zip.bufferedReader().readText()
                        }
                        "manifest.json" -> {
                            manifestJson = zip.bufferedReader().readText()
                        }
                        "board.json", "layout.json" -> {
                            layoutJson = zip.bufferedReader().readText()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (packJson != null) {
                val pack = json.decodeFromString<GamePack>(packJson!!)
                installedPacks[pack.manifest.id] = pack
                return@withContext Result.success(pack)
            }

            if (manifestJson != null && layoutJson != null) {
                val manifest = json.decodeFromString<com.gamechest.core.model.GameManifest>(manifestJson!!)
                val layout = json.decodeFromString<com.gamechest.core.model.TableLayoutConfig>(layoutJson!!)
                val pack = GamePack(manifest = manifest, tableLayout = layout)
                installedPacks[manifest.id] = pack
                return@withContext Result.success(pack)
            }

            Result.failure(IllegalArgumentException("Invalid .gamechest bundle: missing game.json or manifest.json + layout.json"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
