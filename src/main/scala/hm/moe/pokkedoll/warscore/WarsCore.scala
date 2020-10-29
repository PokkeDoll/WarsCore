package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.cspp.CrackShotPP
import hm.moe.pokkedoll.warscore.WarsCore.LEGACY_TORUS_CHANNEL
import hm.moe.pokkedoll.warscore.commands.{GameCommand, InviteCommand, ItemCommand, MerchantCommand, RsCommand, SpawnCommand, TagCommand, UpgradeCommand, WarsCoreCommand}
import hm.moe.pokkedoll.warscore.db.{Database, SQLite}
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, MessageListener, PlayerListener, SignListener}
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, MerchantUtil, TagUtil, UpgradeUtil}
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
    getServer.getMessenger.registerIncomingPluginChannel(this, LEGACY_TORUS_CHANNEL, new MessageListener(this))
    // BungeeCordに送信するのに必要
    getServer.getMessenger.registerOutgoingPluginChannel(this, LEGACY_TORUS_CHANNEL)

    database = new SQLite(this)

    Bukkit.getPluginManager.registerEvents(new LoginListener(this), this)
    Bukkit.getPluginManager.registerEvents(new PlayerListener(this), this)
    Bukkit.getPluginManager.registerEvents(new SignListener(this), this)

    Bukkit.getPluginManager.registerEvents(new CrackShotPP(this, getConfig), this)

    val gameCommand = new GameCommand
    getCommand("game").setExecutor(gameCommand)
    getCommand("game").setTabCompleter(gameCommand)
    getCommand("invite").setExecutor(new InviteCommand)
    getCommand("resourcepack").setExecutor(new RsCommand)
    getCommand("warscore").setExecutor(new WarsCoreCommand)
    getCommand("item").setExecutor(new ItemCommand)
    getCommand("upgrade").setExecutor(new UpgradeCommand)
    getCommand("merchant").setExecutor(new MerchantCommand)
    getCommand("tag").setExecutor(new TagCommand)
    getCommand("spawn").setExecutor(new SpawnCommand)
    saveDefaultConfig()
    WarsCoreAPI.DEFAULT_SPAWN = WarsCoreAPI.getLocation(getConfig.getString("spawns.default", "")).getOrElse(Bukkit.getWorlds.get(0).getSpawnLocation)
    WarsCoreAPI.FIRST_SPAWN = WarsCoreAPI.getLocation(getConfig.getString("spawns.first", "")).getOrElse(Bukkit.getWorlds.get(0).getSpawnLocation)
    WarsCoreAPI.reloadMapInfo(getConfig.getConfigurationSection("mapinfo"))
    WarsCoreAPI.reloadGame(null)
    WarsCoreAPI.reloadRs(getConfig.getConfigurationSection("resourcepacks"))

    ItemUtil.reloadItem()
    MerchantUtil.reload()
    UpgradeUtil.reloadConfig()
    TagUtil.reloadConfig()

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
    getServer.getMessenger.unregisterIncomingPluginChannel(this, LEGACY_TORUS_CHANNEL)
    getServer.getMessenger.unregisterOutgoingPluginChannel(this, LEGACY_TORUS_CHANNEL)
    database.close()
  }
}

object WarsCore {
  protected [warscore] var instance: WarsCore = _

  val LEGACY_TORUS_CHANNEL = "Torus"
}