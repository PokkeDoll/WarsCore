package hm.moe.pokkedoll.warsgame

import hm.moe.pokkedoll.warscore.events.{GameEndEvent, GameJoinEvent}
import hm.moe.pokkedoll.warscore.games.{Game, GamePlayerData, GameState}
import hm.moe.pokkedoll.warscore.utils.{GameConfig, MapInfo, WeakLocation, WorldLoader}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Scoreboard}
import org.bukkit._

class PPEX(override val id: String) extends Game {

  override val newGameSystem: Boolean = true
  /**
   * ゲームの構成
   */
  override val config: GameConfig = GameConfig.getConfig("hcg")
  /**
   * 読み込むワールドのID.  最初は必ず0
   */
  override var worldId: String = id
  /**
   * ゲームのタイトル
   */
  override val title: String = "PPEX"
  /**
   * ボスバー
   */
  override val bossbar: BossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID)
  /**
   * ゲームの説明分
   */
  override val description: String = "Deprecated"
  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = Int.MaxValue
  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _
  override var mapInfo: MapInfo = _
  /**
   * 試合時間
   */
  override val time: Int = 600
  /**
   * 最大試合時間
   */
  override var maxTime: Int = _

  private val matchMakingText: Int => String = i => s"マッチメイキング中... ${i}/$maxMember"

  var phase = 0
  // フェーズ1, フェーズ2, フェーズ3, 最終リング
  val phaseTime: Array[Int] = Array(300, 240, 180, 60)
  // フェーズ1, フェーズ2, フェーズ3, 最終リング
  val closePhaseBorderTime: Array[Int] = Array(240, 180, 120, 120)

  val phaseBorder: Array[Int] = Array(500, 250, 125, 1)

  private var spawn: WeakLocation = _

  var worldBorder: WorldBorder = _

  private val standbyRunnable: BukkitRunnable = new BukkitRunnable {
    state = GameState.STANDBY
    var i = 1
    override def run(): Unit = {
      val players = members.length
      // プレイするため最低人数
      if(players < 2) {
        if(i > 3) i = 1
        bossbar.setTitle(matchMakingText(players) + "."*i)
        i+=1
      } else {
        cancel()
        ready()
      }
    }
  }

  private val readyRunnable: BukkitRunnable = new BukkitRunnable {
    val t = 0.005
    bossbar.setTitle("まもなく試合が始まります")
    override def run(): Unit = {
      if(bossbar.getProgress > 0)
        bossbar.setProgress(bossbar.getProgress - t)
      else {
        this.cancel()
        play()
      }
    }
  }

  /**
   * ゲームを初期化する
   */
  override def init(): Unit = {
    this.state = GameState.INIT
    this.spawn = this.mapInfo.locations.getOrElse("spawn", WeakLocation.empty)

    this.bossbar.removeAll()
    this.bossbar.setProgress(1d)
    this.bossbar.setTitle(matchMakingText(0))

    // ワールドボーダー = リング
    // TODO ダメージが入らない？要調査
    this.worldBorder = this.world.getWorldBorder
    this.worldBorder.setDamageBuffer(1.0)

    standbyRunnable.runTaskTimer(WarsCore.getInstance, 0L, 10L)
  }

  /**
   * ゲームのカウントダウンを開始する
   */
  override def ready(): Unit = {
    this.state = GameState.READY
    readyRunnable.runTaskTimer(WarsCore.getInstance, 0L, 1L)
  }

  /**
   * ゲームを開始する
   */
  override def play(): Unit = {
    this.state = GameState.PLAYING
    this.sendMessage(Component.text("ささやかな選択時間"))
    world.setPVP(true)
    new BukkitRunnable {
      override def run(): Unit = {
        processPhase()
      }
    }.runTaskLater(WarsCore.getInstance, 200L)
  }

  private def processPhase(): Unit = {
    // TODO これ全部スレッドに移せない？
    bossbar.setTitle(s"§aフェーズ: $phase")
    worldBorder.setDamageAmount(1.0 + phase * 2.0)

    this.playSound(Sound.BLOCK_BELL_USE, 2.0F, 1.8F)
    var _time: Int = phaseTime(phase)
    this.sendMessage(Component.text(s"フェーズ: ${phase}; ボーダー縮小まであと${_time}秒"))
    new BukkitRunnable {
      var _time: Int = phaseTime(phase)
      override def run(): Unit = {
        // TODO 人数モニタがあります
        if(state == GameState.PLAYING) {
          _time -= 1
          bossbar.setTitle(s"§aフェーズ: $phase | スタブ: あと${members.length}人 |(テスト表示)リング縮小まであと ${_time} 秒")
          if(_time < 10) {
            bossbar.setTitle(s"§aフェーズ: $phase | リング縮小まであと ${_time} 秒")
          }
          if(_time < 0) {
            cancel()
            processCloseBorder()
          }
        } else {
          cancel()
          end()
        }
      }
    }.runTaskTimer(WarsCore.getInstance, 0, 20L)
  }

  private def processCloseBorder(): Unit = {
    sendMessage(s"フェーズ ${phase}の縮小が開始")
    bossbar.setTitle(s"§aフェーズ: $phase")
    worldBorder.setSize(phaseBorder(phase), closePhaseBorderTime(phase))
    new BukkitRunnable {
      var _time: Int = closePhaseBorderTime(phase)
      override def run(): Unit = {
        if(state == GameState.PLAYING) {
          _time -= 1
          bossbar.setTitle(s"§aフェーズ: $phase")
          if(_time < 0) {
            phase += 1
            cancel()
            processPhase()
          }
        } else {
          cancel()
          end()
        }
      }
    }.runTaskTimer(WarsCore.getInstance, 0, 20L)
  }

  /**
   * ゲームを終了する
   */
  override def end(): Unit = {
    /*
    キルが確定したときの処理などにより，即時にend()メソッドを呼ぶ必要がある．
    既に終了状態となっている場合はこの処理は無視される
     */
    if(this.state == GameState.END) return
    this.state = GameState.END
    Bukkit.getPluginManager.callEvent(new GameEndEvent(this, null))
    members.foreach(wp => {
      wp.game = None
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
      wp.sendMessage(Component.text("リザルト: ").append(Component.newline())
        .append(Component.text(s"${wp.player.getName}")).append(Component.newline())
        .append(Component.text("キル: x\nダメージ: x\nほかなどなど: y\nランクポイント: -999")).append(Component.newline())
        .append(Component.text("{メンバー2}: ")).append(Component.newline())
        .append(Component.text("{メンバー3}: ")).append(Component.newline()))
    })
    bossbar.removeAll()
    // ユーザーデータの消去
    new BukkitRunnable {
      override def run(): Unit = {
        WorldLoader.asyncUnloadWorld(id)
        new BukkitRunnable {
          override def run(): Unit = {
            load()
          }
        }.runTaskLater(WarsCore.getInstance, 190L)
      }
    }.runTaskLater(WarsCore.getInstance, 10L)
  }

  /**
   * プレイヤーがゲームに参加するときのメソッド
   *
   * @param wp プレイヤー
   * @return 参加できる場合
   */
  override def join(wp: WPlayer): Unit = {
    if(!loaded && this.state == GameState.DISABLE) {
      this.load(Vector(wp.player))
    }
  }

  /**
   * プレイヤーがゲームから抜けたときのメソッド
   *
   * @param wp プレイヤー
   */
  override def hub(wp: WPlayer): Unit = {
    val team = WarsCoreAPI.scoreboard.getEntryTeam(wp.player.getName)
    if (team != null) {
      // チームから削除
      team.removeEntry(wp.player.getName)
    }
    // メンバーから削除
    members = members.filterNot(_ eq wp)
    // ボスバーから削除
    bossbar.removePlayer(wp.player)
    // ゲーム情報をリセット
    wp.game = None
    // スポーン情報をリセット
    wp.player.setBedSpawnLocation(Bukkit.getWorlds.get(0).getSpawnLocation)
    sendMessage(s"${wp.player.getName} が退出しました")
    new BukkitRunnable {
      override def run(): Unit = {
        if (wp.player.isOnline) {
          wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
          if (wp.player.getGameMode == GameMode.SPECTATOR) wp.player.setGameMode(GameMode.SURVIVAL)
          // インベントリをリストア
          WarsCoreAPI.restoreLobbyInventory(wp.player)
          // スコアボード情報をリセット
          wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
        }
      }
    }.runTaskLater(WarsCore.getInstance, 1L)
  }

  override def canJoin(wp: WPlayer): Option[Component] = {
    super.canJoin(wp)
    if (!state.join) {
      Some(Component.text("ゲームに参加できません！").color(NamedTextColor.RED))
    } else if (members.length >= maxMember) {
      Some(Component.text("人数が満員なので参加できません！").color(NamedTextColor.RED))
    } else {
      val event = new GameJoinEvent(this, wp)
      Bukkit.getPluginManager.callEvent(event)
      // TODO 改善できそう
      val cr = event.getCancelReason
      if (cr == null ||
        cr.isEmpty) {
        Some(Component.text(cr).color(NamedTextColor.RED))
      } else
        None
    }
  }

  def playSound(sound: Sound, volume: Float, pitch: Float): Unit = {
    members.map(_.player).foreach(f => f.playSound(
      f.getLocation,
      sound,
      volume,
      pitch
    ))
  }

  class PPEXData extends GamePlayerData {
    var rankPoint: Int = -99
    val scoreboard: Scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

    setupScoreboard()

    private def setupScoreboard(): Unit = {
      val oldObj = this.scoreboard.getObjective(DisplaySlot.SIDEBAR)
      if(oldObj != null) oldObj.unregister()
      val newObj = this.scoreboard.registerNewObjective("sidebar", "dummy", "あ")
      newObj.setDisplaySlot(DisplaySlot.SIDEBAR)
      WarsCoreAPI.setSidebarContents(
        newObj,
        List(
          s"キル数: ${this.kill}",
          s"アシスト数: ${this.assist}",
          s"ダメージ: ${this.damage}",
          s"RP: ${this.rankPoint}"
        )
      )
    }



  }
}
