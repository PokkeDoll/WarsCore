package hm.moe.pokkedoll.warscore.commands

import java.util

import hm.moe.pokkedoll.warscore.utils.ItemUtil
import org.bukkit.Bukkit
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

  def give(itemKey: String, players: Array[String]): Unit = {
    ItemUtil.getItem(itemKey) match {
      case Some(item) =>
        // 全員対象
        if(players.length == 0 && (players(0).equalsIgnoreCase("all") || players(0) == "*")) {

        } else if (players.length > 2 && players(0).equalsIgnoreCase("rank")) {
          val param = players(1)
          val rank = players(2).toInt
          if(param == ">" || param == ">=" || param == "<" || param == "<=" || param == "=") {

          }
        }
      case None =>
    }
  }
}
