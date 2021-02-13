package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class SpawnCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player if player.getWorld == Bukkit.getWorlds.get(0) =>
        val wp = WarsCoreAPI.getWPlayer(player)
        if (wp.game.isDefined) {
          player.sendMessage(ChatColor.RED + "ゲームに参加している間は使用できません")
        } else {
          player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
          player.sendMessage(ChatColor.BLUE + "ロビーに移動します...")
        }
      case _ =>
        sender.sendMessage(ChatColor.RED + "ロビーでのみ使用できます！")
    }
    true
  }
}
