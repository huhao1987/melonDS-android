package hh.game.usrcheat_android.usrcheat

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import hh.game.usrcheatreader.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import kotlin.collections.ArrayList

/**
 * The util for usrcheat.dat
 */
class UsrCheatUtils {
    companion object {
        private val headersize = 0x100

        //The real title is start from the address
        private val titlestartaddress = 0x10

        //The end address of title(Should be the limitation)
        private val titleendaddress = 0x4a
        private var cheatdbtitle: String? = null
        private var header: ByteArray? = null
        private var wholedatabase: ByteArray? = null
        var fileuri:Uri?=null

        /**
         * Init the class
         */
        fun initwithAsset(context: Context, file: String) {
            context.assets.open(file).apply {
                header = ByteArray(headersize)
                read(header)
                reset()
                wholedatabase = readBytes()
                close()
            }
        }
        @Throws(IllegalArgumentException::class,IndexOutOfBoundsException::class)
        fun init(context: Context,uri: Uri?){
                uri?.openInputStream(context)?.apply {
                    wholedatabase = readBytes()
                    header= wholedatabase?.copyOfRange(0, headersize)
                    close()
                }
        }

        @WorkerThread
        fun Uri.openInputStream(context: Context): InputStream? {
            return try {
                context.contentResolver.openInputStream(this)
            } catch (e: IOException) {
                null
            }
        }

        fun Uri.toDocumentFile(context: Context) = DocumentFile.fromSingleUri(context, this)


        @Throws(Exception::class)
        fun getEndPointer(): Int {
            return wholedatabase!!.size
        }

        fun getCheatDBname(): String? {
            if (cheatdbtitle == null)
                header?.apply {
                    var titletotalheader = copyOfRange(titlestartaddress, titleendaddress)
                    return getStringBytearray(titletotalheader).toString(Charsets.UTF_8)
                }
            else return cheatdbtitle
            return null
        }

        fun getStringBytearray(bytes: ByteArray, startadd: Int = 0, indexat: Int = 0): ByteArray {
            var endadd = bytes.indexOf(indexat.toByte())

            if (endadd == 0) endadd = bytes.size - 1
            return bytes.copyOfRange(startadd, endadd)
        }

        fun getCodeOffset() = header?.copyOfRange(0x0C, 0x10)!!.little2int()

        fun getSinglePointer(singleblock: ByteArray): Gamedetail {
            var gamedetail = Gamedetail()
            gamedetail.gameId = (singleblock.copyOfRange(0, 4).toString(Charsets.UTF_8))
            gamedetail.gameIdNum =
                singleblock.copyOfRange(0x04, 0x08).toList().reversed().toByteArray().toHex()
            gamedetail.pointer = singleblock.copyOfRange(0x08, 0x0C).little2int()
            return gamedetail
        }


        fun SplitGames(): ArrayList<Gamedetail> {
            var gamePointers = ArrayList<Gamedetail>()
            val input = ByteArrayInputStream(wholedatabase)
            input.skip(getCodeOffset().toLong())
            var tmp = ByteArray(0x10)
            while (true) {
                input.read(tmp)
                if (tmp[0].toInt() == 0) {
                    break
                }
                gamePointers.add(getSinglePointer(tmp))
            }
            input.close()
            return gamePointers
        }


        fun getSingleTitle(gamedetail: Gamedetail, nextpointer: Int): Gamedetail {
            var tempbytelist = wholedatabase!!.copyOfRange(gamedetail.pointer, nextpointer).toList()

            //game title
            var startindex = 0
            var endindex = tempbytelist.indexOf(0)
            gamedetail.gameTitle =
                tempbytelist.subList(startindex, endindex).toByteArray().toString(Charsets.UTF_8)

            //number of codes
            startindex = (endindex+1).align()
            endindex = startindex + 2
            gamedetail.numItems =
                tempbytelist.subList(startindex, endindex).toByteArray().little2short().toInt()

            //Is game enabled
            startindex = endindex
            endindex = startindex + 2
            var intgamenabled =
                tempbytelist.subList(startindex, endindex).toByteArray().get(1).toString(16)
            gamedetail.isgameEnabled = if (intgamenabled.equals("0")) false else true

            //Master code for the game
            startindex = endindex
            endindex = endindex + (4 * 8)
            gamedetail.masterCode =
                tempbytelist.subList(startindex, endindex).chunked(4) as ArrayList<ArrayList<Byte>>
            gamedetail.codepointer = gamedetail.pointer + endindex
            return gamedetail
        }

        fun getCheatCodes(context: Context,gamedetail: Gamedetail, nextpointer: Int): ArrayList<GameFolder> {
            var gameFolderList = ArrayList<GameFolder>()
            try {
                if(wholedatabase==null)
                    init(context,fileuri)
                var templist = wholedatabase!!.toList().subList(gamedetail.codepointer, nextpointer)
                var rootfolder = GameFolder()
                rootfolder.codelist = ArrayList()
                var index = 0
                while (index < gamedetail.numItems) {
                    var codechuck = templist.subList(0, 2).toByteArray().little2short()
                    templist = templist.drop(2).toMutableList()
                    var checkisfolder = templist.subList(0, 2).toByteArray().get(1).toString(16)
                    templist = templist.drop(2).toMutableList()
                    when (checkisfolder) {
                        //11 is folder with single chosen,10 is folder without single chosen
                        "10", "11" -> {
                            var processfolder = processFolder(templist)
                            templist = processfolder.get("list") as List<Byte>
                            var tempgamefolder = processfolder.get("gamefolder") as GameFolder
                            if (checkisfolder.equals(10)) tempgamefolder.isSingleChoosen = false
                            else if (checkisfolder.equals(11)) tempgamefolder.isSingleChoosen = true
                            tempgamefolder.numOfCodes = codechuck.toInt()
                            tempgamefolder.codelist = ArrayList()
                            for (n in 0..tempgamefolder.numOfCodes - 1) {
                                codechuck = templist.subList(0, 2).toByteArray().little2short()
                                templist = templist.drop(2).toMutableList()
                                checkisfolder =
                                    templist.subList(0, 2).toByteArray().get(1).toString(16)
                                templist = templist.drop(2).toMutableList()
                                var processcode = processCheatCode(templist)
                                templist = processcode.get("list") as List<Byte>
                                tempgamefolder.codelist?.add(processcode.get("gamecode") as GameCode)
                                index++
                            }
                            gameFolderList.add(tempgamefolder)
                        }
                        //01 for cheat code with enable, 00 for cheat code with disable
                        "0", "1" -> {
                            var processcode = processCheatCode(templist)
                            templist = processcode.get("list") as List<Byte>
                            var tempgamecode = processcode.get("gamecode") as GameCode
                            rootfolder.codelist?.add(tempgamecode)
                        }
                    }
                    if (index == gamedetail.numItems - 1)
                        gameFolderList.add(rootfolder)
                    index++
                }
            }
            catch (e:Exception){

            }
            return gameFolderList
        }

        fun processFolder(orignlist: List<Byte>): HashMap<String, Any> {
            var templist = orignlist
            var gamefolder = GameFolder()
            var foldernameend = templist.indexOf(0)
            gamefolder.Name =
                templist.subList(0, foldernameend).toByteArray().toString(Charsets.UTF_8)
            templist = templist.drop(foldernameend + 1).toList()
            var folderdesend = templist.indexOf(0)
            gamefolder.Desc =
                templist.subList(0, folderdesend).toByteArray().toString(Charsets.UTF_8)
            templist = orignlist
            templist = templist.drop((foldernameend + 1 + folderdesend + 1).align()).toList()
            gamefolder.codelist = ArrayList()
            var tempmap = HashMap<String, Any>().also {
                it.set("list", templist)
                it.set("gamefolder", gamefolder)
            }
            return tempmap
        }

        fun processCheatCode(orignlist: List<Byte>): HashMap<String, Any> {
            var templist = orignlist
            var gameCode = GameCode()
            var codenameend = templist.indexOf(0)
            gameCode.Name = templist.subList(0, codenameend).toByteArray().toString(Charsets.UTF_8)
            templist = templist.drop(codenameend + 1).toList()
            var codedesend = templist.indexOf(0)
            gameCode.Desc = templist.subList(0, codedesend).toByteArray().toString(Charsets.UTF_8)
            templist = orignlist
            templist = templist.drop((codenameend + 1 + codedesend + 1).align()).toList()
            //Each 4 bytes are inclued in one block, xxxxxxxx yyyyyyyy, xxxxxxxx and yyyyyyyy are both blocks, so two blocks are for one cheat code
            gameCode.numOfCodes = templist.subList(0, 2).toByteArray().little2short().toInt()
            templist = templist.drop(2.align()).toList()
            gameCode.codes = ArrayList<ArrayList<String>>()
            var temponecheat = ArrayList<String>()
            var tempcodestr=""
            var temp = templist.subList(0, gameCode.numOfCodes * 4).chunked(4)
            templist = templist.drop((gameCode.numOfCodes * 4)).toList()
            temp.forEachIndexed { index, list ->
                var cheatcode = list.reversed().toByteArray().toHex()
                temponecheat.add(cheatcode)
                if (index % 2 != 0 || index == temp.size - 1) {
                    gameCode.codes?.add(temponecheat)
                    temponecheat = ArrayList()
                }
                tempcodestr=(tempcodestr+"$cheatcode ")
            }
            gameCode.codesstr=tempcodestr.trim()
            var tempmap = HashMap<String, Any>().also {
                it.set("list", templist)
                it.set("gamecode", gameCode)
            }
            return tempmap
        }


        fun ByteArray.toHex(): String = asUByteArray().joinToString("") {
            it.toString(16).padStart(2, '0')
        }

        fun getGameTitles(block: ArrayList<Gamedetail>): ArrayList<Gamedetail> {
            var gamedetails = ArrayList<Gamedetail>()
            block.forEachIndexed { index, gamedetail ->
                gamedetails.add(
                    getSingleTitle(
                        gamedetail,
                        if (index < block.size - 1) block[index + 1].pointer else wholedatabase!!.size
                    )
                )
            }
            return gamedetails
        }


        @Throws(NullPointerException::class)
        fun getGametitles(): ArrayList<Gamedetail> {
            var block = SplitGames()
            return getGameTitles(block)
        }
    }

}