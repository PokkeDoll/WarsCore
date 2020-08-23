package hm.moe.pokkedoll.warscore.utils

import java.util.Optional

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object TagUtil {
  private lazy val plugin = WarsCore.instance

  val cs = plugin.getConfig.getConfigurationSection("tags")

  val tags = mutable.HashMap.empty[String, String]

  def reload(): Unit = {
    tags.clear()
    cs.getKeys(false).forEach(f => {
      tags.put(f, cs.getString(f))
    })
  }

  reload()

  def setTag(player: Player, tagId: String): Unit = {
    // タグ情報をデータベース側に送るのはプレイヤーがログアウトしたときかセーブしたときのみ！
    val tag = tags.getOrElse(tagId, "-")
    val board = WarsCoreAPI.scoreboards(player)
    val obj = board.getObjective("tag")
    obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"$tag &f0"))
    WarsCoreAPI.scoreboards.values
      .map(_.getObjective("tag"))
      .foreach(_.getScore(player.getName).setScore(0))
  }

  /**
   * アンセーフコード。愚直なコード
   * @param item
   * @return
   */
  def getTagIdFromItemStack(item: ItemStack): String =
    Try(
    tags.filter(f => f._2 == ((opt => if(opt.isEmpty) "" else opt.get().replaceAll(ChatColor.WHITE + "タグ名: ", "")): Optional[String] => String)
      (item.getItemMeta.getLore.stream().filter(_.startsWith(ChatColor.WHITE + "タグ名: ")).findFirst())).keys.head
    ) match {
      case Success(v) => v
      case Failure(e) =>
        e.printStackTrace()
        ""
    }
}
