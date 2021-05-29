package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{ClickEvent, ComponentBuilder}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, Sound}

/**
 * 他プレイヤーを招待するためのコマンド
 */
class InviteCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if (args.length == 0) {
          player.sendMessage("§c使用方法: /invite §a<プレイヤー名>")
        } else {
          val name = args(0)
          Option(Bukkit.getPlayer(name)) match {
            case Some(target) =>
              val wp = WarsCoreAPI.getWPlayer(player)
              val tp = WarsCoreAPI.getWPlayer(target)
              wp.game match {
                case Some(_) if tp.game.isDefined =>
                  player.sendMessage(ChatColor.GREEN + name + ChatColor.RED + s"は他のゲーム ${tp.game.get.title}(id: ${tp.game.get.id}に参加しています！")
                case Some(game) =>
                  target.sendMessage(
                    new ComponentBuilder("= = = = = = = = = = = = = = = = = = = = =\n")
                      .append(s"${player.getName}").color(ChatColor.GREEN).append("から招待が届きました！").color(ChatColor.WHITE).append("\n")
                      .append("ゲームモード: ").color(ChatColor.YELLOW).append(wp.game.get.title).color(ChatColor.GREEN).append("\n")
                      .append("参加するにはこのメッセージをクリック！！").bold(true).color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/game join ${game.id}"))
                      .append("\n= = = = = = = = = = = = = = = = = = = = =").color(ChatColor.WHITE)
                      .create(): _*
                  )
                  player.sendMessage(ChatColor.GREEN + target.getName + ChatColor.WHITE + "に招待を送りました")
                  target.playSound(target.getLocation, Sound.ENTITY_VILLAGER_YES, 1f, 1f)
                case None =>
                  player.sendMessage(ChatColor.RED + "ゲームに参加していないため招待できませんでした")
              }
            case None =>
              player.sendMessage(ChatColor.RED + s"$name は存在しません！")
          }
        }
      case _ =>
    }
    true
  }
}
