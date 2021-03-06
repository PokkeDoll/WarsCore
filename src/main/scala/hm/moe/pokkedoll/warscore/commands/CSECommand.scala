package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class CSECommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        player.performCommand("/shot config reload")
        WarsCore.instance.wl.loadWeapons()
      case _ =>
    }
    true
  }
}
