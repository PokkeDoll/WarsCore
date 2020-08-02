package hm.moe.pokkedoll.warscore.events

import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}

class PlayerUnfreezeEvent(val player: Player, var walkSpeed: Float = 0.2f, var flySpeed: Float = 0.1f) extends Event {

  def getPlayer(): Player = player

  def getWalkSpeed(): Float = walkSpeed

  def setWalkSpeed(walkSpeed: Float): Unit = this.walkSpeed = walkSpeed

  def getFlySpeed(): Float = flySpeed

  def setFlySpeed(flySpeed: Float): Unit = this.flySpeed = flySpeed

  override def getHandlers: HandlerList = PlayerUnfreezeEvent.handlers
}

object PlayerUnfreezeEvent {
  val handlers = new HandlerList()
  def getHandlerList: HandlerList = handlers
}
