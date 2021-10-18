package me.magnum.melonds.usrcheat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import hh.game.usrcheat_android.usrcheat.GameCode
import hh.game.usrcheat_android.usrcheat.GameFolder
import hh.game.usrcheat_android.usrcheat.Gamedetail
import hh.game.usrcheatreader.adapters.GameFolderAdapter
import me.magnum.melonds.MelonEmulator
import me.magnum.melonds.R
import me.magnum.melonds.common.uridelegates.UriHandler
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.parcelables.RomInfoParcelable
import me.magnum.melonds.parcelables.RomParcelable
import me.magnum.melonds.ui.emulator.EmulatorActivity
import javax.inject.Inject


@AndroidEntryPoint
class CheatsDatActivity : AppCompatActivity() {
    private var codelist = ArrayList<GameCode>()
    private var loading: RelativeLayout? = null
    private var errortext:TextView?=null
    private var romInfoParcelable: RomInfoParcelable? = null
    private var romParcelable: RomParcelable? = null

    @Inject
    lateinit var uriHandler: UriHandler

    companion object {
        const val KEY_ROM_INFO = "key_rom_info"
    }

    private val viewModel: usrcheatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usrcheat)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        errortext=findViewById<TextView>(R.id.errortext)
        loading = findViewById<RelativeLayout>(R.id.loadingarea)
        loading?.visibility = View.VISIBLE
        initcheatdata(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(newintent: Intent?) {
        super.onNewIntent(newintent)
        newintent?.apply {
            initcheatdata(this)
        }
    }

    private fun initcheatdata(i: Intent) {
        var newromonfirparcelable = i.getParcelableExtra<RomInfoParcelable>(KEY_ROM_INFO)
            ?: throw NullPointerException("KEY_ROM_INFO argument is required")
        romParcelable = intent.extras?.getParcelable(EmulatorActivity.KEY_ROM) as RomParcelable?
        if (romInfoParcelable == null || (romInfoParcelable != null && romInfoParcelable != newromonfirparcelable)) {
            romInfoParcelable = newromonfirparcelable
            viewModel.getGametitleList(
                this@CheatsDatActivity, romParcelable!!.rom).observe(this@CheatsDatActivity) { gametitlelist ->
                viewModel.getgamedetail(this@CheatsDatActivity, romInfoParcelable!!, gametitlelist)
                    .observe(this@CheatsDatActivity) { gamedetail ->
                        if(gamedetail.gameTitle==null) {
                            loading?.visibility = View.GONE
                            errortext?.visibility=View.VISIBLE
                            errortext?.text = getString(R.string.usrcheatnocheat)
                        }
                        else {
                            supportActionBar?.title = gamedetail?.gameTitle
                            viewModel.getUsrcheat(this, gametitlelist, gamedetail)
                                .observe(this@CheatsDatActivity) {
                                    if (it.size > 0) {
                                        var adapter = GameFolderAdapter(this@CheatsDatActivity,
                                            it,
                                            object : GameFolderAdapter.onClickListener {
                                                override fun onclick(
                                                    view: View,
                                                    gamedetail: Gamedetail,
                                                    position: Int,
                                                    nextpointer: Int
                                                ) {
                                                }
                                            },
                                            object : GameFolderAdapter.onCheckListener {
                                                override fun onCheck(
                                                    folderlist: ArrayList<GameFolder>
                                                ) {
                                                    usrceheatUtil.enablegameCheatList = folderlist
                                                    viewModel.saveCheatlist(folderlist)
                                                    var tempcodelist=ArrayList<GameCode>()
                                                    folderlist.forEach {
                                                        var clist=it.codelist?.filter {
                                                            it.isCodeEnabled==true
                                                        }
                                                        if(clist!=null&&clist.size>0)
                                                        tempcodelist+=ArrayList(clist)
                                                    }
                                                    this@CheatsDatActivity.codelist = tempcodelist
                                                }
                                            })
                                        var cheatlistview =
                                            findViewById<RecyclerView>(R.id.cheatlistview)
                                        var llm = LinearLayoutManager(this@CheatsDatActivity)
                                        llm!!.orientation = RecyclerView.VERTICAL
                                        cheatlistview.layoutManager = llm
                                        cheatlistview.adapter = adapter
                                    } else {
                                        errortext?.visibility=View.VISIBLE
                                        errortext?.text = getString(R.string.usrcheatnocheat)
                                    }
                                    loading?.visibility = View.GONE
                                }
                        }
                    }
            }

            viewModel.gametitlelisterrordata?.observe(this){
                when(it.message){
                    "usrcheat modified"->{
                        Toast.makeText(this,getString(R.string.usrcheatmoditied),Toast.LENGTH_LONG).show()
                    }
                    "null array","IndexOutOfBoundsException"->{
                        errortext?.visibility=View.VISIBLE
                        errortext?.text = getString(R.string.usrcheatfilewrong)
                        loading?.visibility=View.GONE
                    }
                    "usrcheat no found"->{
                        errortext?.visibility=View.VISIBLE
                        errortext?.text = getString(R.string.usrcheatnotexist)
                        loading?.visibility=View.GONE
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        setupcheats(codelist)
        super.onBackPressed()
    }

    private fun setupcheats(codelist: ArrayList<GameCode>) {
        var cheatarray = ArrayList<Cheat>()

        codelist.forEach {
            if (it.isCodeEnabled) {
                var cheat = Cheat(0, it.Name!!, it.Desc, it.codesstr!!, true)
                cheatarray.add(cheat)
            }
        }
        if (usrceheatUtil.currentcodelist != null && usrceheatUtil.currentcodelist != cheatarray) {
            usrceheatUtil.currentcodelist?.forEach {
                it.enabled = false
            }
            MelonEmulator.setupCheats(usrceheatUtil.currentcodelist!!.toTypedArray())
            MelonEmulator.resumeEmulation()
        }
        usrceheatUtil.currentcodelist = cheatarray
        viewModel.saveEnabledlist(cheatarray)
        MelonEmulator.setupCheats(cheatarray.toTypedArray())
        MelonEmulator.resumeEmulation()
//        Toast.makeText(this, "Cheat enabled", Toast.LENGTH_SHORT).show()
    }
}