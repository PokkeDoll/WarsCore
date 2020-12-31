package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.apache.commons.lang.StringUtils
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
 * ショップ
 *
 * @author Emorard
 * @since 1.7.10
 *
 */
object ShopUtil {
  private lazy val plugin = WarsCore.instance

  var configFile: File = _
  var config: FileConfiguration = _

  var shopCache = Map.empty[String, IndexedSeq[Shop]]

  def reload(): Unit = {
    createConfig() match {
      case Success(_) =>
        shopCache = Map.empty[String, IndexedSeq[Shop]]
      case Failure(exception) =>
        exception.printStackTrace()
        plugin.getLogger.warning("shop.ymlの読み込みに失敗しました")
    }
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "shop.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("shop.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  def saveConfig(): Unit = {
    config.save(configFile)
  }

  def hasName(name: String): Boolean = config.contains(name)

  /**
   * ショップの情報を取得する。キャッシュに保存されている場合はそれを使う
   *
   * @param name 名前
   * @return
   */
  def getShops(name: String): IndexedSeq[Shop] = {
    shopCache.getOrElse(name, {
      val shop = config.getStringList(name)
        .asScala
        .flatMap(text => {
          val item = text.split(",")
          if (item.length < 1) None else {
            val product = createShopItem(item.head)
            val price = item.tail.flatMap(createShopItem)
            if (product.isEmpty || price.isEmpty) None else {
              Some(new Shop(product.get, price))
            }
          }
        })
        .toIndexedSeq
      shopCache += name -> shop
      shop
    })
  }

  def setShop(key: String, stringList: java.util.List[String]): Unit = {
    config.set(key, stringList)
    saveConfig()
    reload()
  }

  def newShop(name: String): Unit = {
    config.set(name, java.util.Arrays.asList("air@1,air@1"))
    saveConfig()
    reload()
  }

  def delShop(name: String): Unit = {
    config.set(name, null)
    saveConfig()
    reload()
  }

  def createShopItem(string: String): Option[ShopItem] = {
    val split = string.split("@")
    if (split.length == 2)
      Some(new ShopItem(split(0), WarsCoreAPI.parseInt(split(1))))
    else
      None
  }

  /**
   * ショップそのもの
   *
   * @param product 製品
   * @param price   購入するために必要なアイテム
   */
  class Shop(val product: ShopItem, val price: Array[ShopItem])

  /**
   * データ化されたアイテムの名前と数
   *
   * @param name   名前
   * @param amount 数
   */
  class ShopItem(val name: String, val amount: Int)

}
