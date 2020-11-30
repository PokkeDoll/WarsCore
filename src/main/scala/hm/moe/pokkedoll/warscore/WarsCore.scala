package hm.moe.pokkedoll.warscore

import com.comphenix.protocol.events.{ListenerPriority, PacketAdapter, PacketEvent}
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.{PacketType, ProtocolLibrary, ProtocolManager}
import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.cspp.CrackShotPP
import hm.moe.pokkedoll.warscore.WarsCore.MODERN_TORUS_CHANNEL
import hm.moe.pokkedoll.warscore.commands._
import hm.moe.pokkedoll.warscore.db.{Database, SQLite}
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, MessageListener, PlayerListener, SignListener}
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, MerchantUtil, TagUtil, UpgradeUtil}
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

/**
 * WarsCoreのメインクラス
 * @author Emorard
 */
class WarsCore extends JavaPlugin {

  protected[warscore] var database: Database = _

  private val develop = true

  var cspp: Option[CrackShotPP] = None

  var protocolManager: ProtocolManager = _

  override def onEnable(): Unit = {
    WarsCore.instance = this

    // BungeeCordから受信するのに必要
    getServer.getMessenger.registerIncomingPluginChannel(this, MODERN_TORUS_CHANNEL, new MessageListener(this))
    // BungeeCordに送信するのに必要
    getServer.getMessenger.registerOutgoingPluginChannel(this, MODERN_TORUS_CHANNEL)

    if(!develop) {
      val out1 = ByteStreams.newDataOutput
      out1.writeUTF("ServerProgress")
      out1.writeByte(1)
      getServer.sendPluginMessage(this, MODERN_TORUS_CHANNEL, out1.toByteArray)
    }


    database = new SQLite(this)

    Bukkit.getPluginManager.registerEvents(new LoginListener(this), this)
    Bukkit.getPluginManager.registerEvents(new PlayerListener(this), this)
    Bukkit.getPluginManager.registerEvents(new SignListener(this), this)

    val gameCommand = new GameCommand
    getCommand("game").setExecutor(gameCommand)
    getCommand("game").setTabCompleter(gameCommand)
    getCommand("invite").setExecutor(new InviteCommand)
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

    ItemUtil.reloadItem()
    MerchantUtil.reload()
    UpgradeUtil.reloadConfig()
    TagUtil.reloadConfig()

    setupCSPP()

    protocolManager = ProtocolLibrary.getProtocolManager

    if(Bukkit.getOnlinePlayers.size()!=0) {
      Bukkit.getOnlinePlayers.forEach(f => {
        WarsCoreAPI.getWPlayer(f)
        WarsCoreAPI.addScoreBoard(f)
      })
    }

    if(!develop) {
      val out2 = ByteStreams.newDataOutput
      out2.writeUTF("ServerProgress")
      out2.writeByte(2)
      getServer.sendPluginMessage(this, MODERN_TORUS_CHANNEL, out2.toByteArray)
    }
  }

  override def onDisable(): Unit = {
    lazy val board = Bukkit.getScoreboardManager.getMainScoreboard
    WarsCoreAPI.scoreboards.keys.foreach(_.setScoreboard(board))
    getServer.getMessenger.unregisterIncomingPluginChannel(this, MODERN_TORUS_CHANNEL)
    getServer.getMessenger.unregisterOutgoingPluginChannel(this, MODERN_TORUS_CHANNEL)
    database.close()
  }

  def setupCSPP(): Unit = {
    cspp match {
      case Some(v) =>
        HandlerList.unregisterAll(v)
        getLogger.info("Reset CSPP handler!")
      case None =>
        getLogger.info("CSPP is not loaded!")
    }
    cspp = Some(new CrackShotPP(this, getConfig))
  }
}

object WarsCore {
  protected [warscore] var instance: WarsCore = _

  @Deprecated
  val LEGACY_TORUS_CHANNEL = "Torus"

  val MODERN_TORUS_CHANNEL = "torus:main"
}