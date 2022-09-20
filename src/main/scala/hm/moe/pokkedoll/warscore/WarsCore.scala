package hm.moe.pokkedoll.warscore

import com.google.common.io.ByteStreams
import com.shampaggon.crackshot.{CSDirector, CSUtility}
import hm.moe.pokkedoll.db.PokkeDollDB
import hm.moe.pokkedoll.warscore.WarsCore.MODERN_TORUS_CHANNEL
import hm.moe.pokkedoll.warscore.commands.ItemCommand
import hm.moe.pokkedoll.warscore.db.{Database, SQLite}
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, MessageListener}
import hm.moe.pokkedoll.warscore.utils._
import org.bukkit.Bukkit
import org.bukkit.plugin.PluginManager
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

    getCommand("item").setExecutor(new ItemCommand)

    saveDefaultConfig()

    ItemUtil.reloadItem()

    if(getConfig.isList("periodic_message")) {
      new PeriodicMessage(getConfig.getStringList("periodic_message").asScala.toList).runTaskTimerAsynchronously(this, 0L, 20 * 60 * 10L)
    }

    cs = new CSUtility

    if (!develop) {
      val out2 = ByteStreams.newDataOutput
      out2.writeUTF("ServerProgress")
      out2.writeByte(2)
      getServer.sendPluginMessage(this, MODERN_TORUS_CHANNEL, out2.toByteArray)
    }

    if(Bukkit.getPluginManager.isPluginEnabled("PokkeDollDB")) {
      db = Bukkit.getPluginManager.getPlugin("PokkeDollDB").asInstanceOf[PokkeDollDB]
      // getCommand("vote").setExecutor(new VoteCommand(this))
    }

    new BukkitRunnable {
      override def run(): Unit = {
        Bukkit.getOnlinePlayers.forEach(_.setArrowsInBody(0))
      }
    }.runTaskTimer(this, 0L, 5L)

    Registry.init()
  }

  override def onDisable(): Unit = {
    lazy val board = Bukkit.getScoreboardManager.getMainScoreboard
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