package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.BankManager
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class BankCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        BankManager.openBankInventory(player)
      case _ =>
    }
    true
  }
}
