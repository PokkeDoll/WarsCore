package hm.moe.pokkedoll.warsgame

import hm.moe.pokkedoll.warscore.events.{GameDeathEvent, GameEndEvent, GameJoinEvent}
import hm.moe.pokkedoll.warscore.games.{Game, GamePlayerData, GameState}
import hm.moe.pokkedoll.warscore.utils.{GameConfig, MapInfo, WeakLocation, WorldLoader}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit._
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.{ArmorStand, EntityType, Player}
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.{EquipmentSlot, Inventory, ItemStack}
import org.bukkit.scheduler.BukkitRunnable

import java.util.UUID
import scala.collection.mutable

class PPEX(override val id: String) extends Game {

  override val newGameSystem: Boolean = true

  this.debug = true

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

  val data = mutable.HashMap.empty[Player, PPEXData]

  val deathBox = mutable.HashMap.empty[UUID, Inventory]

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

    this.members = Vector.empty[WPlayer]

    this.data.clear()
    this.deathBox.clear()

    this.world.setPVP(true)
    standby()
  }

  private def standby(): Unit = {
    this.state = GameState.STANDBY
    new BukkitRunnable {
      var i = 1

      override def run(): Unit = {
        val players = members.length
        // プレイするため最低人数
        if (players < 1) {
          if (i > 3) i = 1
          bossbar.setTitle(matchMakingText(players) + "." * i)
          i += 1
        } else {
          cancel()
          ready()
        }
      }
    }.runTaskTimer(WarsCore.getInstance, 0L, 10L)
  }

  /**
   * ゲームのカウントダウンを開始する
   */
  override def ready(): Unit = {
    this.state = GameState.READY
    new BukkitRunnable {
      val t = 0.005
      bossbar.setTitle("まもなく試合が始まります")

      override def run(): Unit = {
        if (bossbar.getProgress > t)
          bossbar.setProgress(bossbar.getProgress - t)
        else {
          this.cancel()
          play()
        }
      }
    }.runTaskTimer(WarsCore.getInstance, 0L, 1L)
  }

  /**
   * ゲームを開始する
   */
  override def play(): Unit = {
    this.state = GameState.PLAYING
    members.foreach(f => f.player.teleport(spawn.getLocation(world)))
    this.sendMessage(Component.text("スタブ: 準備時間"))
    world.setPVP(true)
    new BukkitRunnable {
      override def run(): Unit = {
        processPhase()
      }
    }.runTaskLater(WarsCore.getInstance, 300L)
  }

  private def shouldGameSet: Boolean = {
    if(!this.debug && getSurvivedCount < 1) {
      true
    } else {
      members.exists(p => p.player.getInventory.getItemInMainHand.getType == Material.DIAMOND)
    }
  }

  private def processPhase(): Unit = {
    // TODO これ全部スレッドに移せない？
    bossbar.setTitle(s"§aフェーズ: $phase")
    worldBorder.setDamageAmount(1.0 + phase * 2.0)

    playSound(Sound.BLOCK_BELL_USE, 6.0F, 0.0F)
    var _time: Int = phaseTime(phase)
    sendMessage(Component.text(s"フェーズ: ${phase}; ボーダー縮小まであと${_time}秒"))
    new BukkitRunnable {
      var _time: Int = phaseTime(phase)

      var tpb: Double = 1.0 / _time

      override def run(): Unit = {
        // TODO 人数モニタがあります
        if (state == GameState.PLAYING) {
          _time -= 1
          bossbar.setTitle(s"§aフェーズ: $phase | 残り${getSurvivedCount}人 |(test${_time})")
          val npg = bossbar.getProgress - tpb
          if (0 < npg) bossbar.setProgress(npg)
          if (_time < 10) {
            bossbar.setTitle(s"§aフェーズ: $phase | リング縮小まであと ${_time} 秒")
          }
          if (_time < 0) {
            cancel()
            processCloseBorder()
          }
          if (shouldGameSet) {
            cancel()
            end()
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
        if (state == GameState.PLAYING) {
          _time -= 1
          bossbar.setTitle(s"§aフェーズ: $phase")
          if (_time < 0) {
            phase += 1
            cancel()
            processPhase()
          }
          if (shouldGameSet) {
            cancel()
            end()
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
    if (this.state == GameState.END) return
    this.state = GameState.END
    this.world.setPVP(false)
    this.data.filter(_._2.survived).keys.foreach(f => {
      f.showTitle(Title.title(
        Component.text("スタブ: 勝利"),
        Component.empty(),
        Title.DEFAULT_TIMES))
      f.playSound(f.getLocation, "minecraft:battle2", 2.0f, 1.0f)
    })
    this.data.filter(!_._2.survived).keys.foreach(f => f.showTitle(
      Title.title(Component.text("スタブ: 敗北")
        , Component.empty(), Title.DEFAULT_TIMES)
    ))
    Bukkit.getPluginManager.callEvent(new GameEndEvent(this, null))

    bossbar.removeAll()
    // ユーザーデータの消去
    var delay = 200L

    new BukkitRunnable {
      override def run(): Unit = {
        members.foreach(wp => {
          wp.game = None
          wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
          wp.sendMessage(Component.text("リザルト: ").append(Component.newline())
            .append(Component.text(s"${wp.player.getName}")).append(Component.newline())
            .append(Component.text("キル: x\nダメージ: x\nほかなどなど: y\nランクポイント: -999")).append(Component.newline())
            .append(Component.text("{メンバー2}: ")).append(Component.newline())
            .append(Component.text("{メンバー3}: ")).append(Component.newline()))
        })
      }
    }.runTaskLater(WarsCore.getInstance, delay)

    delay += 300L

    new BukkitRunnable {
      override def run(): Unit = {
        WorldLoader.asyncUnloadWorld(id)
      }
    }.runTaskLater(WarsCore.getInstance, delay)

    delay += 100L

    new BukkitRunnable {
      override def run(): Unit = {
        load()
      }
    }.runTaskLater(WarsCore.getInstance, delay)
  }



  /**
   * プレイヤーがゲームに参加するときのメソッド
   *
   * @param wp プレイヤー
   * @return 参加できる場合
   */
  override def join(wp: WPlayer): Unit = {
    if (false /*!loaded && this.state == GameState.DISABLE*/ ) {
      this.load(Vector(wp.player))
    } else {
      wp.game = Some(this)
      this.bossbar.addPlayer(wp.player)
      this.members :+= wp
      this.data.put(wp.player, new PPEXData)
      sendMessage(Component.text(wp.player.getName + " が参加"))

      WarsCoreAPI.scoreboards.get(wp.player) match {
        case Some(scoreboard) =>
          val sidebar = scoreboard.getObjective("sidebar")
          if (sidebar != null) {
            sidebar.displayName(Component.text("PPEX").color(NamedTextColor.DARK_GREEN))
            println(scoreboard.getEntries.forEach(print))
            scoreboard.getEntries.forEach(e => {
              scoreboard.resetScores(e)
            })
            WarsCoreAPI.setSidebarContents(
              sidebar,
              List(
                "プレイヤー1HP: <HP>",
                "プレイヤー2HP: <HP>",
                " ",
                "キル数: <KILL>",
                "アシスト数: <ASSIST>",
                "ダメージ数: <DAMAGE>",
                " ",
                s"ランクポイント: ${Int.MinValue}"
              )
            )
          }
        case None =>
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
    data.remove(wp.player)
    // ボスバーから削除
    bossbar.removePlayer(wp.player)
    // ゲーム情報をリセット
    wp.game = None
    // スポーン情報をリセット
    wp.player.setBedSpawnLocation(Bukkit.getWorlds.get(0).getSpawnLocation)
    // sendMessage(s"${wp.player.getName} が退出しました")
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
    if (!loaded && this.state == GameState.DISABLE) {
      this.load(Vector(wp.player))
      Some(Component.text("ゲームを起動しています"))
    } else if (!state.join) {
      Some(Component.text("ゲームに参加できません！").color(NamedTextColor.RED))
    } else if (members.length >= maxMember) {
      Some(Component.text("人数が満員なので参加できません！").color(NamedTextColor.RED))
    } else {
      val event = new GameJoinEvent(this, wp)
      Bukkit.getPluginManager.callEvent(event)
      // TODO 改善できそう
      val cr = event.getCancelReason
      if (cr == null || cr.isEmpty) {
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

  override def onDeath(e: PlayerDeathEvent): Unit = {
    super.onDeath(e)

    println(this.state.name)
    if (this.state != GameState.PLAYING && state != GameState.PLAYING_CANNOT_JOIN) return

    val victim = e.getEntity
    val gde = (attacker: Player) => new GameDeathEvent(this, attacker, victim)
    val vdata = data(victim)
    vdata.survived = false
    if (shouldGameSet) {
      this.end()
    }
    // キルログのための処理
    Option(victim.getKiller) match {
      case Some(attacker) =>
        WarsCoreAPI.getAttackerWeaponName(attacker) match {
          case Some(name) =>
            sendMessage(Component.text(s"${attacker.getName} [$name] → Killed → ${victim.getName}"))
          case None =>
            sendMessage(Component.text(s"${attacker.getName} → Killed → ${victim.getName}"))
        }
        Bukkit.getPluginManager.callEvent(gde(attacker))
      case None =>
        sendMessage(Component.text(s"Dead → ${victim.getName}"))
        Bukkit.getPluginManager.callEvent(gde(null))
    }

    spawnDeathBox(victim)

    // 死亡したプレイヤーの処理
    if (false /* もしチームメンバーがいるとき */ ) {

    } else {
      victim.showTitle(Title.title(
        Component.text("部隊全滅").color(NamedTextColor.DARK_RED),
        Component.empty(),
        Title.DEFAULT_TIMES))
      victim.setGameMode(GameMode.SPECTATOR)
      new BukkitRunnable {
        override def run(): Unit = {
          victim.sendMessage("スタブ: ここに報酬")
          victim.sendMessage("スタブ: ロビーに帰っても良い処理(強制送還)")
          hub(victim)
        }
      }.runTaskLater(WarsCore.getInstance, 60L)
    }
  }

  private def getSurvivedCount: Int = {
    data.values.map(_.survived).count(f => f)
  }

  class PPEXData extends GamePlayerData {
    var rankPoint: Int = -99
    var survived: Boolean = true
    /*
    val scoreboard: Scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

    setupScoreboard()

    private def setupScoreboard(): Unit = {
      val oldObj = this.scoreboard.getObjective(DisplaySlot.SIDEBAR)
      if (oldObj != null) oldObj.unregister()
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
    */
  }

  private def spawnDeathBox(victim: Player): Unit = {
    victim.getWorld.spawnEntity(victim.getLocation, EntityType.ARMOR_STAND, SpawnReason.CUSTOM) match {
      case base: ArmorStand =>
        base.setInvisible(true)
        base.setInvulnerable(true)
        base.setBasePlate(true)
        // 全部対象
        base.setDisabledSlots(EquipmentSlot.values(): _*)

        base.setHelmet(new ItemStack(Material.PUMPKIN))
        base.setVelocity(base.getVelocity.add(new org.bukkit.util.Vector(0, 1, 0)))

        deathBox.put(base.getUniqueId, Bukkit.createInventory(null, 36, Component.text(s"${victim.getName}'s DeathBox'")))
      case _ =>
    }
  }
}
