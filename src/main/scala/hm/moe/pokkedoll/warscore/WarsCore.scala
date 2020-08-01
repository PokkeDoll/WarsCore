package hm.moe.pokkedoll.warscore

import com.comphenix.protocol.PacketType.Protocol
import com.comphenix.protocol.{PacketType, ProtocolLibrary}
import com.comphenix.protocol.events.{ListenerPriority, PacketAdapter, PacketEvent}
import hm.moe.pokkedoll.warscore.commands.{GameCommand, InviteCommand, RsCommand}
import hm.moe.pokkedoll.warscore.db.{Database, MemoryDatabase}
import hm.moe.pokkedoll.warscore.lisners.{LoginListener, MessageListener, PlayerListener, SignListener}
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

    // BungeeCordから受信するのに必要
    getServer.getMessenger.registerIncomingPluginChannel(this, "pokkedoll:torus", new MessageListener(this))
    // BungeeCordに送信するのに必要
    getServer.getMessenger.registerOutgoingPluginChannel(this, "pokkedoll:torus")

    database = new MemoryDatabase

    Bukkit.getPluginManager.registerEvents(new LoginListener(this), this)
    Bukkit.getPluginManager.registerEvents(new PlayerListener(this), this)
    Bukkit.getPluginManager.registerEvents(new SignListener(this), this)


    val gameCommand = new GameCommand
    getCommand("game").setExecutor(gameCommand)
    getCommand("game").setTabCompleter(gameCommand)
    getCommand("invite").setExecutor(new InviteCommand)
    getCommand("resourcepack").setExecutor(new RsCommand)

    saveDefaultConfig()
    WarsCoreAPI.DEFAULT_SPAWN = Bukkit.getWorlds.get(0).getSpawnLocation
    WarsCoreAPI.reloadMapInfo(getConfig.getConfigurationSection("mapinfo"))
    WarsCoreAPI.reloadGame(null)
    WarsCoreAPI.reloadRs(getConfig.getConfigurationSection("resourcepacks"))
  }
}

object WarsCore {
  protected [warscore] var instance: WarsCore = _
}