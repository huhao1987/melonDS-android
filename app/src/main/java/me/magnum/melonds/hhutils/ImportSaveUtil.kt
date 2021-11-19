package me.magnum.melonds.hhutils

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import hh.game.usrcheat_android.usrcheat.UsrCheatUtils.Companion.openInputStream
import hh.game.usrcheat_android.usrcheat.UsrCheatUtils.Companion.openOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.magnum.melonds.R
import me.magnum.melonds.di.AppModule
import me.magnum.melonds.domain.model.Rom
import me.magnum.melonds.domain.model.RuntimeConsoleType
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ImportSaveUtil {
    private var context: Context? = null
    private var uri: Uri? = null
    private var rom: Rom? = null
    fun init(context: Context, importuri: Uri, rom: Rom) {
        this.context = context
        uri = importuri
        this.rom = rom
        GlobalScope.launch {
            uri?.openInputStream(context)?.apply {
                var desmumesavestr = "|<--Snip"
                var wholebytearray = readBytes()
                var text = wholebytearray.toString(Charsets.UTF_8)
                var index = text.indexOf(desmumesavestr)
                if (index != -1) {
                    wholebytearray = wholebytearray.copyOfRange(0, index + 1)
                }
                AppModule.provideUriHandler(context).apply {
                    var rootfolder = DocumentFile.fromTreeUri(context, rom.parentTreeUri)
                    var gamesaveuri = rom.uri.toString().replace(".nds", ".sav").toUri()
                    if (fileExists(gamesaveuri)) {
                        withContext(Dispatchers.Main) {
                            AlertDialog.Builder(context)
                                .setTitle(R.string.import_battery_save)
                                .setMessage(R.string.import_save_dialog_content)
                                .setPositiveButton(
                                    R.string.ok,
                                    DialogInterface.OnClickListener { dialogInterface, i ->
                                        var gamefilename = URLDecoder.decode(
                                            DocumentFile.fromSingleUri(
                                                context,
                                                gamesaveuri
                                            )?.name, "UTF-8"
                                        )
                                        var newbakfiledisplayname =
                                            gamefilename + ".bak" + SimpleDateFormat("_yyyy_MM_dd_hh_mm_ss").format(
                                                Date()
                                            )
                                        var newbackupsavefile =
                                            rootfolder?.createFile(
                                                "application/bak",
                                                newbakfiledisplayname
                                            )
                                        //Backup old save file if it exist
                                        newbackupsavefile?.uri?.openOutputStream(context)?.apply {
                                            try {
                                                write(
                                                    gamesaveuri.openInputStream(context)
                                                        ?.readBytes()
                                                )
                                            } catch (e: Exception) {

                                            }
                                            close()
                                        }
                                        gamesaveuri.openOutputStream(context)?.apply {
                                            try {
                                                write(wholebytearray)
                                            } catch (e: Exception) {

                                            }
                                            close()
                                        }
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.imported_battery_save),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        dialogInterface.dismiss()
                                    })
                                .setNegativeButton(
                                    R.string.cancel,
                                    DialogInterface.OnClickListener { dialogInterface, i ->
                                        dialogInterface.dismiss()
                                    })
                                .show()
                        }
                    } else {
                        DocumentFile.fromSingleUri(context, rom.uri)?.name?.replace(".nds", ".sav")
                            ?.let {
                                rootfolder?.createFile("application/sav", it)
                            }
                        gamesaveuri.openOutputStream(context)?.apply {
                            try {
                                write(wholebytearray)
                            } catch (e: Exception) {

                            }
                            close()
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.imported_battery_save),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        }
    }

}