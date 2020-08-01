package hm.moe.pokkedoll.warscore.commands

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class RsCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        player.sendMessage("§9クライアントのバージョンを取得しています...")
        new BukkitRunnable {
          override def run(): Unit = {
            val out = ByteStreams.newDataOutput
            out.writeUTF("PlayerVersion")
            player.sendPluginMessage(WarsCore.instance, "pokkedoll:torus", out.toByteArray)
          }
        }.runTaskLater(WarsCore.instance, 20L)
    }
    true
  }
}
