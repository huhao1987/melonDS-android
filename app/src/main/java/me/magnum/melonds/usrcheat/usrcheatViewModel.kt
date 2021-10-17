package me.magnum.melonds.usrcheat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.CacheDoubleUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hh.game.usrcheat_android.usrcheat.GameCode
import hh.game.usrcheat_android.usrcheat.GameFolder
import hh.game.usrcheat_android.usrcheat.Gamedetail
import hh.game.usrcheat_android.usrcheat.UsrCheatUtils
import hh.game.usrcheat_android.usrcheat.UsrCheatUtils.Companion.toDocumentFile
import hh.game.usrcheat_android.usrcheat.UsrCheatUtils.Companion.toHex
import kotlinx.coroutines.launch
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.domain.model.Rom
import me.magnum.melonds.parcelables.RomInfoParcelable

class usrcheatViewModel : ViewModel() {
    private var gametitlelistdata: MutableLiveData<ArrayList<Gamedetail>>? = null
    var gametitlelisterrordata: MutableLiveData<Throwable>? = null
    private var gamedetaildata: MutableLiveData<Gamedetail>? = null
    private var gamecheat: MutableLiveData<ArrayList<GameFolder>>? = null
    private var tag: String? = null
    private var LASTMODIFIED = "usrcheatlastmodified"
    private var GAMETITLESTAG = "gametitles"
    private var DETAILSUBTAG = "detail"
    private var CHEATSUBTAG = "cheat"
    private var ENABLECHEATSUBTAG = "enablecheat"

    /**
     * Get game titles
     */
    fun getGametitleList(context: Context, rom: Rom): LiveData<ArrayList<Gamedetail>> {
        if (gametitlelistdata != null) return gametitlelistdata!!
        UsrCheatUtils.fileuri = Uri.parse(rom.parentTreeUri.toString() + "%2Fusrcheat.dat")
        var documentfile = UsrCheatUtils.fileuri?.toDocumentFile(context)
        documentfile?.apply {
            if (!documentfile.lastModified().toString()
                    .equals(CacheDoubleUtils.getInstance().getString(LASTMODIFIED))
            ) {
                gametitlelisterrordata = MutableLiveData()
                gametitlelisterrordata?.postValue(Throwable("usrcheat modified"))
                CacheDoubleUtils.getInstance().clear()
            }
        }
        gametitlelistdata = MutableLiveData()
        var gametitles = CacheDoubleUtils.getInstance().getString(GAMETITLESTAG, null)
        if (gametitles != null) {
            var list: ArrayList<Gamedetail> =
                Gson().fromJson(gametitles, object : TypeToken<ArrayList<Gamedetail>>() {}.type)
            if (list.size > 0) {
                gametitlelistdata?.postValue(list)
            } else cleanAllCache()
        } else {
            viewModelScope.launch {
                try{
                UsrCheatUtils.init(
                    context,
                    UsrCheatUtils.fileuri
                )
                    var gametitlelist = UsrCheatUtils.getGametitles()
                    CacheDoubleUtils.getInstance().put(GAMETITLESTAG, Gson().toJson(gametitlelist))
                    var documentfile = UsrCheatUtils.fileuri?.toDocumentFile(context)
                    CacheDoubleUtils.getInstance()
                        .put(LASTMODIFIED, documentfile?.lastModified().toString())
                    gametitlelistdata?.postValue(gametitlelist)
                }
                catch (e:NullPointerException){
                    gametitlelisterrordata= MutableLiveData()
                    gametitlelisterrordata?.postValue(Throwable("null array"))
                }
                catch (e:IndexOutOfBoundsException){
                    gametitlelisterrordata= MutableLiveData()
                    gametitlelisterrordata?.postValue(Throwable("IndexOutOfBoundsException"))
                }
                catch (e:IllegalArgumentException){
                    gametitlelisterrordata= MutableLiveData()
                    gametitlelisterrordata?.postValue(Throwable("usrcheat no found"))
                }
            }
        }
        return gametitlelistdata!!
    }

    /**
     * Get single game detail
     */
    fun getgamedetail(
        context: Context,
        romInfoParcelable: RomInfoParcelable,
        gametitlelist: ArrayList<Gamedetail>
    ): LiveData<Gamedetail> {
        if (gamedetaildata != null) return gamedetaildata!!
        gamedetaildata = MutableLiveData()
        tag = romInfoParcelable.gameCode + romInfoParcelable.headerChecksum.toBigInteger()
            .toByteArray().toHex()
        if (tag != null) {
            var gamedetail = CacheDoubleUtils.getInstance()
                .getParcelable(tag!! + DETAILSUBTAG, Gamedetail.CREATOR)
            if (gamedetail != null && gamedetail.gameTitle != null) {
                gamedetaildata?.postValue(gamedetail)
            } else {
                viewModelScope.launch {
                    var gamedetails = gametitlelist.filter {
                        it.gameId == romInfoParcelable.gameCode && it.gameIdNum!!.equals(
                            romInfoParcelable.headerChecksum.toBigInteger().toByteArray().toHex()
                        )
                    }
                    var gamedetail = Gamedetail()
                    if (gamedetails.size > 0)
                        gamedetail = gamedetails[0]
                    CacheDoubleUtils.getInstance().put(tag!! + DETAILSUBTAG, gamedetail)
                    gamedetaildata?.postValue(gamedetail)
                }
            }
        }
        return gamedetaildata!!
    }

    /**
     * Get usrcheat cheat
     */
    fun getUsrcheat(
        context: Context,
        gametitlelist: ArrayList<Gamedetail>,
        gamedetail: Gamedetail
    ): LiveData<ArrayList<GameFolder>> {
        if (gamecheat != null) return gamecheat!!
        gamecheat = MutableLiveData()
        if (usrceheatUtil.enablegameCheatList != null) gamecheat!!.postValue(usrceheatUtil.enablegameCheatList)
        else {
            if (tag != null) {
                var cache = CacheDoubleUtils.getInstance().getString(tag!! + CHEATSUBTAG, null)
                if (cache != null) {
                    var list: ArrayList<GameFolder> =
                        Gson().fromJson(cache, object : TypeToken<ArrayList<GameFolder>>() {}.type)
                    if (list.size > 0) {
                        usrceheatUtil.enablegameCheatList = list
                        gamecheat?.postValue(list)
                    } else cleanGameCache(tag!! + CHEATSUBTAG)
                } else {
                    viewModelScope.launch {
                        var index = gametitlelist.indexOf(gamedetail)
                        var cheatlist = UsrCheatUtils.getCheatCodes(
                            context,
                            gamedetail!!,
                            if (index < gametitlelist.size - 1) gametitlelist[index + 1].codepointer else UsrCheatUtils.getEndPointer()
                        )
                        usrceheatUtil.enablegameCheatList = cheatlist
                        CacheDoubleUtils.getInstance()
                            .put(tag + CHEATSUBTAG, Gson().toJson(cheatlist))
                        gamecheat?.postValue(cheatlist)
                    }
                }
            }
        }
        return gamecheat!!
    }

    /**
     * Save the cheats to cache
     */
    fun saveCheatlist(list: ArrayList<GameFolder>) {
        tag?.apply {
            CacheDoubleUtils.getInstance().put(this + CHEATSUBTAG, Gson().toJson(list))
        }
    }

    /**
     * Save the enable cheats
     */
    fun saveEnabledlist(list: ArrayList<Cheat>) {
        tag?.apply {
            CacheDoubleUtils.getInstance().put(this + ENABLECHEATSUBTAG, Gson().toJson(list))
        }
    }

    /**
     * Clean all cache include game cheats and game titles
     */
    fun cleanAllCache() {
        CacheDoubleUtils.getInstance().clear()
    }

    /**
     * Clean single game cheats
     */
    fun cleanGameCache(tag: String) {
        CacheDoubleUtils.getInstance().remove(tag + CHEATSUBTAG)
        CacheDoubleUtils.getInstance().remove(tag + DETAILSUBTAG)
        usrceheatUtil.enablegameCheatList = null
    }
}