package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.{Test, WarsCore}
import org.apache.commons.lang3.StringUtils
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.Player
import org.bukkit.inventory.{Merchant, MerchantRecipe}
import org.bukkit.{Bukkit, ChatColor}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
 * 村人と取引をするための拡張オブジェクト
 * 村人以外に対してもできるように
 * @author Emorard
 * @since 0.24.0
 * @version 2
 */
object MerchantUtil {

  private lazy val plugin = WarsCore.instance

  var config: FileConfiguration = _

  val merchantCache = mutable.HashMap.empty[String, Merchant]

  def reload(): Unit = {
    createConfig() match {
      case Success(_) =>
        merchantCache.clear()
      case Failure(exception) =>
        exception.printStackTrace()
        plugin.getLogger.warning("item.ymlの読み込みに失敗しました")
    }
  }

  def createConfig(): Try[Unit] = {
    val file = new File(plugin.getDataFolder, "merchant.yml")
    if (!file.exists()) {
      file.getParentFile.mkdirs()
      plugin.saveResource("merchant.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(file))
  }

  def saveConfig(): Unit = {
    config.save("merchant.yml")
  }

  def hasName(name: String): Boolean = config.contains(name)

  def getMerchantRecipes(key: String): Option[java.util.List[MerchantRecipe]] = {
    if(config.contains(key)) {
      val list = config.getStringList(key).asScala
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
      val i1 = ItemUtil.getItem(item1)
      val r = ItemUtil.getItem(result)
      if(i1.isDefined && r.isDefined) {
        val mr = new MerchantRecipe(r.get, Int.MaxValue)
        mr.setExperienceReward(false)
        mr.setUses(0)
        mr.addIngredient(i1.get)
        if(item2!=null) ItemUtil.getItem(item2) match {
          case Some(i2) =>
            mr.addIngredient(i2)
          case None =>
        }
        Some(mr)
      } else {
        None
      }
    }
  }
}
