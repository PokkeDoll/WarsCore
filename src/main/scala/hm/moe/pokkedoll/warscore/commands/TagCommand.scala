package hm.moe.pokkedoll.warscore.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{ClickEvent, ComponentBuilder}
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

        } else if (args.length >= 1 && player.hasPermission("pokkedoll.admin")) {
          if(args(1) == "list") {

          } else if (args(1) == "add" || args(1) == "set") {

          } else if (args(1) == "del" || args(1) == "remove") {

          } else {
            player.sendMessage(
              new ComponentBuilder("Tag Command Help\n")
                .append("list").color(ChatColor.GOLD)
                .append(": 登録されているタグの一覧を表示\n").color(ChatColor.WHITE)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tag list"))
                .append("add || set").color(ChatColor.GOLD).append("<id>").color(ChatColor.LIGHT_PURPLE).append("<displayName>").color(ChatColor.GREEN)
                .append(": タグを追加する\n").color(ChatColor.WHITE)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tag add "))
                .append("del || remove").color(ChatColor.GOLD).append("<id>").color(ChatColor.LIGHT_PURPLE)
                .append(": 登録されているタグを削除する\n").color(ChatColor.WHITE)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tag del "))
                .create():_*
            )
          }
        }
      case _ =>
    }
    true
  }
}
