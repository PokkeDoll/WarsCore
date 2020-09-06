package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.commands.{BankCommand, GameCommand, InviteCommand, ItemCommand, MerchantCommand, RsCommand, UpgradeCommand, WarsCoreCommand, WhelpCommand}
import hm.moe.pokkedoll.warscore.db.{Database, SQLite}
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, MessageListener, PlayerListener, SignListener}
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, UpgradeUtil}
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * WarsCoreのメインクラス
 * @author Emorard
 */
class WarsCore extends JavaPlugin {

  protected[warscore] var database: Database = _

  override def onEnable(): Unit = {
    WarsCore.instance = this

    // BungeeCordから受信するのに必要
    getServer.getMessenger.registerIncomingPluginChannel(this, "pokkedoll:torus", new MessageListener(this))
    // BungeeCordに送信するのに必要
    getServer.getMessenger.registerOutgoingPluginChannel(this, "pokkedoll:torus")

    database = new SQLite(this)

    Bukkit.getPluginManager.registerEvents(new LoginListener(this), this)
    Bukkit.getPluginManager.registerEvents(new PlayerListener(this), this)
    Bukkit.getPluginManager.registerEvents(new SignListener(this), this)


    val gameCommand = new GameCommand
    getCommand("game").setExecutor(gameCommand)
    getCommand("game").setTabCompleter(gameCommand)
    getCommand("invite").setExecutor(new InviteCommand)
    getCommand("resourcepack").setExecutor(new RsCommand)
    getCommand("whelp").setExecutor(new WhelpCommand)
    getCommand("warscore").setExecutor(new WarsCoreCommand)
    getCommand("item").setExecutor(new ItemCommand)
    getCommand("upgrade").setExecutor(new UpgradeCommand)
    getCommand("merchant").setExecutor(new MerchantCommand)
    getCommand("bank").setExecutor(new BankCommand)
    saveDefaultConfig()
    WarsCoreAPI.DEFAULT_SPAWN = Bukkit.getWorlds.get(0).getSpawnLocation
    WarsCoreAPI.reloadMapInfo(getConfig.getConfigurationSection("mapinfo"))
    WarsCoreAPI.reloadGame(null)
    WarsCoreAPI.reloadRs(getConfig.getConfigurationSection("resourcepacks"))

    ItemUtil.reloadItem()
    UpgradeUtil.reload()
    if(Bukkit.getOnlinePlayers.size()!=0) {
      Bukkit.getOnlinePlayers.forEach(f => {
        WarsCoreAPI.getWPlayer(f)
        WarsCoreAPI.addScoreBoard(f)
      })
    }
  }

  override def onDisable(): Unit = {
    lazy val board = Bukkit.getScoreboardManager.getMainScoreboard
    WarsCoreAPI.scoreboards.keys.foreach(_.setScoreboard(board))
    getServer.getMessenger.unregisterIncomingPluginChannel(this, "pokkedoll:torus")
    getServer.getMessenger.unregisterOutgoingPluginChannel(this, "pokkedoll:torus")
    database.close()
  }
}

object WarsCore {
  protected [warscore] var instance: WarsCore = _
}