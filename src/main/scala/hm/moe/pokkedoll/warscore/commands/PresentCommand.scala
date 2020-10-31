package hm.moe.pokkedoll.warscore.commands

import java.util

import org.bukkit.command.{Command, CommandExecutor, CommandSender, TabExecutor}
import org.bukkit.entity.Player

class PresentCommand extends CommandExecutor with TabExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.isEmpty) {
          player.sendMessage("")
        }
    }
    true
  }

  override def onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array[String]): util.List[String] = {
    if(command.getName.equalsIgnoreCase("present")) {

    }
    return null
  }
}
