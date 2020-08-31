package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.inventory.MerchantRecipe

import scala.collection.JavaConverters._

object MerchantUtil {

  private lazy val plugin = WarsCore.instance

  def getMerchantRecipes(key: String): Option[java.util.List[MerchantRecipe]] = {
    val cs = plugin.getConfig.getConfigurationSection("merchants")
    if(cs.contains(key)) {
      val list = cs.getStringList(key).asScala
        .map(_.split(","))
        .filter(_.length == 3)
        .flatMap(arr => new MerchantData(arr(1), arr(2), arr(0)).build())
        .asJava
      if(list==null || list.size()==0) None else Some(list)
    } else None
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
