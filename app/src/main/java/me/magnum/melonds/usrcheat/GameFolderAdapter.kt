package hh.game.usrcheatreader.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hh.game.usrcheat_android.usrcheat.GameCode
import hh.game.usrcheat_android.usrcheat.GameFolder
import hh.game.usrcheat_android.usrcheat.Gamedetail
import me.magnum.melonds.R

class GameFolderAdapter(
    var context: Context,
    var gamefolderlist: ArrayList<GameFolder>,
    var onclickListener: onClickListener,
    var oncheckListener: onCheckListener
) : RecyclerView.Adapter<GameFolderAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.row_folder, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var gamefolder = gamefolderlist[position]
        //This means the codes in root folder
        if (gamefolder.Name.equals(""))
            holder.cheat.text = "root"
        else
            holder.cheat.text =
                gamefolder.Name + " " + gamefolder.Desc + if (gamefolder.isSingleChoosen) context.getString(R.string.singlechosen) else ""

        gamefolder.codelist?.apply {
            var adapter = GameCodeAdapter(
                context,
                gamefolder.isSingleChoosen,
                this,
                object : GameCodeAdapter.onCheckListener {
                    override fun onCheck(codelist: ArrayList<GameCode>) {
                        gamefolder.codelist = codelist
                        gamefolderlist.set(holder.absoluteAdapterPosition, gamefolder)
                        oncheckListener.onCheck(gamefolderlist,)
                    }
                })
            var llm = LinearLayoutManager(context)
            llm!!.orientation = RecyclerView.VERTICAL
            holder.codelist.layoutManager = llm
            holder.codelist.adapter = adapter
            holder.cheat.setOnClickListener {
                holder.codelist.visibility = if (holder.codelist.visibility == View.GONE) {
                    holder.foldericon.setImageResource(R.drawable.folder_opened)
                    View.VISIBLE
                } else {
                    holder.foldericon.setImageResource(R.drawable.folder)
                    View.GONE
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return gamefolderlist.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var view = view
        var foldericon = view.findViewById<ImageView>(R.id.foldericon)
        var cheat = view.findViewById<TextView>(R.id.cheat)
        var codelist = view.findViewById<RecyclerView>(R.id.codelist)
    }

    interface onClickListener {
        fun onclick(view: View, gamedetail: Gamedetail, position: Int, nextpointer: Int)
    }

    interface onCheckListener {
        fun onCheck(folderlist: ArrayList<GameFolder>)
    }
}