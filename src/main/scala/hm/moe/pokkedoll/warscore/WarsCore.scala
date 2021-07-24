package hm.moe.pokkedoll.warscore

import com.google.common.io.ByteStreams
import com.shampaggon.crackshot.{CSDirector, CSUtility}
import hm.moe.pokkedoll.crackshot.WeaponLoader
import hm.moe.pokkedoll.db.PokkeDollDB
import hm.moe.pokkedoll.warscore.WarsCore.MODERN_TORUS_CHANNEL
import hm.moe.pokkedoll.warscore.commands._
import hm.moe.pokkedoll.warscore.db.{Database, SQLite}
import hm.moe.pokkedoll.warscore.events.InitializeWarsCoreEvent
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, MessageListener, PlayerListener, SignListener}
import hm.moe.pokkedoll.warscore.utils._
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

import scala.jdk.CollectionConverters._
/**
 * WarsCoreのメインクラス
 *
 * @author Emorard
 */
class WarsCore extends JavaPlugin {

  protected[warscore] var database: Database = _

  private val develop = true

  var cs: CSUtility = _

  var wl: WeaponLoader = _

  var db: PokkeDollDB = _

  override def onEnable(): Unit = {
    WarsCore.instance = this

    // BungeeCordから受信するのに必要
    getServer.getMessenger.registerIncomingPluginChannel(this, MODERN_TORUS_CHANNEL, new MessageListener(this))
    // BungeeCordに送信するのに必要
    getServer.getMessenger.registerOutgoingPluginChannel(this, MODERN_TORUS_CHANNEL)

    if (!develop) {
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
    getCommand("tag").setExecutor(new TagCommand)
    getCommand("spawn").setExecutor(new SpawnCommand)
    getCommand("wp").setExecutor(new WeaponCommand)
    getCommand("shop").setExecutor(new ShopCommand)
    getCommand("sndchecker").setExecutor(new SndCheckerCommand)
    getCommand("money").setExecutor(new MoneyCommand)
    getCommand("continue").setExecutor(new ContinueCommand)
    getCommand("cse").setExecutor(new CSECommand)

    saveDefaultConfig()

    ItemUtil.reloadItem()
    // MerchantUtil.reload()
    ShopUtil.reload()
    TagUtil.init()
    GameConfig.reload()

    WarsCoreAPI.DEFAULT_SPAWN = WarsCoreAPI.getLocation(getConfig.getString("spawns.default", "")).getOrElse(Bukkit.getWorlds.get(0).getSpawnLocation)
    WarsCoreAPI.FIRST_SPAWN = WarsCoreAPI.getLocation(getConfig.getString("spawns.first", "")).getOrElse(Bukkit.getWorlds.get(0).getSpawnLocation)
    // WarsCoreAPI.reloadMapInfo(getConfig.getConfigurationSection("mapinfo"))
    WarsCoreAPI.reloadGame(null)

    // setupCSPP()

    if(getConfig.isList("periodic_message")) {
      new PeriodicMessage(getConfig.getStringList("periodic_message").asScala.toList).runTaskTimerAsynchronously(this, 0L, 20 * 60 * 10L)
    }

    cs = new CSUtility

    if (Bukkit.getOnlinePlayers.size() != 0) {
      Bukkit.getOnlinePlayers.forEach(f => {
        WarsCoreAPI.getWPlayer(f)
        // WarsCoreAPI.addScoreBoard(f)
      })
    }

    if (!develop) {
      val out2 = ByteStreams.newDataOutput
      out2.writeUTF("ServerProgress")
      out2.writeByte(2)
      getServer.sendPluginMessage(this, MODERN_TORUS_CHANNEL, out2.toByteArray)
    }

    if(Bukkit.getPluginManager.isPluginEnabled("PokkeDollDB")) {
      db = Bukkit.getPluginManager.getPlugin("PokkeDollDB").asInstanceOf[PokkeDollDB]
      getCommand("vote").setExecutor(new VoteCommand(this))
    }

    new BukkitRunnable {
      override def run(): Unit = {
        Bukkit.getOnlinePlayers.forEach(_.setArrowsInBody(0))
      }
    }.runTaskTimer(this, 0L, 5L)

    Registry.init()

    if(Bukkit.getPluginManager.isPluginEnabled("CrackShot")) {
      val cd = Bukkit.getPluginManager.getPlugin("CrackShot").asInstanceOf[CSDirector]
      wl = new WeaponLoader(cd)
      Bukkit.dispatchCommand(Bukkit.getConsoleSender, "/shot config reload")
      wl.loadWeapons()
    }

    Bukkit.getPluginManager.callEvent(new InitializeWarsCoreEvent(this))
  }

  override def onDisable(): Unit = {
    lazy val board = Bukkit.getScoreboardManager.getMainScoreboard
    WarsCoreAPI.scoreboards.keys.foreach(_.setScoreboard(board))
    getServer.getMessenger.unregisterIncomingPluginChannel(this, MODERN_TORUS_CHANNEL)
    getServer.getMessenger.unregisterOutgoingPluginChannel(this, MODERN_TORUS_CHANNEL)
    database.close()
  }

  def getCSUtility: CSUtility = cs

  /**
   * onEnableメソッドより早く呼び出すとnullが返る
   * @return
   */
  def getDatabase: Database = database

  def info(string: String): Unit = {
    getLogger.info(string)
  }
}

object WarsCore {
  protected[warscore] var instance: WarsCore = _
  val MODERN_TORUS_CHANNEL = "torus:main"

  def log(string: String): Unit = instance.getLogger.info(string)

  /**
   * onEnableメソッドより早く呼び出すとnullが返る
   * @return
   */
  def getInstance: WarsCore = instance
}