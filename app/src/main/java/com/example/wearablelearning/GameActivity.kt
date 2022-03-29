package com.example.wearablelearning

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder


/**
 * Game activity.
 */
class GameActivity : AppCompatActivity() {
    companion object {
        var map: Map<String, Any> = emptyMap()
        var states: MutableMap<String, State> = mutableMapOf()
        var transitions: MutableMap<String, Transition> = mutableMapOf()
        var currTransId: String = String()
    }

    lateinit var gameInfo: GameInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameInfo = (intent.getSerializableExtra("gameInfo") as? GameInfo)!!
        var gameInfoOfStartedGame = intent.getSerializableExtra("gameInfoOfStartedGame") as? GameInfo

        var fm: FragmentManager = supportFragmentManager

        mapJson()

        if (gameInfo != null) {
            gameInfo?.gameName = mapGameName()
        }

        mapStates()
        mapTransitions()
        addGamePlayerInfo(fm)

        if(gameInfoOfStartedGame != null &&
            gameInfoOfStartedGame.gamePin.equals(gameInfo.gamePin) &&
            gameInfoOfStartedGame.name.equals(gameInfo.name) &&
            gameInfoOfStartedGame.userName.equals(gameInfo.userName) &&
            gameInfoOfStartedGame.team.equals(gameInfo.team) &&
            gameInfoOfStartedGame.player.equals(gameInfo.player)) {

            callTransition(gameInfoOfStartedGame.currTrans!!)
        } else {
            var idx = 1

            changeState(fm, idx)
            changeTransition(fm, getOutputTransition(idx))
        }
    }

    fun callTransition(transId: String) {
        currTransId = transId

        val nextStateId: Int = stateWithInputTransition(transId)
        var fm: FragmentManager = supportFragmentManager

        changeState(fm, nextStateId)
        val outputTransition = getOutputTransition(nextStateId)

        // if there is only one output transition
        if(!outputTransition.contains(",") or isAllSameType(outputTransition)) {
            changeTransition(fm, outputTransition)
        }
    }

    private fun mapJson() {
        val jsonContents: String = resources.openRawResource(R.raw.game12) //TODO
            .bufferedReader().use { it.readText() }

        map = StringUtils.parseJsonWithGson(jsonContents)
    }

    private fun mapGameName(): String {
        val gameInfo: String = map.getValue("game").toString()
        val gameName: String = gameInfo.substringAfter("game_name=").substringBefore(", team_cnt=")

        return gameName
    }

    private fun mapStates() {
        val stateList: ArrayList<Any> = map.getValue("states") as ArrayList<Any>

        for(s in stateList) {
            var stateStr: String = s.toString()
            var stateId: String = stateStr.substringAfter("{id=").substringBefore(", name=")

            val newState = State(
                stateId,
                stateStr.substringAfter(", name=").substringBefore(", players="),
                stateStr.substringAfter(", players=").substringBefore(", type="),
                stateStr.substringAfter(", type=").substringBefore(", content="),
                stateStr.substringAfter(", content=").substringBefore(", other="),
                stateStr.substringAfter(", other=").substringBefore(", trans_inputs="),
                stateStr.substringAfter(", trans_inputs=").substringBefore(", trans_outputs="),
                stateStr.substringAfter(", trans_outputs=").substringBefore("}")
            )

            states[stateId] = newState
        }
    }

    private fun mapTransitions() {
        val transList: ArrayList<Any> = map.getValue("transitions") as ArrayList<Any>

        for(t in transList) {
            var transStr: String = t.toString()
            var transId: String = transStr.substringAfter("{id=").substringBefore(", type=")

            val newTransition = Transition(
                transId,
                transStr.substringAfter(", type=").substringBefore(", content="),
                transStr.substringAfter(", content=").substringBefore("}")
            )

            transitions[transId] = newTransition
        }
    }

    private fun getOutputTransition(idx: Int): String {
        var outputs: List<String> = states["state_$idx"]!!.trans_outputs

        if(outputs.size == 1) {
            return outputs[0]
        } else {
           return outputs.joinToString()
        }
    }

    private fun isAllSameType(transStr: String): Boolean {
        val transList = transStr.replace(" ", "").split(',')
        val transType = transitions[transList[0]]?.type.toString()

        for(t in transList) {
            if(transitions[t]?.type.toString() != transType) {
                return false
            }
        }

        return true
    }

    private fun stateWithInputTransition(transId: String): Int {
        var idx = 0

        for(i in 1..states.size) {
            if(states["state_$i"]?.trans_inputs?.contains(transId) == true) {
                return i
            }
        }

        return idx
    }

    private fun addGamePlayerInfo(fm: FragmentManager) {
        val ft: FragmentTransaction = fm.beginTransaction()
        ft.replace(R.id.frameLayout0, GamePlayerInfoFragment())
        ft.commit()
    }

    private fun changeState(fm: FragmentManager, idx: Int) {
        val ft: FragmentTransaction = fm.beginTransaction()
        val bundle = Bundle()

        val content = states["state_$idx"]?.content.toString()
        val type = states["state_$idx"]?.type.toString()

        if(type.contains("text")) {
            val fragInfo = StateTextFragment()
            bundle.putString("content", content)
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout1, fragInfo)
            ft.commit()
        }
        else if(type.contains("photo")) {
            val image = states["state_$idx"]?.other.toString()

            val fragInfo = StatePhotoFragment()
            bundle.putString("content", content)
            bundle.putString("image", image)
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout1, fragInfo)
            ft.commit()
        }
        else if(type.contains("sound")) {
            val sound = states["state_$idx"]?.other.toString()

            val fragInfo = StateSoundFragment()
            bundle.putString("content", content)
            bundle.putString("sound", sound)
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout1, fragInfo)
            ft.commit()
        }
        else if(type.contains("video")) {
            val video = states["state_$idx"]?.other.toString()

            val fragInfo = StateVideoFragment()
            bundle.putString("content", content)
            bundle.putString("video", video)
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout1, fragInfo)
            ft.commit()
        }
    }

    private fun changeTransition(fm: FragmentManager, transition: String) {
        var type = transitions[transition]?.type.toString()
        var id = transitions[transition]?.id.toString()
        var content = transitions[transition]?.content.toString()

        //if multiple transitions
        if(transition.contains(",")) {
            val transList = transition.replace(" ", "").split(",")
            type = transitions[transList[0]]?.type.toString()
            id = ""
            content = ""

            for(t in transList) {
                if(StringUtils.isEmptyOrBlank(content)) {
                    content = transitions[t]?.content.toString()
                    id = transitions[t]?.id.toString()
                }
                else {
                    content = content + ";;" + transitions[t]?.content.toString()
                    id = id + ";;" + transitions[t]?.id.toString()
                }
            }
        }

        val ft: FragmentTransaction = fm.beginTransaction()
        val bundle = Bundle()
        bundle.putString("id", id)
        bundle.putString("content", content)

        if(type.contains("button_press")) {
            val fragInfo = TransitionBtnPressFragment()
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout2, fragInfo)
            ft.commit()
        }
        else if(type.contains("color_sequence")) {
            val fragInfo = TransitionSequenceFragment()
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout2, fragInfo)
            ft.commit()
        }
        else if(type.contains("text_entry")) {
            val fragInfo = TransitionTextEntryFragment()
            fragInfo.arguments = bundle
            ft.replace(R.id.frameLayout2, fragInfo)
            ft.commit()
        }
        else {
            val fragInfo = TransitionEndGameFragment()
            ft.replace(R.id.frameLayout2, fragInfo)
            ft.commit()
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this, R.style.Theme_WearableLearning_AlertDialog)
            .setMessage(resources.getString(R.string.confirm_back_text))
            .setNegativeButton(resources.getString(R.string.no_text)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(resources.getString(R.string.yes_text)) { _, _ ->
                val intent = Intent(this@GameActivity, MainActivity::class.java)

               /*
                    FLAG_ACTIVITY_CLEAR_TOP:
                    If set, and the activity being launched is already running in the current task,
                    then instead of launching a new instance of that activity, all of the other
                    activities on top of it will be closed and this Intent will be delivered to the
                    (now on top) old activity as a new Intent.
                */
                gameInfo.currTrans = currTransId

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("gameInfo", gameInfo)
                intent.putExtra("gameInfoOfStartedGame", gameInfo)
                startActivity(intent)
            }
            .show()
    }
}
