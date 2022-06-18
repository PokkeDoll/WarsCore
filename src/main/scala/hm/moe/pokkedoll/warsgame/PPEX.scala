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
import org.bukkit.event.{Event, HandlerList}
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.{EquipmentSlot, Inventory, ItemStack}
import org.bukkit.scheduler.BukkitRunnable
import peru.sugoi.ppapi.classes.Party

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
  override val maxMember: Int = 40

  val minMember: Int = 1
  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _
  override var mapInfo: MapInfo = _

  /**
   * 最大試合時間
   */
  override var maxTime: Int = _

  private val matchMakingText: Int => String = i => s"マッチメイキング中... $i/$maxMember"

  // フェーズ1, フェーズ2, フェーズ3, 最終リング
  //val phaseTime: Array[Int] = Array(300, 240, 180, 60)
  // 合計10分 = 600秒
  // 3m + 2m40s(P1) + 2m40s + 2m20s(P2) + 2m + 1m50s(P3) + 1m30s + 1m30s(P4) + 1m + 2m(PE)
  val phaseTime: Array[Int] = Array(180, 160, 160, 140, 120, 100, 90, 90, 30, 120)

  val phaseBorder: Array[Int] = Array(500, 250, 200, 120, 1)

  private var spawn: WeakLocation = _

  var worldBorder: WorldBorder = _

  val data = mutable.HashMap.empty[Player, PPEXData]

  val deathBox = mutable.HashMap.empty[UUID, Inventory]

  var killLeader: Option[Player] = None

  var parties: Vector[Party] = Vector.empty

  /**
   *
   */
  var currentPhase: Int = 0
  var currentTime: Int = 0

  /**
   * ゲームを初期化する
   */
  override def init(): Unit = {
    this.state = GameState.INIT

    this.spawn = this.mapInfo.locations.getOrElse("spawn", WeakLocation.empty)

    this.bossbar.removeAll()
    this.bossbar.setVisible(true)
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
        if (players < minMember) {
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
    bossbar.setVisible(false)
    new BukkitRunnable {
      override def run(): Unit = {
        processPhase(1)
      }
    }.runTaskLater(WarsCore.getInstance, 300L)
  }

  private def shouldGameSet: Boolean = {
    // リーダーの数が一人の時 = 勝利
    if (!this.debug && members.map(wp => Party.getParty(wp.player)).groupBy(_.getLeader).size <= 1) {
      true
    } else if (!this.debug && getSurvivedCount <= 1) {
      true
    } else {
      members.exists(p => p.player.getInventory.getItemInMainHand.getType == Material.DIAMOND)
      // members.exists(p => p.player.isSneaking)
    }
  }

  private def processPhase(phase: Int): Unit = {
    currentPhase = phase
    Bukkit.getPluginManager.callEvent(new PPEXStartPhaseEvent(this, phase))
    // リング縮小
    if (phase != 0 && phase % 2 == 0) {
      worldBorder.setSize(phaseBorder((phase / 2) - 1), phaseTime(phase - 1))
      playSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 6.0F, 0.0F)
      currentTime = phaseTime(phase - 1)
      sendMessage(Component.text(s"フェーズ: $phase; ボーダーの縮小が開始(完了まで: ${currentTime}秒)"))
    } else {
      //worldBorder.setDamageAmount(1.0 + phase)
      worldBorder.setDamageAmount(1.0)
      worldBorder.setDamageBuffer(16.0)
      playSound(Sound.BLOCK_BELL_USE, 6.0F, 0.0F)
      currentTime = phaseTime(phase - 1)
      sendMessage(Component.text(s"フェーズ: $phase; ボーダー縮小まであと${currentTime}秒"))
    }
    new BukkitRunnable {
      override def run(): Unit = {
        if (state == GameState.PLAYING) {
          currentTime -= 1
          if (currentTime < 0) {
            cancel()
            processPhase(phase + 1)
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
    }.runTaskTimer(WarsCore.getInstance, 0L, 20L)
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
    val leaders = members.map(wp => Party.getParty(wp.player)).groupBy(_.getLeader).keys.toSeq
    // 1位が決まっている場合
    if (leaders.size == 1) {
      Bukkit.getPluginManager.callEvent(new PPEXGameOverEvent(this, Party.getParty(leaders.head), 1))
    } else {
      leaders.map(Party.getParty).foreach(party => Bukkit.getPluginManager.callEvent(new PPEXGameOverEvent(this, party, 0)))
    }
    /*
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
    */
    Bukkit.getPluginManager.callEvent(new GameEndEvent(this, null))

    bossbar.removeAll()
    // ユーザーデータの消去
    var delay = 200L

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
                "マップ名: -",
                "縮小フェーズ: ∞"
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
    } else if (wp.game.isDefined) {
      Some(Component.text("既にゲームに参加しています！").color(NamedTextColor.RED))
    } else {
      val event = new GameJoinEvent(this, wp)
      Bukkit.getPluginManager.callEvent(event)
      // TODO 改善できそう
      val cr = event.getCancelReason
      if (cr == null || cr.isBlank) {
        None
      } else
        Some(Component.text(cr).color(NamedTextColor.RED))
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

  private def __getParty(): Int = 1

  override def onDeath(e: PlayerDeathEvent): Unit = {
    super.onDeath(e)

    if (this.state != GameState.PLAYING && state != GameState.PLAYING_CANNOT_JOIN) return

    val victim = e.getEntity

    val isVoid = victim.getLastDamageCause.getCause match {
      case DamageCause.VOID => true
      case _ => false
    }
    val vData = data(victim)

    if (isVoid || __getParty() == 1 || vData.knockdown) {
      // そのまま殺す
      vData.knockdown = false
    } else {
      vData.knockdown = true
      return
    }

    val gde = (attacker: Player) => new GameDeathEvent(this, attacker, victim)

    vData.survived = false
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
        val aData = data(attacker)
        aData.kill += 1
        Bukkit.getPluginManager.callEvent(gde(attacker))

        val aK = data.values.map(_.kill).max
        val a = data.filter(p => p._2.kill == Math.max(aK, 3))
        if ((this.killLeader.isEmpty && a.nonEmpty) || (this.killLeader.isDefined && !a.contains(this.killLeader.get))) {
          this.killLeader = a.keys.headOption
          Bukkit.getPluginManager.callEvent(new PPEXNewKillLeaderEvent(this, this.killLeader.get, aK))
        }
      case None =>
        sendMessage(Component.text(s"Dead → ${victim.getName}"))
        vData.death += 1
        Bukkit.getPluginManager.callEvent(gde(null))
    }

    spawnDeathBox(victim)

    val party = Party.getParty(victim)
    val survivedTeammate = party.getMembers.stream().map(p => data(p.asInstanceOf[Player]).survived).filter(p => p).findFirst().isPresent

    // 死亡したプレイヤーの処理
    if (survivedTeammate) {
      // 観戦モード
      world.sendMessage(Component.text(s"${victim.getName} died"))
    } else {
      world.sendMessage(Component.text(s"${party.getLeader.getName}'s party lost!'"))
      Bukkit.getPluginManager.callEvent(new PPEXGameOverEvent(this, party, -1))
      new BukkitRunnable {
        override def run(): Unit = {
          party.getMembers.stream()
            .filter(p => p.isOnline)
            .map(p => p.asInstanceOf[Player])
            .forEach(p => {
              p.sendMessage("スタブ: ここに報酬\nスタブ: ロビーに帰っても良い処理(強制送還)")
              hub(WarsCoreAPI.getWPlayer(p))
            })
        }
      }.runTaskLater(WarsCore.getInstance, 60L)
    }
  }

  override def onRespawn(e: PlayerRespawnEvent): Unit = {
    super.onRespawn(e)
    val player = e.getPlayer
    val vData = data(player)
    if (vData.knockdown) {
      e.setRespawnLocation(player.getLocation)
      Game.knockdown(player)
    }
  }

  private def getSurvivedCount: Int = {
    data.values.map(_.survived).count(f => f)
  }

  class PPEXData extends GamePlayerData {
    var rankPoint: Int = -99
    var survived: Boolean = true
    var knockdown: Boolean = false

    // キルストリーク
    var killStreak: Int = 0
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

  def getDataJava(player: Player): PPEXData = data(player)

  class PPEXNewKillLeaderEvent(val ppex: PPEX, val killLeader: Player, val killLeaderKillCount: Int) extends Event {
    override def getHandlers: HandlerList = PPEXNewKillLeaderEvent.handlers
  }

  object PPEXNewKillLeaderEvent {
    val handlers = new HandlerList()

    def getHandlerList: HandlerList = handlers
  }

  /**
   * パーティーがゲームオーバーになったときに呼ばれる。1位でも呼ばれる
   *
   * @param ppex  ゲームそのもの
   * @param party 対象のパーティー
   * @param rank  パーティーの順位。1位が決まらなかった場合は0が入る
   */
  class PPEXGameOverEvent(val ppex: PPEX, val party: peru.sugoi.ppapi.classes.Party, val rank: Int) extends Event {
    override def getHandlers: HandlerList = PPEXGameOverEvent.handlers
  }

  object PPEXGameOverEvent {
    val handlers = new HandlerList()

    def getHandlerList: HandlerList = handlers
  }

  class PPEXStartPhaseEvent(val ppex: PPEX, val phase: Int) extends Event {

    /**
     * 0じゃない偶数フェーズはワールドの縮小フェーズになる
     *
     * @return
     */
    def isClosingBorder: Boolean = phase != 0 && phase % 2 == 0

    override def getHandlers: HandlerList = PPEXStartPhaseEvent.handlers
  }

  object PPEXStartPhaseEvent {
    val handlers = new HandlerList()

    def getHandlerList: HandlerList = handlers
  }

  /**
   * 試合時間
   */
  override val time: Int = 0
}
