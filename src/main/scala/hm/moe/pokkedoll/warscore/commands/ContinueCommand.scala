package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCore
import net.md_5.bungee.api.ChatColor
import org.bukkit.Sound
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue

class ContinueCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        val continue = if(args.isEmpty) false else true
        player.setMetadata("wc-continue", new FixedMetadataValue(WarsCore.instance, continue))
        if(continue) {
          player.sendMessage("試合終了時の自動参加を" + ChatColor.BOLD + "" + ChatColor.GREEN + "有効" + ChatColor.RESET + "にしました")
          player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1f)
        } else {
          player.sendMessage("試合終了時の自動参加を" + ChatColor.BOLD + "" + ChatColor.RED + "無効" + ChatColor.RESET + "にしました")
          player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 1f)
        }
      case _ =>
    }
    true
  }
}
