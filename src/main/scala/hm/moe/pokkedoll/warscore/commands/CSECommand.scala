package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class CSECommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        player.performCommand("/shot config reload")
        player.sendMessage("# 継承武器をリセット...")
        WarsCore.instance.wl.loadWeapons()
        player.sendMessage("# 継承武器を読み込みました")
      case _ =>
    }
    true
  }
}
