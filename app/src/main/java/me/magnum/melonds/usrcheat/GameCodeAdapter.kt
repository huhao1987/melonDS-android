package hh.game.usrcheatreader.adapters

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hh.game.usrcheat_android.usrcheat.GameCode
import hh.game.usrcheat_android.usrcheat.Gamedetail
import me.magnum.melonds.R

class GameCodeAdapter(var context: Context, var issinglechoosen:Boolean,var gamecodelist:ArrayList<GameCode>, var oncheckListener: GameCodeAdapter.onCheckListener):RecyclerView.Adapter<GameCodeAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
            .inflate(R.layout.row_cheat, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        var gamecode=gamecodelist[position]
        holder.cheat.text=gamecode.Name+" "+gamecode.Desc
//        var strcodelist=ArrayList<String>()
//        gamecode.codes?.forEach {
//            strcodelist.add(TextUtils.join(" ",it))
//        }
//        holder.cheatcode.text=TextUtils.join("\n",strcodelist)
//        if(issinglechoosen){
//            holder.cheatradio.visibility=View.VISIBLE
//            holder.cheatcheck.visibility=View.GONE
//
//        }
//        else {
            holder.cheatcheck.visibility=View.VISIBLE
            holder.cheatradio.visibility=View.GONE
            holder.cheatcheck.setOnCheckedChangeListener { compoundButton, boolean ->
                when (boolean) {
                    true -> {
                        gamecode.isCodeEnabled = true
                    }
                    false -> {
                        gamecode.isCodeEnabled = false
                    }
                }
                gamecodelist.set(position, gamecode)
                oncheckListener.onCheck(gamecodelist)
            }
            holder.cheatcheck.isChecked = gamecode.isCodeEnabled
//        }
    }

    override fun getItemCount(): Int {
        return gamecodelist.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        var view=view
        var cheat=view.findViewById<TextView>(R.id.cheat)
        var cheatcode=view.findViewById<TextView>(R.id.cheatcode)
        var cheatcheck=view.findViewById<CheckBox>(R.id.cheatcheck)
        var cheatradio=view.findViewById<RadioButton>(R.id.cheatradio)
    }
    interface onCheckListener{
        fun onCheck(codelist:ArrayList<GameCode>)
    }
}