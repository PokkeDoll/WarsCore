package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class GameConfig(cs: ConfigurationSection) {
  val maps: Seq[MapInfo] = cs.getConfigurationSection("maps")
    .getKeys(false)
    .asScala
    .map(mapId => {
      new MapInfo(
        mapId = mapId,
        mapName = cs.getString(s"maps.$mapId.mapName"),
        authors = cs.getString(s"maps.$mapId.authors"),
        locations = cs.getConfigurationSection(s"maps.$mapId.location")
          .getKeys(false)
          .asScala
          .flatMap(locationName => {
            WeakLocation.of(cs.getString(s"maps.$mapId.location.$locationName", "").split(",")) match {
              case Some(weakLocation) => Some((locationName, weakLocation))
              case None => None
            }})
          .toMap
      )
    })
    .toList

  val events = Map(
    "kill" -> (cs.getStringList("event.kill.item").asScala.flatMap(Item.of).toArray, cs.getInt("event.kill.exp")),
    "win" -> (cs.getStringList("event.win.item").asScala.flatMap(Item.of).toArray, cs.getInt("event.win.exp", 0)),
    "lose" -> (cs.getStringList("event.lose.item").asScala.flatMap(Item.of).toArray, cs.getInt("event.lose.exp", 0))
  )
}

object GameConfig {
  private lazy val plugin = WarsCore.instance

  private var configFile: File = _
  private var config: FileConfiguration = _

  def reload(): Unit = {
    createConfig() match {
      case Success(_) =>
        gameConfig = config.getKeys(false).asScala.map(f => (f, new GameConfig(config.getConfigurationSection(f)))).toMap
        plugin.getLogger.info("game.ymlの読み込みに成功しました")
      case Failure(exception) =>
        exception.printStackTrace()
        plugin.getLogger.warning("game.ymlの読み込みに失敗しました")
    }
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "game.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("game.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  def saveConfig(): Unit = {
    config.save(configFile)
  }

  private var gameConfig: Map[String, GameConfig] = _

  def getConfig(name: String): GameConfig = gameConfig.getOrElse(name, new GameConfig(config.getConfigurationSection(name)))
}
