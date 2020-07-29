package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.{Game, TeamDeathMatch}
import hm.moe.pokkedoll.warscore.utils.MapInfo
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, Location}
import org.bukkit.scoreboard.{Scoreboard, Team}

import scala.collection.mutable
import scala.util.Random

/**
 * 便利なメソッドをまとめたオブジェクト
 *
 * @author Emorard
 */
object WarsCoreAPI {
  // 内部バージョン. 特に意味はない
  val VERSION = 1

  lazy val scoreboard: Scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

  lazy val random = new Random()

  protected[warscore] var DEFAULT_SPAWN: Location = _

  /** ゲーム情報 */
  val games = mutable.HashMap.empty[String, Game]

  /** プレイヤーのキャッシュ */
  val wplayers = mutable.HashMap.empty[Player, WPlayer]

  /** ワールドの設定 */
  @Deprecated
  var worldSettingConfig: ConfigurationSection = _

  /** マップ情報 */
  var mapinfo = Seq.empty[MapInfo]

  /**
   * チームの設定をまとめたもの
   * @param team
   */
  def setBaseTeam(team: Team): Unit = {
    team.setAllowFriendlyFire(false)
    team.setCanSeeFriendlyInvisibles(true)
    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS)
  }

  /**
   * テストコード
   * @param player
   * @return
   */
  def getWPlayer(player: Player): WPlayer = wplayers.getOrElseUpdate(player, new WPlayer(player))

  /**
   * プレイヤーの動きを止める。視点は動かせる
   * @param player
   */
  def freeze(player: Player): Unit = {
    player.teleport(player.getLocation().add(0, 0.001, 0))
    player.setAllowFlight(true)
    player.setFlying(true)
    player.setFlySpeed(0.001f)
    player.setWalkSpeed(0.001f)
  }

  /**
   * プレイヤーを動けるようにする。freezeと共に使用する
   * @see freeze(Player)
   */
  def unfreeze(player: Player): Unit = {
    player.setAllowFlight(false)
    player.setFlying(false)
    player.setFlySpeed(0.1f)
    player.setWalkSpeed(0.2f)
  }

  /**
   * 説明しよう！(図で)<br>
   * mapinfo:             <- ↑ cs ↑<br>
   *     tdm:             <- gameType<br>
   *         mapA:        <- id<br>
   *             author:<br>
   *             spawn:<br>
   *             ...<br>
   * @param cs
   */
  def reloadMapInfo(cs: ConfigurationSection): Unit = {
    cs.getKeys(false).forEach(gameType => {
      mapinfo = Seq.empty[MapInfo]

      cs.getConfigurationSection(gameType).getKeys(false).forEach(id => {
        val i = new MapInfo(gameType, id)
        i.mapName = cs.getString(s"$gameType.$id.mapName")
        i.authors = cs.getString(s"$gameType.$id.authors")
        cs.getConfigurationSection(s"$gameType.$id.location").getKeys(false).forEach(location => {
          val str = cs.getString(s"$gameType.$id.location.$location")
          val data = str.split(",")
          try {
            i.locations.put(location, (data(0).toDouble, data(1).toDouble, data(2).toDouble, data(3).toFloat, data(4).toFloat))
          } catch {
            case e: ArrayIndexOutOfBoundsException =>
              e.printStackTrace()
            case e: NumberFormatException =>
              e.printStackTrace()
          }
        })
        mapinfo = mapinfo :+ i
      })
    })
  }

  /**
   * ゲームの情報を読み込む
   * @param cs
   */
  def reloadGame(cs: ConfigurationSection): Unit = {
    games.clear()

    games.put("tdm-test-1", new TeamDeathMatch("tdm-test-1"))
    games.put("tdm-test-2", new TeamDeathMatch("tdm-test-2"))
  }
}
