package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.utils.MapInfo
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import org.bukkit.World
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

import scala.collection.mutable

/**
 * ゲームのコア部分のトレイト
 * @author Emorard
 */
trait Game {

  var loaded = false
  /**
   * ゲームの識別ID
   */
  val id: String

  /**
   * ゲームのタイトル
   */
  val title: String

  /**
   * ボスバー
   */
  val bossbar: BossBar

  /**
   * ゲームの説明分
   */
  val description: String

  /**
   * ゲームが無効化されているか
   */
  var disable = true

  /**
   * ゲームの状態
   */
  var state: GameState = GameState.DISABLE

  /**
   * 受け入れる最大人数
   */
  val maxMember: Int

  /**
   * 参加するプレイヤー共
   */
  var members: Vector[WPlayer] = Vector.empty[WPlayer]

  /**
   * ワールド。でも使うかわからん...
   */
  var world: World

  var mapInfo: MapInfo

  /**
   * 試合時間
   */
  val time: Int

  def load(): Unit

  def init(): Unit

  def ready(): Unit

  def play(): Unit

  def end(): Unit

  def delete(): Unit

  def join(wp: WPlayer): Boolean

  /**
   * Playerバージョン
   * @param p
   */
  def join(p: Player): Boolean = join(WarsCoreAPI.getWPlayer(p))

  def hub(wp: WPlayer): Unit

  def hub(p: Player): Unit = hub(WarsCoreAPI.getWPlayer(p))

  def death(e: PlayerDeathEvent): Unit

  def sendMessage(string: String): Unit = {
    world.getPlayers.forEach(_.sendMessage(string))
  }
}
