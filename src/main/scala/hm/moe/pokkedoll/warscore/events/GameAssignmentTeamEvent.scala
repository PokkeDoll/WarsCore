package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.{Game, GameTeam}
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

import scala.collection.mutable

/**
 * チーム決めがされた時に呼ばれるイベント
 *
 * @param game    ゲーム
 * @param players 参加したプレイヤー
 * @param data    データ
 */
class GameAssignmentTeamEvent(private val game: Game, private val players: Array[Player], private var data: mutable.Map[Player, GameTeam]) extends Event {

  def getGame: Game = game

  def getPlayers: Array[Player] = players

  def getData: mutable.Map[Player, GameTeam] = data

  def setData(data: mutable.Map[Player, GameTeam]): Unit = this.data = data

  override def getHandlers: HandlerList = GameAssignmentTeamEvent.handlers
}

object GameAssignmentTeamEvent {
  val handlers = new HandlerList()

  def getHandlerList: HandlerList = handlers
}