package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.{Test, WarsCore}
import org.apache.commons.lang.StringUtils
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.Player
import org.bukkit.inventory.{ItemStack, Merchant, MerchantRecipe}
import org.bukkit.{Bukkit, ChatColor}

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
 * 村人と取引をするための拡張オブジェクト
 * 村人以外に対してもできるように
 *
 * @author Emorard
 * @since 0.24.0
 * @version 2
 */
@Deprecated
object MerchantUtil {

  private lazy val plugin = WarsCore.instance

  var configFile: File = _
  var config: FileConfiguration = _

  val merchantCache = mutable.HashMap.empty[String, Merchant]

  def reload(): Unit = {
    createConfig() match {
      case Success(_) =>
        merchantCache.clear()
      case Failure(exception) =>
        exception.printStackTrace()
        plugin.getLogger.warning("merchant.ymlの読み込みに失敗しました")
    }
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "merchant.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("merchant.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  def saveConfig(): Unit = {
    config.save(configFile)
  }

  def hasName(name: String): Boolean = config.contains(name)

  def getMerchantRecipes(key: String): Option[java.util.List[MerchantRecipe]] = {
    if (config.contains(key)) {
      val list = config.getStringList(key).asScala
        .map(StringUtils.split(_, ','))
        .filter(_.length == 3)
        .flatMap(arr => buildMerchantData(arr(1), arr(2), arr(0)))
        .asJava
      if (list == null || list.size() == 0) None else Some(list)
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
    test.log(2L)
  }

  private def buildMerchantData(_1: String, _2: String, _r: String): Option[MerchantRecipe] = {
    val i1 = getItem(_1)
    val i2 = getItem(_2)
    val r = getItem(_r)
    if (i1.isDefined && r.isDefined) {
      val mr = new MerchantRecipe(r.get, Int.MaxValue)
      mr.setExperienceReward(false)
      mr.setUses(0)
      mr.addIngredient(i1.get)
      i2.foreach(mr.addIngredient)
      Some(mr)
    } else {
      None
    }
  }

  private def getItem(str: String): Option[ItemStack] = {
    // 書式は<itemID>@<個数>,...という感じに書かれている
    val arr = str.split("@")
    ItemUtil.getItem(arr(0)) match {
      case Some(value) =>
        val item = value.clone()
        item.setAmount(if (arr.length == 1) 1 else Try(arr(1).toInt).getOrElse(1))
        Some(item)
      case _ =>
        None
    }
  }

  def setMerchant(key: String, stringList: java.util.List[String]): Unit = {
    config.set(key, stringList)
    println(config.contains(key))
    saveConfig()
    reload()
    println(config.contains(key))
  }

  def newMerchant(title: String): Unit = {
    config.set(title, java.util.Arrays.asList("air@1,air@1,air@1"))
    println(config.contains(title))
    saveConfig()
    reload()
    println(config.contains(title))
  }

  def delMerchant(title: String): Unit = {
    config.set(title, null)
    saveConfig()
    reload()
  }
}
