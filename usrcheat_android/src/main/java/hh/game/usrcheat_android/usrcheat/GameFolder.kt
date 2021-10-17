package hh.game.usrcheat_android.usrcheat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GameFolder(
    var isSingleChoosen:Boolean=false,
    var Name: String? = "",
    var Desc: String? = null,
    var numOfCodes:Int=0,
    var codelist:ArrayList<GameCode>?=null
): Parcelable