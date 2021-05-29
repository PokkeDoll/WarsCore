package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

class GamePostRespawnEvent(val game: Game, val player: Player) extends Event {
  override def getHandlers: HandlerList = GamePostRespawnEvent.handlers
}


object GamePostRespawnEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
