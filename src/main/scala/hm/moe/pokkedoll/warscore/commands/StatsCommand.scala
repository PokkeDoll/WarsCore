package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class StatsCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        val open = WarsCoreAPI.openStatsInventory(player, _)
        if(args.length == 0) {
          open(player)
        } else {
          val target = Bukkit.getPlayer(args(0))
          if(target == null) {
            player.sendMessage(ChatColor.RED + s"${target}のデータを取得できませんでした")
          } else {
            open(target)
          }
        }
    }
    true
  }
}
