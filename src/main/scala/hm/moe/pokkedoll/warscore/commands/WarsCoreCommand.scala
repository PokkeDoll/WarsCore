package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.EconomyUtil
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class WarsCoreCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {
          player.sendMessage(
            ChatColor.translateAlternateColorCodes('&',
              "HELP\n" +
                "/whelp [economy|eco] [get] [ingot|coin] [amount]\n" +
                "* 自身にぽっけコインを与えます"
            )
          )
        } else {
          if (args(0) == "economy" || args(0) == "eco") {
            println("eco")
            if(args.length > 3 && args(1) == "get") {
              println("get")
              val eco = if(args(2) == "ingot") EconomyUtil.INGOT else EconomyUtil.COIN
              println("val eco")
              try {
                val amount = args(3).toInt
                println("val amount")
                EconomyUtil.give(player, eco, amount)
                println("EconomyUtil.give")
              } catch {
                case e: NumberFormatException =>
                  e.printStackTrace()
              }
            }
          }
        }
    }
    true
  }
}
