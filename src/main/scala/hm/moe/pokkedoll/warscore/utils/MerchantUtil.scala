package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{Test, WarsCore}
import org.apache.commons.lang3.StringUtils
import org.bukkit.entity.Player
import org.bukkit.inventory.{Merchant, MerchantRecipe}
import org.bukkit.{Bukkit, ChatColor}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * 村人と取引をするための拡張オブジェクト
 * 村人以外に対してもできるように
 * @author Emorard
 * @since 0.24.0
 */
object MerchantUtil {

  private lazy val plugin = WarsCore.instance

  private val merchantCache = mutable.HashMap.empty[String, Merchant]

  def hasName(name: String): Boolean = plugin.getConfig.contains(s"merchants.$name")

  def getMerchantRecipes(key: String): Option[java.util.List[MerchantRecipe]] = {
    val cs = plugin.getConfig.getConfigurationSection("merchants")
    if(cs.contains(key)) {
      val list = cs.getStringList(key).asScala
        //.map(_.split(","))
        .map(StringUtils.split(_, ','))
        .filter(_.length == 3)
        .flatMap(arr => new MerchantData(arr(1), arr(2), arr(0)).build())
        .asJava
      if(list==null || list.size()==0) None else Some(list)
    } else None
    
  }

  def openMerchantInventory(player: Player, name: String): Unit = {
    val test = new Test("openMerchantInventory")
    merchantCache.get(name) match {
      case Some(mer) =>
        player.openMerchant(mer, true)
      case None =>
        getMerchantRecipes(name) match {
          case Some(recipes) =>
            val mer = Bukkit.createMerchant(name)
            mer.setRecipes(recipes)
            // キャッシュ
            merchantCache.put(name, mer)
            player.openMerchant(mer, true)
          case None =>
            player.sendMessage(ChatColor.RED + "取引が見つかりません！")
        }
    }
    test.log(1L)
  }

  class MerchantData(item1: String, item2: String, result: String) {
    def build(): Option[MerchantRecipe] = {
      if(ItemUtil.items.contains(item1) && ItemUtil.items.contains(result)) {
        val mr = new MerchantRecipe(ItemUtil.items(result), Int.MaxValue)
        mr.setExperienceReward(false)
        mr.setUses(0)
        mr.addIngredient(ItemUtil.items(item1))
        if(item2!=null && ItemUtil.items.contains(item2)) mr.addIngredient(ItemUtil.items(item2))
        Some(mr)
      } else {
        None
      }
    }
  }
}
