package hm.moe.pokkedoll.warscore.events

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.entity.Player
import org.bukkit.event.{Event, HandlerList}
import org.bukkit.inventory.ItemStack

class GameWeaponDropEvent(val game: Game, val player: Player, private var weapon: ItemStack, private var chance: Double) extends Event {

  def getWeapon: ItemStack = this.weapon
  def setWeapon(weapon: ItemStack): Unit = this.weapon = weapon

  def getChance: Double = this.chance
  def setChance(chance: Double): Unit = this.chance = chance

  override def getHandlers: HandlerList = GameWeaponDropEvent.handlers
}

object GameWeaponDropEvent {
  val handlers = new HandlerList()

  def getHandlerList: HandlerList = handlers
}
