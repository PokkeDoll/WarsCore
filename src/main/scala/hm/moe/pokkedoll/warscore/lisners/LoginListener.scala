package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener}

class LoginListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onJoin(e: PlayerJoinEvent): Unit = {

  }

  @EventHandler
  def onQuit(e: PlayerQuitEvent): Unit = {

  }
}
