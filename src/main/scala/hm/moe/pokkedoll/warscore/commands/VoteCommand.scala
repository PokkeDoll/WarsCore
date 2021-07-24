package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.db.PokkeDollDB
import hm.moe.pokkedoll.warscore.WarsCore
import hm.moe.pokkedoll.warscore.ui.WeaponUI
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class VoteCommand(val plugin: WarsCore) extends CommandExecutor {

  val db: PokkeDollDB = plugin.db

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.nonEmpty && args(0).equalsIgnoreCase("get")) {
          val result = db.getVoteItem(player.getName)
          if(result.equals("")) {
            plugin.database.addWeapon(player.getUniqueId.toString, "item", "vote", 1)
            WeaponUI.weaponCache.remove(player)
            player.sendMessage(ChatColor.BLUE + "ポールクリスタルを獲得しました")
          } else {
            player.sendMessage(ChatColor.RED + result)
          }
        } else {
          val vp = db.getVP(player.getUniqueId)
          player.sendMessage(
            ChatColor.BLUE + "現在所持しているVP: " + ChatColor.GREEN + vp + " VP\n" +
            "/vote get"+ChatColor.BLUE+"でポールクリスタルに変換できます.")
        }
      case _ =>
    }
    true
  }
}
