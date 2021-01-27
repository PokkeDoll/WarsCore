package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.ui.SndCheckerUI
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class SndCheckerCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player => SndCheckerUI.openUI(player)
      case _ =>
    }
    true
  }
}
