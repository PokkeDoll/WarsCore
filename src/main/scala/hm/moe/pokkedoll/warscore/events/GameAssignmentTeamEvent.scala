package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

import scala.collection.mutable

/**
 * チーム決めがされた時に呼ばれるイベント
 *
 * @param game   ゲーム
 * @param players 参加したプレイヤー
 * @param data   データ
 * @tparam T GamePlayerData
 */
class GameAssignmentTeamEvent[T](private val game: Game, private val players: Array[Player], private var data: mutable.Map[Player, T]) extends Event {

  def getGame: Game = game

  def getPlayers: Array[Player] = players

  def getData: mutable.Map[Player, T] = data

  def setData(data: mutable.Map[Player, T]): Unit = this.data = data

  override def getHandlers: HandlerList = GameAssignmentTeamEvent.handlers
}

object GameAssignmentTeamEvent {
  val handlers = new HandlerList()

  def getHandlerList: HandlerList = handlers
}