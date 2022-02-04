package hm.moe.pokkedoll.warscore.games

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import hm.moe.pokkedoll.warscore.utils.{GameConfig, MapInfo, WorldLoader}
import hm.moe.pokkedoll.warscore.wplayer.WPlayerState
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore, WarsCoreAPI}
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.{Component, TextComponent}
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{BaseComponent, ComponentBuilder}
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.{GameRule, World}

import scala.util.Try

/**
 * ゲームのコア部分のトレイト
 *
 * @author Emorard
 */
trait Game {

  val newGameSystem: Boolean

  var loaded = false

  var debug = false
  /**
   * ゲームの識別ID
   */
  val id: String

  /**
   * ゲームの構成
   */
  val config: GameConfig

  /**
   * 読み込むワールドのID.  最初は必ず0
   */
  var worldId: String

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

  import scala.jdk.CollectionConverters._

  /**
   * Javaにやさしいメンバー取得メソッド
   *
   * @return
   */
  @Deprecated
  def getMembersAsJava: java.util.List[WPlayer] = members.asJava

  /**
   * ワールド。でも使うかわからん...
   */
  var world: World

  var mapInfo: MapInfo

  /**
   * 試合時間
   */
  @Deprecated
  val time: Int

  /**
   * 最大試合時間
   */
  var maxTime: Int


  /**
   * 設定の独立性を保つために使用される変数。使用中ならtrue
   */
  protected [warscore] var isSetting: Boolean = false
  /**
   * ゲームを開始するためのワールドを読み込むメソッド。
   */
  def load(players: Vector[Player] = Vector.empty[Player], mapInfo: Option[MapInfo] = None): Unit = {
    state = GameState.LOADING_WORLD
    this.mapInfo = mapInfo.getOrElse(scala.util.Random.shuffle(config.maps).head)
    WorldLoader.asyncLoadWorld(world = this.mapInfo.mapId, worldId = worldId, new Callback[World] {
      override def success(value: World): Unit = {
        world = value
        world.setGameRule(GameRule.KEEP_INVENTORY, java.lang.Boolean.TRUE)
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, java.lang.Boolean.FALSE)
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, java.lang.Boolean.FALSE)
        loaded = true
        disable = false
        init()
        players.foreach(join)
      }

      override def failure(error: Exception): Unit = {
        state = GameState.ERROR
      }
    })
  }

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
   * プレイヤーがゲームに参加するときのメソッド
   *
   * @param wp プレイヤー
   * @return 参加できる場合
   */
  protected[Game] def join(wp: WPlayer): Unit

  /**
   * 次のバージョンで削除
   * @param wp
   * @return
   */
  @Deprecated
  protected def canJoinD(wp: WPlayer): Boolean = {
    if(wp.game.isDefined) {
      wp.sendMessage("他のゲームに参加しています！")
      false
    } else if(!loaded && state == GameState.DISABLE) {
      load(Vector(wp.player))
      false
    } else if (!state.join) {
      wp.sendMessage("ゲームに参加できません！")
      false
    } else if (members.length >= maxMember) {
      wp.sendMessage("人数が満員なので参加できません！")
      false
    } else {
      true
    }
  }

  def canJoin(wp: WPlayer): Option[Component] = None

  /**
   * Playerバージョン
   *
   * @param p プレイヤー
   */
  protected[Game] def join(p: Player): Unit = join(WarsCoreAPI.getWPlayer(p))

  /**
   * プレイヤーがゲームから抜けたときのメソッド
   *
   * @param wp プレイヤー
   */
  def hub(wp: WPlayer): Unit

  def hub(p: Player): Unit = hub(WarsCoreAPI.getWPlayer(p))

  /**
   * プレイヤーがダメージを受けた時のイベント
   *
   * @param e イベント
   */
  def onDamage(e: EntityDamageByEntityEvent): Unit = {}

  /**
   * プレイヤーが死亡したときのイベント
   *
   * @param e イベント
   */
  def onDeath(e: PlayerDeathEvent): Unit = {
    e.setKeepLevel(true)
    e.setKeepInventory(true)
  }

  /**
   * プレイヤーがリスポーン**する時**に呼ばれるイベント
   * @param e イベント
   */
  def onRespawn(e: PlayerRespawnEvent): Unit = {}

  /**
   * プレイヤーがリスポーン**した後**に呼ばれるイベント
   * @param e イベント
   */
  def onPostRespawn(e: PlayerPostRespawnEvent): Unit = {}

  /**
   * ブロックを破壊するときに呼び出されるイベント
   *
   * @param e イベント
   */
  def onBreak(e: BlockBreakEvent): Unit = {
    e.setCancelled(true)
  }

  /**
   * ブロックを設置するときに呼び出されるイベント
   *
   * @param e イベント
   */
  def onPlace(e: BlockPlaceEvent): Unit = {
    e.setCancelled(true)
  }

  /**
   * 報酬を与えるメソッド
   *
   * @param p          報酬を与えるプレイヤー
   * @param rewardType 報酬タイプ
   */
  def reward(p: Player, rewardType: GameRewardType): Unit = {
    (rewardType match {
      case GameRewardType.KILL => config.events.get("kill")
      case GameRewardType.LOSE => config.events.get("lose")
      case GameRewardType.WIN => config.events.get("win")
      case GameRewardType.ASSIST => config.events.get("assist")
    }) match {
      case Some(v) =>
        WarsCore.instance.database.addItem(p.getUniqueId.toString, v._1)
      case None =>
    }
  }

  /**
   * ゲームに参加しているプレイヤー全員にメッセージを送信する
   *
   * @param string メッセージ
   */
  @Deprecated
  def sendMessage(string: String): Unit = {
    world.getPlayers.forEach(_.sendMessage(ChatColor.translateAlternateColorCodes('&', string)))
  }

  def sendMessage(text: TextComponent): Unit = {
    world.getPlayers.forEach(_.sendMessage(text))
  }

  @Deprecated
  def sendMessage(components: Array[BaseComponent]): Unit = {
    world.getPlayers.forEach(_.sendMessage(components: _*))
  }

  @Deprecated
  def sendActionBar(string: String): Unit = {
    world.getPlayers.forEach(_.sendActionBar(ChatColor.translateAlternateColorCodes('&', string)))
  }

  def sendActionBar(text: TextComponent): Unit = {
    world.getPlayers.forEach(_.sendActionBar(text))
  }

  def log(reason: String, message: String): Try[Unit] = {
    WarsCore.instance.database.gameLog(id, reason, message)
  }

  @Deprecated
  def createResult(data: GamePlayerData, winner: GameTeam): Array[BaseComponent] = {
    val comp = new ComponentBuilder("= - = - = - = - = -").color(ChatColor.GRAY).underlined(true)
      .append(" 戦績 ").underlined(false).bold(true).color(ChatColor.AQUA)
      .append("- = - = - = - = - = \n\n").underlined(true).bold(false).color(ChatColor.GRAY)
      .append("* ").underlined(false).color(ChatColor.WHITE)
      .append("結果: ").color(ChatColor.GRAY)

    if (winner == GameTeam.DEFAULT) {
      comp.append("引き分け\n")
    } else if (winner == data.team) {
      comp.append("勝利").color(ChatColor.YELLOW).bold(true).append("\n").bold(false)
    } else {
      comp.append("敗北").color(ChatColor.BLUE).append("\n")
    }

    comp.append("* ").reset()
      .append("キル数: ").color(ChatColor.GRAY)
      .append(data.kill.toString).color(ChatColor.GREEN).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)

    comp.append("* ")
      .append("デス数: ").color(ChatColor.GRAY)
      .append(data.death.toString).color(ChatColor.GREEN).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)

    val kd = if (data.death == 0) data.kill.toDouble else BigDecimal.valueOf(data.kill / data.death.toDouble).setScale(-2, BigDecimal.RoundingMode.FLOOR).doubleValue

    comp.append("* ")
      .append("K/D: ").color(ChatColor.GRAY)
      .append(kd.toString).color(ChatColor.GREEN).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)

    comp.append("* ")
      .append("アシスト: ").color(ChatColor.GRAY)
      .append(data.assist.toString).color(ChatColor.GREEN).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)

    comp.append("* ")
      .append("与えたダメージ: ").color(ChatColor.GRAY)
      .append(s"❤ × ${data.damage.toInt / 2}").color(ChatColor.RED).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)

    comp.append("* ")
      .append("受けたダメージ: ").color(ChatColor.GRAY)
      .append(s"❤ × ${data.damaged.toInt / 2}").color(ChatColor.RED).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)

    comp.create()
  }
}

object Game {
  def join(wp: WPlayer, game: Game): Unit = {
    // TODO newGameSystemを解決する
    if(game.newGameSystem) {
      canJoin(wp, game) match {
        case Some(error) =>
          wp.sendMessage(error)
        case None =>
          game.join(wp)
          println(s"Game.join: ${wp.game.isDefined}")
      }
    } else {
      wp.sendMessage(Component.text("v2.3より: 新システムに対応していないゲームです.  参加処理は拒否されました"))
    }
  }

  /**
   * プレイヤーが参加できるかを判定する
   * @param wp
   * @param game
   * @return Someならエラーメッセージあり！Noneが正解！
   */
  def canJoin(wp: WPlayer, game: Game): Option[Component] = {
    wp.state match {
      case WPlayerState.ONLINE =>
        game.canJoin(wp)
      case WPlayerState.PLAYING =>
        Some(Component.text("試合中だよ！").color(NamedTextColor.RED))
      case WPlayerState.ENTRY =>
        Some(Component.text("既にゲームにエントリーしています").color(NamedTextColor.RED))
      case _ =>
        Some(Component.text("参加が拒否されました！").color(NamedTextColor.RED))
    }
  }
}
