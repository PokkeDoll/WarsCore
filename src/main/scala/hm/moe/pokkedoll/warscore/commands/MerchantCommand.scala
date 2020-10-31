package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.MerchantUtil
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

import scala.util.{Failure, Success, Try}

class MerchantCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        val send = sendMessage(player, _)
        if (args.length == 0) {
          send(
            "&f/merchant <list|add|del|mod>\n" +
              "&f詳細は&bhttps://gitlab.com/PokkeDoll/pokkedoll-mc/-/wikis/wars-howto-mer"
          )
        } else if (args(0) == "list") {
          val comp = new ComponentBuilder("Show merchants list\n")
          MerchantUtil.config.getKeys(false).forEach(
            k => comp.append("* ").append(k).append("\n")
          )
          player.sendMessage(comp.create(): _*)
          // 取引グループを作成する
        } else if (args(0) == "add") {
          if (args.length > 1) {
            val title = args(1)
            if (!MerchantUtil.config.contains(title)) {
              MerchantUtil.newMerchant(title)
              player.sendMessage(ChatColor.BLUE + title + "を追加")
            } else {
              player.sendMessage(ChatColor.RED + title + "はすでに存在している.  delで削除")
            }
          } else {
            player.sendMessage(ChatColor.RED + "追加する取引タイトルを入力")
          }
          // 取引グループを削除
        } else if (args(0) == "del") {
          if (args.length > 1) {
            val title = args(1)
            if (MerchantUtil.config.contains(title)) {
              MerchantUtil.delMerchant(title)
              player.sendMessage(ChatColor.BLUE + title + "を削除")
            } else {
              player.sendMessage(ChatColor.RED + title + "は存在しない.  addで追加")
            }
          } else {
            player.sendMessage(ChatColor.RED + "削除する取引タイトルを入力")
          }
          // 取引内容を弄る
        } else if (args(0) == "mod") {
          if (args.length > 1) {
            val key = args(1)
            if (MerchantUtil.config.contains(key)) {
              if (args.length > 2) {
                val str = args(2)
                val content = MerchantUtil.config.getStringList(key)
                if (str.startsWith("+")) {
                  val text = str.substring(1)
                  ".*@[0-9]*,.*@[0-9]*,.*@[0-9]*".r.findFirstMatchIn(text) match {
                    case Some(_) =>
                      content.add(text)
                      MerchantUtil.setMerchant(key, content)
                      player.sendMessage(ChatColor.BLUE + "追加しました")
                    case None =>
                      player.sendMessage(ChatColor.RED + "構文が間違っています！")
                  }
                } else if (str.startsWith("-")) {
                  Try(str.substring(1).toInt) match {
                    case Success(value) =>
                      content.remove(value)
                      MerchantUtil.setMerchant(key, content)
                      player.sendMessage(ChatColor.BLUE + "削除しました")
                    case Failure(_) =>
                      player.sendMessage(ChatColor.RED + "数字のみ可能。インデックス番号は'i'で確認できる")
                  }
                } else if (str.startsWith("i")) {
                  val sb = new StringBuilder(s"MerchantRecipe for $key\n")
                  var i = 0
                  content.forEach(f => {
                    sb.append(s"$i. $f\n");
                    i += 1
                  })
                  player.sendMessage(sb.toString())
                }
              } else {
                player.sendMessage(ChatColor.RED + "err: 310")
              }
            } else {
              player.sendMessage(ChatColor.RED + args(1) + "は存在しない")
            }
          } else {
            player.sendMessage(ChatColor.RED + "取引タイトルを入力！")
          }
        } else if (args(0) == "reload") {
          MerchantUtil.reload()
          player.sendMessage("リロードしました")
        }
      case _ =>
    }
    true
  }

  private def sendMessage(player: Player, msg: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
  }

}
