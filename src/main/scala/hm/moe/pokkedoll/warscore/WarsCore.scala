package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.commands.GameCommand
import hm.moe.pokkedoll.warscore.db.{Database, MemoryDatabase}
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, PlayerListener, SignListener}
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * WarsCoreのメインクラス
 * @author Emorard
 */
class WarsCore extends JavaPlugin {


  private var database: Database = _

  override def onEnable(): Unit = {
    WarsCore.instance = this

    database = new MemoryDatabase

    Bukkit.getPluginManager.registerEvents(new LoginListener(this), this)
    Bukkit.getPluginManager.registerEvents(new PlayerListener(this), this)
    Bukkit.getPluginManager.registerEvents(new SignListener(this), this)

    getCommand("game").setExecutor(new GameCommand)

    saveDefaultConfig()
    WarsCoreAPI.DEFAULT_SPAWN = Bukkit.getWorlds.get(0).getSpawnLocation
    WarsCoreAPI.reloadMapInfo(getConfig.getConfigurationSection("mapinfo"))
    WarsCoreAPI.reloadGame(null)
  }
}

object WarsCore {
  protected [warscore] var instance: WarsCore = _
}