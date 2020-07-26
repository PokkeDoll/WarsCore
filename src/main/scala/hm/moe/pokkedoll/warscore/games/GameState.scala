package hm.moe.pokkedoll.warscore.games

/**
 * @author Emorard
 * @version 1.0
 */
object GameState {
  case object DISABLE extends GameState("disable", "無効化", false)
  case object INIT extends GameState("init", "初期化中",false)
  case object WAIT extends GameState("wait", "受付中", true)
  case object READY extends GameState("ready", "準備中", true)
  case object PLAY extends GameState("play", "試合中!",true)
  case object PLAY2 extends GameState("play2", "試合中(参加不可)",false)
  case object END extends GameState("end", "終了", false)
}

sealed abstract class GameState(val name: String, val title: String, val join: Boolean)
