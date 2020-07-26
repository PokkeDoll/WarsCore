package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.lisners.{LoginListener, PlayerListener, SignListener}
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * WarsCoreのメインクラス
 */
class WarsCore extends JavaPlugin {

  override def onEnable(): Unit = {
    WarsCore.instance = this

    Bukkit.getPluginManager.registerEvents(new LoginListener(this), this)
    Bukkit.getPluginManager.registerEvents(new PlayerListener(this), this)
    Bukkit.getPluginManager.registerEvents(new SignListener(this), this)
  }
}

object WarsCore {
  protected [warscore] var instance: WarsCore = _
}