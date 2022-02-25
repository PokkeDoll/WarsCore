package hm.moe.pokkedoll.warscore.features

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.{Component, TextComponent}
import org.bukkit.block.Chest
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.{Location, Material}

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Random

/**
 * 一度しか開けられないチェストのクラス
 * @param parent 報酬として出現するアイテムが入っている親チェストの名前
 * @param isOpened 開けられたかを表すフラグ
 */
@Deprecated
class ChildChest(val parent: String, var isOpened: Boolean = false) {
  /**
   * 名前を返すのと同時にフラグを有効にする
   * @return parent
   */
  def thenOpen: String = {
    isOpened = true
    parent
  }
}

/**
 * ランダムでアイテムが入っているチェストのオブジェクト
 */
@Deprecated
object Chests {
  val parentChestMap = mutable.HashMap.empty[String, Location]
  val childChestMap = mutable.HashMap.empty[Location, ChildChest]

  val stub_chest_data = 0

  def reloadParent(): Unit = {
    parentChestMap.clear()
    // TODO スタブ
    val configuration = (null).asInstanceOf[ConfigurationSection]
    configuration.getKeys(false).asScala.map(
      key => configuration.getLocation("parent."+key)
    ).foreach(
      location => location.getBlock match {
        case chest: Chest =>
          parentChestMap.put(chest.getCustomName, location)
        case _ =>
      }
    )
  }

  def getLoot(childLocation: Location): Either[TextComponent, Seq[ItemStack]] = {
    childChestMap.get(childLocation).flatMap(cc => parentChestMap.get(cc.thenOpen)) match {
      case Some(parentLocation) =>
        parentLocation.getBlock match {
          case chest: Chest =>
            // 役目を終えたので子供チェストからは
            val contents = chest.getInventory.getContents.filterNot(i => i.getType == Material.AIR)
            Right(Random.shuffle(contents.toSeq).take(3))
          case _ =>
            Left(Component.text(s"親チェストがチェスト以外のブロックになってる！${parentLocation.toString}を確認").color(NamedTextColor.RED))
        }
      case None =>
        Left(Component.text("親チェストが見つからない...").color(NamedTextColor.RED))
    }
  }
}
