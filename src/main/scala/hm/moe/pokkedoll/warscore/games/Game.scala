package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.utils.MapInfo
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import org.bukkit.{ChatColor, World}
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
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

  /**
   * ゲームを読み込む
   */
  def load(): Unit

  /**
   * ゲームを初期化する
   */
  def init(): Unit

  /**
   * ゲームのカウントダウンを開始する
   */
  def ready(): Unit

  /**
   * ゲームを開始する
   */
  def play(): Unit

  /**
   * ゲームを終了する
   */
  def end(): Unit

  /**
   * ゲームを削除する
   */
  def delete(): Unit

  /**
   * プレイヤーがゲームに参加するときのメソッド
   * @param wp プレイヤー
   * @return 参加できる場合
   */
  def join(wp: WPlayer): Boolean

  /**
   * Playerバージョン
   * @param p
   */
  def join(p: Player): Boolean = join(WarsCoreAPI.getWPlayer(p))

  /**
   * プレイヤーがゲームから抜けたときのメソッド
   * @param wp プレイヤー
   */
  def hub(wp: WPlayer): Unit

  def hub(p: Player): Unit = hub(WarsCoreAPI.getWPlayer(p))

  /**
   * プレイヤーが死亡したときのイベント
   * @param e イベント
   */
  def death(e: PlayerDeathEvent): Unit


  /**
   * プレイヤーがダメージを受けた時のイベント
   * @param e イベント
   */
  def damage(e: EntityDamageByEntityEvent): Unit


  /**
   * ブロックを破壊するときに呼び出されるイベント
   * @param e イベント
   */
  def break(e: BlockBreakEvent): Unit

  /**
   * ブロックを設置するときに呼び出されるイベント
   * @param e イベント
   */
  def place(e: BlockPlaceEvent): Unit

  /**
   * ゲームに参加しているプレイヤー全員にメッセージを送信する
   * @param string
   */
  def sendMessage(string: String): Unit = {
    world.getPlayers.forEach(_.sendMessage(ChatColor.translateAlternateColorCodes('&', string)))
  }
}
