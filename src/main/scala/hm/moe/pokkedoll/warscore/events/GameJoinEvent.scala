package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.WPlayer
import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.event.{Event, HandlerList}

/**
 * プレイヤーが参加した後に呼び出されるイベント。
 * プレイヤーが参加が許可されたか拒否されたかの結果がわかる。
 * @param game
 * @param wp
 * @param accept
 */
class GameJoinEvent(private val game: Game, private val wp: WPlayer, private var accept: Boolean = false) extends Event {

  def getGame: Game = game

  def getWPlayer: WPlayer = wp

  def isAccept: Boolean = accept

  def setAccept(accept: Boolean): Unit = this.accept = accept

  override def getHandlers: HandlerList = GameJoinEvent.handlers
}

object GameJoinEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
