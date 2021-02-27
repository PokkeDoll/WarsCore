package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class MoneyCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        val amount = WarsCore.instance.database.getAmount(player.getUniqueId.toString, "coin")
        WarsCoreAPI.info(player, s"現在ぽっけコインを " + ChatColor.YELLOW + s"${amount}個" + ChatColor.BLUE + " 所持しています！")
      case _ =>
    }
    true
  }
}
