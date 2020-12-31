package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.ItemUtil
import org.bukkit.{ChatColor, Material}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class ItemCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        player.sendMessage(ChatColor.GRAY + "Wars互換モード: false [since v0.26.1]")
        val v0 = if (args.length == 0) "" else args(0)
        if (v0.equalsIgnoreCase("list")) {
          val param = if(args.length > 1) args(1) else ""
          val s = new StringBuilder(s"アイテム一覧 (パラメーター: $param\n")
          if(param == "-w") {
            ItemUtil.cache
              .filter(pred => pred._2.getType == Material.IRON_HOE ||
                pred._2.getType == Material.DIAMOND_SWORD ||
                pred._2.getType == Material.STONE_HOE)
              .keys
              .foreach(key => s.append(s"$key\n"))
          } else {
            ItemUtil.config.getKeys(false).forEach(key => s.append(s"$key\n"))
          }
          sender.sendMessage(s.toString())
        } else if (args.length > 1 && v0.equalsIgnoreCase("set")) {
          val item = player.getInventory.getItemInMainHand
          if (item == null || item.getType == Material.AIR) {
            player.sendMessage("無効なアイテムです")
          } else {
            ItemUtil.setItem(args(1), item.clone())
            player.sendMessage(s"${args(1)}をセットしました")
          }
        } else if (args.length > 1 && v0.equalsIgnoreCase("remove")) {
          ItemUtil.removeItem(args(1))
          player.sendMessage(s"${args(1)}を削除しました")
        } else if (args.length > 1 && v0.equalsIgnoreCase("get")) {
          ItemUtil.getItem(args(1)) match {
            case Some(item) =>
              player.sendMessage(s"${args(1)}を入手しました")
              player.getInventory.addItem(item)
            case None =>
              player.sendMessage(s"${args(1)}は存在しません")
          }
        } else if (v0.equalsIgnoreCase("reload")) {
          ItemUtil.reloadItem()
          player.sendMessage("リロードしました")
        } else {
          sender.sendMessage(
            "構文: /item (list|set|remove|reload) (...)\n" +
              "/item list: 登録されているアイテムのリストを表示\n" +
              "/item set A: 手に持ってるアイテムをAとして登録\n" +
              "/item remove A: Aを削除\n" +
              "/item get A: Aを入手\n" +
              "/item reload: コンフィグをリロード, セーブ前のデータは消滅する"
          )
        }
    }
    true
  }
}
