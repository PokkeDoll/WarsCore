package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.{Game, GameTeam}
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

import scala.collection.mutable


class GameAssignmentTeamEvent[T <: {def getTeam: GameTeam}](private val game: Game, private var data: mutable.Map[Player, T]) extends Event {

  def getGame: Game = game

  def getData: mutable.Map[Player, T] = data

  def setData(data: mutable.Map[Player, T]): Unit = this.data = data

  override def getHandlers: HandlerList = GameAssignmentTeamEvent.handlers
}

object GameAssignmentTeamEvent {
  val handlers = new HandlerList()

  def getHandlerList: HandlerList = handlers
}