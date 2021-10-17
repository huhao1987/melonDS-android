package me.magnum.melonds.ui.emulator.rom

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.CacheDoubleUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import hh.game.usrcheat_android.usrcheat.Gamedetail
import hh.game.usrcheat_android.usrcheat.UsrCheatUtils.Companion.toHex
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import me.magnum.melonds.MelonEmulator
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.*
import me.magnum.melonds.parcelables.RomInfoParcelable
import me.magnum.melonds.parcelables.RomParcelable
import me.magnum.melonds.ui.emulator.EmulatorActivity
import me.magnum.melonds.ui.emulator.EmulatorDelegate
import java.text.SimpleDateFormat

class RomEmulatorDelegate(activity: EmulatorActivity, private val picasso: Picasso) : EmulatorDelegate(activity) {

    private enum class RomPauseMenuOptions(override val textResource: Int) : EmulatorActivity.PauseMenuOption {
        SETTINGS(R.string.settings),
        SAVE_STATE(R.string.save_state),
        LOAD_STATE(R.string.load_state),
        CHEATS(R.string.cheats),
        USRCHEAT(R.string.usrcheats),
        RESET(R.string.reset),
        EXIT(R.string.exit)
    }

    private lateinit var loadedRom: Rom
    private var cheatsLoadDisposable: Disposable? = null

    override fun getEmulatorSetupObservable(extras: Bundle?): Completable {
        val romParcelable = extras?.getParcelable(EmulatorActivity.KEY_ROM) as RomParcelable?
        romParcelable?.rom
        val romLoader = if (romParcelable?.rom != null) {
            Maybe.just(romParcelable.rom)
        } else {
            if (extras?.containsKey(EmulatorActivity.KEY_PATH) == true) {
                val romPath = extras.getString(EmulatorActivity.KEY_PATH) ?: throw NullPointerException("${EmulatorActivity.KEY_PATH} was null")
                activity.viewModel.getRomAtPath(romPath)
            } else if (extras?.containsKey(EmulatorActivity.KEY_URI) == true) {
                val romUri = extras.getString(EmulatorActivity.KEY_URI) ?: throw NullPointerException("${EmulatorActivity.KEY_URI} was null")
                activity.viewModel.getRomAtUri(romUri.toUri())
            } else {
                throw NullPointerException("No ROM was specified")
            }
        }

        return romLoader.toSingle().onErrorResumeNext {
            if (it is NoSuchElementException) {
                showRomNotFoundDialog()
                // Prevent the observable from completing
                Single.never()
            } else {
                // Re-throw the error
                Single.error(it)
            }
        }.flatMap { rom ->
            activity.viewModel.loadLayoutForRom(rom)
            activity.viewModel.getRomLoader(rom)
        }.flatMap { romPair ->
            loadedRom = romPair.first
            loadRomCheats(loadedRom).toSingle(emptyList()).zipWith(getEmulatorLaunchConfiguration(loadedRom)) { cheats, emulatorConfiguration ->
                Pair(cheats, emulatorConfiguration)
            }.flatMap { (cheats, emulatorConfiguration) ->
                Single.create<MelonEmulator.LoadResult> { emitter ->
                    MelonEmulator.setupEmulator(emulatorConfiguration, activity.assets, activity.buildUriFileHandler(), activity.getRendererTextureBuffer())

                    val rom = romPair.first
                    val romPath = romPair.second
                    val sramPath = activity.viewModel.getRomSramFile(rom)
                    val showBios = emulatorConfiguration.showBootScreen

                    val gbaCartPath = rom.config.gbaCartPath
                    val gbaSavePath = rom.config.gbaSavePath
                    val loadResult = MelonEmulator.loadRom(romPath, sramPath, !showBios, rom.config.mustLoadGbaCart(), gbaCartPath, gbaSavePath)
                    if (loadResult.isTerminal) {
                        throw EmulatorActivity.RomLoadFailedException(loadResult)
                    }
                    MelonEmulator.setupCheats(cheats.toTypedArray())
                    enableUsrcheatcode(rom)
                    emitter.onSuccess(loadResult)
                }
            }
        }.doAfterSuccess {
            if (it == MelonEmulator.LoadResult.SUCCESS_GBA_FAILED) {
                activity.runOnUiThread {
                    Toast.makeText(activity, R.string.error_load_gba_rom, Toast.LENGTH_SHORT).show()
                }
            }
        }.ignoreElement()
    }

    private fun enableUsrcheatcode(rom: Rom){
        //Load usrcheat cheats
        val romInfo = activity.viewModel.getRomInfo(rom)
        romInfo?.let {
            var romInfoParcelable= RomInfoParcelable.fromRomInfo(it)
            var tag = romInfoParcelable.gameCode + romInfoParcelable.headerChecksum.toBigInteger()
                .toByteArray().toHex()
            var enablecheats=CacheDoubleUtils.getInstance().getString(tag+"enablecheat")
            if(enablecheats!=null) {
                var list: ArrayList<Cheat> =
                    Gson().fromJson(
                        enablecheats,
                        object : TypeToken<ArrayList<Cheat>>() {}.type
                    )
                if (list.size > 0) {
                    MelonEmulator.setupCheats(list.toTypedArray())
                }
            }
        }
    }
    private fun showRomNotFoundDialog() {
        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.error_rom_not_found)
                    .setMessage(R.string.error_rom_not_found_info)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        activity.finish()
                    }
                    .setOnDismissListener {
                        activity.finish()
                    }
                    .show()
        }
    }

    private fun getEmulatorConfigurationForRom(rom: Rom): EmulatorConfiguration {
        return activity.viewModel.getEmulatorConfigurationForRom(rom)
    }

    private fun getEmulatorLaunchConfiguration(rom: Rom): Single<EmulatorConfiguration> {
        val baseEmulatorConfiguration = getEmulatorConfigurationForRom(rom)
        return activity.adjustEmulatorConfigurationForPermissions(baseEmulatorConfiguration, true)
    }

    override fun getEmulatorConfiguration(): EmulatorConfiguration {
        val baseEmulatorConfiguration = getEmulatorConfigurationForRom(loadedRom)
        return activity.adjustEmulatorConfigurationForPermissions(baseEmulatorConfiguration, false).blockingGet()
    }

    override fun getPauseMenuOptions(): List<EmulatorActivity.PauseMenuOption> {
        return RomPauseMenuOptions.values().toList()
    }

    override fun onPauseMenuOptionSelected(option: EmulatorActivity.PauseMenuOption) {
        when (option) {
            RomPauseMenuOptions.SETTINGS -> activity.openSettings()
            RomPauseMenuOptions.SAVE_STATE -> pickSaveStateSlot {
                val saveStateUri = activity.viewModel.getRomSaveStateSlotUri(loadedRom, it)
                if (MelonEmulator.saveState(saveStateUri)) {
                    val screenshot = activity.takeScreenshot()
                    activity.viewModel.setRomSaveStateSlotScreenshot(loadedRom, it, screenshot)
                } else {
                    Toast.makeText(activity, activity.getString(R.string.failed_save_state), Toast.LENGTH_SHORT).show()
                }

                activity.resumeEmulation()
            }
            RomPauseMenuOptions.LOAD_STATE -> pickSaveStateSlot {
                if (!it.exists) {
                    Toast.makeText(activity, activity.getString(R.string.cant_load_empty_slot), Toast.LENGTH_SHORT).show()
                } else {
                    val saveStateUri = activity.viewModel.getRomSaveStateSlotUri(loadedRom, it)
                    if (!MelonEmulator.loadState(saveStateUri))
                        Toast.makeText(activity, activity.getString(R.string.failed_load_state), Toast.LENGTH_SHORT).show()
                }

                activity.resumeEmulation()
            }
            RomPauseMenuOptions.USRCHEAT -> openUsrCheatActivity()
            // Read cheat from usrcheat.dat
            RomPauseMenuOptions.CHEATS -> openCheatsActivity()

            RomPauseMenuOptions.RESET -> activity.resetEmulation()
            RomPauseMenuOptions.EXIT -> activity.finish()
        }
    }

    override fun getCrashContext(): Any {
        val sramUri = try {
            activity.viewModel.getRomSramFile(loadedRom)
        } catch (e: Exception) {
            null
        }
        return RomCrashContext(getEmulatorConfiguration(), activity.viewModel.getRomSearchDirectory()?.toString(), loadedRom.uri, sramUri)
    }

    override fun dispose() {
        cheatsLoadDisposable?.dispose()
    }

    private fun loadRomCheats(rom: Rom): Maybe<List<Cheat>> {
        return Maybe.create<List<Cheat>> { emitter ->
            val romInfo = activity.viewModel.getRomInfo(rom)
            if (romInfo == null) {
                emitter.onComplete()
                return@create
            }

            val liveData = activity.viewModel.getRomEnabledCheats(romInfo)
            var observer: Observer<List<Cheat>>? = null
            observer = Observer {
                if (it == null) {
                    emitter.onComplete()
                } else {
                    emitter.onSuccess(it)
                }
                liveData.removeObserver(observer!!)
            }
            liveData.observeForever(observer)
        }.subscribeOn(activity.schedulers.uiThreadScheduler).observeOn(activity.schedulers.backgroundThreadScheduler)
    }

    private fun pickSaveStateSlot(onSlotPicked: (SaveStateSlot) -> Unit) {
        val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy", ConfigurationCompat.getLocales(activity.resources.configuration)[0])
        val timeFormatter = SimpleDateFormat("kk:mm:ss", ConfigurationCompat.getLocales(activity.resources.configuration)[0])
        val slots = activity.viewModel.getRomSaveStateSlots(loadedRom)
        var dialog: AlertDialog? = null
        var adapter: SaveStateListAdapter? = null

        adapter = SaveStateListAdapter(slots, picasso, dateFormatter, timeFormatter, {
            dialog?.cancel()
            onSlotPicked(it)
        }) {
            activity.viewModel.deleteRomSaveStateSlot(loadedRom, it)
            val newSlots = activity.viewModel.getRomSaveStateSlots(loadedRom)
            adapter?.updateSaveStateSlots(newSlots)
        }

        dialog = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.save_slot))
                .setAdapter(adapter) { _, _ ->
                }
                .setNegativeButton(R.string.cancel) { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
                .setOnCancelListener { activity.resumeEmulation() }
                .show()
    }

    private fun openCheatsActivity() {
        activity.openCheats(loadedRom) {
            cheatsLoadDisposable?.dispose()
            cheatsLoadDisposable = loadRomCheats(loadedRom).observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        MelonEmulator.setupCheats(it.toTypedArray())
                        activity.resumeEmulation()
                    }
        }
    }

    private fun openUsrCheatActivity(){
        activity.openUsrCheats(loadedRom) {
//            cheatsLoadDisposable?.dispose()
//            cheatsLoadDisposable = loadRomCheats(loadedRom).observeOn(AndroidSchedulers.mainThread())
//                .subscribe {
//                    MelonEmulator.setupCheats(it.toTypedArray())
//                    activity.resumeEmulation()
//                }
        }
    }
    private data class RomCrashContext(val emulatorConfiguration: EmulatorConfiguration, val romSearchDirUri: String?, val romUri: Uri, val sramUri: Uri?)
}