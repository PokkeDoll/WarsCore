package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.{Game, GameTeam}
import org.bukkit.event.{Event, HandlerList}

/**
 * 試合が終了したときに呼び出されるイベント。キャンセル不可
 * @param game 試合そのもの
 * @param winner 勝者
 */
class GameEndEvent(private val game: Game, private val winner: GameTeam) extends Event {

  def getGame: Game = game

  def getWinner: GameTeam = winner

  override def getHandlers: HandlerList = GameEndEvent.handlers
}

object GameEndEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
