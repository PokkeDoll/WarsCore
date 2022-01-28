package hm.moe.pokkedoll.warscore.games
import hm.moe.pokkedoll.warscore.events.{GameEndEvent, GameJoinEvent}
import hm.moe.pokkedoll.warscore.utils.{GameConfig, MapInfo, WeakLocation, WorldLoader}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.bukkit._

/**
 * スタブ:
 * ワールド中心: 22, 65, -2100
 * ワールド: hcg-0
 * テストスポーン1: -51, 64, -1859
 * テストスポーン2: 42, 72, -1815
 *
 * 開始時は暗闇からの状態
 */

class HardCoreGames(override val id : String) extends Game {
  override val newGameSystem: Boolean = false
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
  override val title: String = "HCG"
  /**
   * ボスバー
   */
  override val bossbar: BossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID)
  /**
   * ゲームの説明分
   */
  override val description: String = "最後まで生き残ります"
  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = 45
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

  var dynamicTime: Int = _

  var spawn: WeakLocation = _

  var phase = 0
  // フェーズ1, フェーズ2, フェーズ3, 最終リング
  val phaseTime: Array[Int] = Array(300, 240, 180, 60)
  // フェーズ1, フェーズ2, フェーズ3, 最終リング
  val closePhaseBorderTime: Array[Int] = Array(240, 180, 120, 120)

  val phaseBorder: Array[Int] = Array(500, 250, 125, 1)

  var loadingTask: BukkitTask = _

  var worldBorder: WorldBorder = _

  /**
   * ゲームを初期化する
   */
  override def init(): Unit = {
    state = GameState.INIT

    spawn = mapInfo.locations.getOrElse("spawn", WeakLocation.empty)

    bossbar.removeAll()
    bossbar.setProgress(1d)
    bossbar.setTitle("スタブ: フェーズ, ボーダー")

    worldBorder = world.getWorldBorder
    worldBorder.setDamageBuffer(5.0)

    loadingTask = new BukkitRunnable {
      var i = 0
      override def run(): Unit = {
        if(state == GameState.WAIT || state == GameState.STANDBY | state == GameState.READY) {
          if(i > 3) i = 0
          members.map(_.player).foreach(f => f.showTitle(Title.title(
            Component.text(WarsCoreAPI.loadingFont(i)),
            Component.text("準備中です"),
            Title.DEFAULT_TIMES
          )))
          i += 1
        } else {
          cancel()
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 5L)

    // val wg = WorldGuard.getInstance()
    // wg.getPlatform.getRegionContainer.get()

    state = GameState.STANDBY
  }

  /**
   * ゲームのカウントダウンを開始する
   */
  override def ready(): Unit = {
    state = GameState.READY
    sendMessage("§a10秒後に試合を始めます！")
    new BukkitRunnable {
      var count = 10
      override def run(): Unit = {
        if(canPlay) {
          if(count <= 0) {
            play()
            cancel()
          } else {
            sendMessage(s"スタブ: あと${count}秒で試合開始")
            count -= 1
          }
        } else {
          sendMessage("待機状態に戻ります")
          state = GameState.STANDBY
          cancel()
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  /**
   * ゲームを開始する
   */
  override def play(): Unit = {
    members.foreach(f => f.player.teleport(spawn.getLocation(world)))
    processPhase()
  }

  private def processPhase(): Unit = {
    state = GameState.PLAY
    world.setPVP(true)
    bossbar.setTitle(s"§aフェーズ: $phase")
    worldBorder.setDamageAmount(1.0 + phase * 2.0)

    new BukkitRunnable {
      var _time: Int = phaseTime(phase)
      override def run(): Unit = {
        if(canPlay) {
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
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  private def processCloseBorder(): Unit = {
    sendMessage(s"フェーズ ${phase}の縮小が開始")
    bossbar.setTitle(s"§aフェーズ: $phase | §aリング縮小中！")
    worldBorder.setSize(phaseBorder(phase), closePhaseBorderTime(phase))
    new BukkitRunnable {
      var _time: Int = closePhaseBorderTime(phase)
      override def run(): Unit = {
        if(canPlay) {
          _time -= 1
          bossbar.setTitle(s"§aフェーズ: $phase | §aリング縮小中 あと${_time}秒")
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
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  private def canPlay: Boolean = {
    // members.length > 2
    true
  }


  /**
   * ゲームを終了する
   */
  override def end(): Unit = {
    state = GameState.END
    world.setPVP(false)
    sendMessage("スタブ: ゲーム終了")
    Bukkit.getPluginManager.callEvent(new GameEndEvent(this, null))
    members.foreach(wp => {
      // ゲーム情報のリセット
      wp.game = None
      // チェストプレートをクリア
      wp.player.getInventory.setChestplate(new ItemStack(Material.AIR))
      // インベントリのリストア
      WarsCoreAPI.restoreLobbyInventory(wp.player)
      // スコアボードのリセット
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
    })

    val beforeMembers = members.map(_.player)
    sendMessage("&910秒後に自動で試合に参加します")
    world.getPlayers.forEach(p => p.teleport(Bukkit.getWorlds.get(0).getSpawnLocation))
    bossbar.removeAll()
    new BukkitRunnable {
      override def run(): Unit = {
        WorldLoader.asyncUnloadWorld(id)
        new BukkitRunnable {
          override def run(): Unit = {
            load(players = beforeMembers.filter(_.isOnline).filter(WarsCoreAPI.isContinue))
          }
        }.runTaskLater(WarsCore.instance, 190L)
      }
    }.runTaskLater(WarsCore.instance, 10L)
  }

  /**
   * プレイヤーがゲームに参加するときのメソッド
   *
   * @param wp プレイヤー
   * @return 参加できる場合
   */
  override def join(wp: WPlayer): Unit = {
    if(canJoinD(wp)) {
      val joinEvent = new GameJoinEvent(this, wp)
      Bukkit.getPluginManager.callEvent(joinEvent)
      if(joinEvent.getCancelReason != "") {
        wp.sendMessage(joinEvent.getCancelReason)
      } else {
        wp.sendMessage("スタブ: マップ説明")
        wp.game = Some(this)

        members :+= wp
        bossbar.addPlayer(wp.player)

        wp.sendMessage(s"スタブ: ${wp.player.getName}が参加")

        if(state == GameState.STANDBY) {
          println("do ready")
          ready()
        }
      }
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
    }.runTaskLater(WarsCore.instance, 1L)
  }
}
