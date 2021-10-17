package hh.game.usrcheat_android.usrcheat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class GameCode(
    var codes:ArrayList<ArrayList<String>>?=null,
    var codesstr:String?=null,
    var numOfCodes: Int = 0,
    var isCodeEnabled: Boolean = false,
    var Name: String? = null,
    var Desc: String? = null
) : Parcelable