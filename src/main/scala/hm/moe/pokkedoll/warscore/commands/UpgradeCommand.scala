package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.UpgradeUtil
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class UpgradeCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    if(args.length==0) return false
    sender match {
      case player: Player =>
        player.sendMessage(ChatColor.GRAY + "Wars互換モード: true")
        if(args(0).equalsIgnoreCase("list")) {
          val sb = new StringBuilder("list > \n")
          UpgradeUtil.routes.foreach(f => {
            sb.append(s"${f._1} => ${f._2.name}\n* list =>\n")
            f._2.list.foreach(ff => {
              sb.append(s"** ${ff._1} => ID: ${ff._2._1}, FixedChance: ${ff._2._2}\n")
            })
          })
          player.sendMessage(sb.toString())
        } else if (args.length > 1 && args(0).equalsIgnoreCase("create")) {
          val id = args(1)
          if(UpgradeUtil.routes.contains(id)) {
            player.sendMessage(s"$id はすでに存在します")
          } else {
            UpgradeUtil.createUpgradeItem(id)
            player.sendMessage(s"$id を追加しました")
          }
        } else if (args.length > 1 && args(0).equalsIgnoreCase("delete")) {
          val id = args(1)
          if(UpgradeUtil.routes.contains(id)) {
            player.sendMessage(s"$id を削除しました")
            UpgradeUtil.removeUpgradeItem(id)
          } else {
            player.sendMessage(s"$id は存在しません")
          }
        } else if (args.length > 2 && args(0).equalsIgnoreCase("mod")) {
          val id = args(1); val a = args(2)
          UpgradeUtil.routes.get(id) match {
            case Some(item) =>
              if(a.equalsIgnoreCase("list")) {
                val sb = new StringBuilder(s"mod > list > ${item.name}'s routes'\n")
                item.list.foreach(f => {
                  sb.append(s"${f._1} => to: ${f._1}, fixedChance: ${f._2}")
                })
                player.sendMessage(sb.toString())
              } else if(args.length > 5 && a.equalsIgnoreCase("set")) {
                // 糞みたいなコーディング
                // 強化素材と強化先と補正確率
                try {
                  val from = args(3); val to = args(4); val fixedChance = args(5).toDouble
                  item.list.put(from, (to, fixedChance))
                } catch {
                  case e: Exception =>
                    player.sendMessage("エラーが発生しました")
                    return true
                }
                UpgradeUtil.updateUpgradeItem(item)
                player.sendMessage(s"${id}をアップデートしました")
              } else if(args.length > 3 && a.equalsIgnoreCase("remove")) {
                val from = args(3)
                item.list.remove(from)
                UpgradeUtil.updateUpgradeItem(item)
                player.sendMessage(s"${id}から${from}を削除しました")
              }
            case None =>
              player.sendMessage(s"${id}は存在しません")
          }
        } else if (args(0).equalsIgnoreCase("help")) {
          player.sendMessage(
            "注意: 構造を根本的に変え, saveやreloadは存在しない\n" +
              "/UpgradeUtil help: ヘルプを表示\n" +
              "/UpgradeUtil list: 登録されている強化リストを表示\n" +
              "/UpgradeUtil create <id>: 強化リストを登録する\n" +
              "/UpgradeUtil delete <id>: 強化リストを削除する\n" +
              "/UpgradeUtil mod <id> <...>: 強化リストの編集\n" +
              "- list: 強化リストの詳細を見る.  /UpgradeUtil listと同じ\n" +
              "- set <強化素材ID> <強化先ID> <補正確率>: \n" +
              "-    強化素材IDと強化先IDは/item listから参照する\n" +
              "-    強化素材IDを'else'にすると全強化素材に対応するルート強化になる\n" +
              "-    補正確率は強化素材が持つ成功確率から減算する数値, 強化素材の成功確率が80.0%で補正確率が50.0ならこの強化の成功確率は30.0%\n" +
              "- remove <強化素材ID>: 削除する"
          )
        }
      case _ =>
    }
    true
  }

}
