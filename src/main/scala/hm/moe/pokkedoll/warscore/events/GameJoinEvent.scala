package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.WPlayer
import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.event.{Event, HandlerList}

/**
 * プレイヤーが参加する前に呼ばれるイベント
 */
class GameJoinEvent(private val game: Game, private val wp: WPlayer, private var accept: Boolean = false, private var cancelReason: String = "") extends Event {

  def getGame: Game = game

  def getWPlayer: WPlayer = wp

  @Deprecated
  def isAccept: Boolean = accept

  @Deprecated
  def setAccept(accept: Boolean): Unit = this.accept = accept

  def getCancelReason: String = cancelReason

  def setCancelReason(cancelReason: String): Unit = this.cancelReason = cancelReason

  override def getHandlers: HandlerList = GameJoinEvent.handlers
}

object GameJoinEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
