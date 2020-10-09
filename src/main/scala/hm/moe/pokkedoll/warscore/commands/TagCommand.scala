package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.TagUtil
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

/**
 * タグを設定するコマンド
 * @author Emorard
 * @since 0.21
 */
class TagCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {

        } else if (player.hasPermission("pokkedoll.admin")) {
          if(args(0) == "list") {
            val comp = new ComponentBuilder("タグ一覧\n").color(ChatColor.GREEN)
            TagUtil.cache.foreach(f => {
              comp.append(s"${f._1} = ${f._2}\n")
            })
            player.sendMessage(comp.append("//").color(ChatColor.GREEN).create():_*)
          } else if (args(0) == "reload") {
            TagUtil.reloadConfig()
            player.sendMessage("リロードしました")
          } else {
            player.sendMessage(
              new ComponentBuilder("Tag Command Help(0.30.4 >)\n").color(ChatColor.GREEN)
                .append("/tag list: リストを一覧表示\n")
                .append("/tag reload: configをリロード")
                .create():_*
            )
          }
        }
      case _ =>
    }
    true
  }
}
