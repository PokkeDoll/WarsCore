package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.events.{GameDeathEvent, GameEndEvent, GameJoinEvent, GameStartEvent}
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{GameConfig, MapInfo, RankManager, WeakLocation, WorldLoader}
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{BaseComponent, ComponentBuilder}
import org.bukkit.{Bukkit, Color, FireworkEffect, GameMode, Location, Material, Sound, World, scheduler}
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.{Arrow, EntityType, Firework, Player}
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Objective, Team}

import scala.collection.mutable

/**
 * 3個所 + 各自軍の占領をかけて争うゲームモード <br>
 * リスポーンできる場所が無い、時間切れで占領数が多いチームの勝利 <br>
 *
 * @author Emorard
 * @version 1.0
 */
class Domination(override val id: String) extends Game {
  /**
   * 読み込むワールドのID.  最初は必ず0
   */
  override var worldId: String = s"$id"


  /**
   * ゲームの構成
   */
  override val config: GameConfig = GameConfig.getConfig("dom")

  /**
   * ゲームのタイトル
   */
  override val title: String = "ドミネーション"
  /**
   * ボスバー
   */
  override val bossbar: BossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID)
  /**
   * ゲームの説明分
   */
  override val description: String = "より多く占領したチームの勝利！！！"
  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = 30
  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _
  override var mapInfo: MapInfo = _
  /**
   * 試合時間
   */
  override val time: Int = 600

  private val scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

  val sidebar: Objective = scoreboard.registerNewObjective("sidebar", "dummy")
  sidebar.setDisplayName(ChatColor.GOLD + "戦況")
  sidebar.setDisplaySlot(DisplaySlot.SIDEBAR)

  var redTeam: Team = scoreboard.registerNewTeam(s"$id-red")
  redTeam.setColor(org.bukkit.ChatColor.RED)
  redTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)

  val blueTeam: Team = scoreboard.registerNewTeam(s"$id-blue")
  blueTeam.setColor(org.bukkit.ChatColor.BLUE)
  blueTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)

  WarsCoreAPI.setBaseTeam(redTeam)
  WarsCoreAPI.setBaseTeam(blueTeam)

  var spawnPoint: Location = _
  /**
   * 占領地点の状態, サイズは5
   * A,B,C
   */
  var captureData: Vector[CapturePoint] = _

  var redPoint: Location = _

  var bluePoint: Location = _

  val data = mutable.HashMap.empty[Player, DOMData]

  val buildRange = 6

  /**
   * ゲームを読み込む
   */
  override def load(players: Vector[Player] = Vector.empty[Player], mapInfo: Option[MapInfo] = None): Unit = {
    state = GameState.INIT
    this.mapInfo = mapInfo.getOrElse(scala.util.Random.shuffle(config.maps).head)
    WorldLoader.asyncLoadWorld(world = this.mapInfo.mapId, worldId = worldId, new Callback[World] {
      override def success(value: World): Unit = {
        world = value
        loaded = true
        disable = false
        init()
        players.foreach(join)
      }

      override def failure(error: Exception): Unit = {
        players.foreach(_.sendMessage(ChatColor.RED + "エラー！ワールドの読み込みに失敗しました！"))
        state = GameState.ERROR
      }
    })
  }

  /**
   * ゲームを初期化する
   */
  override def init(): Unit = {
    if (state != GameState.INIT) state = GameState.INIT

    bossbar.removeAll()
    bossbar.setProgress(1d)
    bossbar.setTitle(s"(DOM) ${mapInfo.mapName} §7|| §a-1")

    redTeam.getEntries.forEach(f => redTeam.removeEntry(f))
    blueTeam.getEntries.forEach(f => blueTeam.removeEntry(f))

    scoreboard.getEntries.forEach(scoreboard.resetScores(_))

    sidebar.getScore(ChatColor.RED + "拠点 赤軍").setScore(100)
    sidebar.getScore(ChatColor.BLUE + "拠点 青軍").setScore(100)
    sidebar.getScore(ChatColor.GRAY + "拠点 A").setScore(0)
    sidebar.getScore(ChatColor.GRAY + "拠点 B").setScore(0)
    sidebar.getScore(ChatColor.GRAY + "拠点 C").setScore(0)

    //setLocationData()
    setCapturePoint()

    data.clear()

    members = Vector.empty[WPlayer]

    world.setPVP(false)

    // 実に待機状態...! Joinされるまで待つ
    state = GameState.WAIT
  }

  /**
   * ゲームのカウントダウンを開始する
   */
  override def ready(): Unit = {
    state = GameState.READY
    new BukkitRunnable {
      val removeProgress: Double = 1d / 40d
      var count = 40

      override def run(): Unit = {
        if (members.length < 2) {
          bossbar.setProgress(1.0)
          sendMessage("&c人数が足りないため待機状態に戻ります")
          state = GameState.WAIT
          cancel()
        } else if (count <= 0) {
          play()
          cancel()
        } else {
          try {
            bossbar.setTitle(s"§fあと§a$count§f秒で試合が始まります!")
            bossbar.setProgress(bossbar.getProgress - removeProgress)
            count -= 1
          } catch {
            case e: IllegalArgumentException =>
              e.printStackTrace()
              sendMessage("エラーが発生したため待機状態に戻ります")
              bossbar.setProgress(1.0)
              state = GameState.WAIT
              cancel()
          }
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  /**
   * ゲームを開始する
   */
  override def play(): Unit = {
    //WarsCoreAPI.noticeStartGame(this)
    state = GameState.PLAY
    world.setPVP(true)
    // チーム決め + 移動
    members.map(_.player).foreach(p => {
      setTeam(p)
      spawn(p)
      if (redTeam.hasEntry(p.getName)) {
        p.sendTitle("§7YOU ARE §cRED §7TEAM!", "- §6TDM §f- Kill §9Blue §fteam!", 30, 20, 20)
      } else {
        p.sendTitle("§7YOU ARE §9BLUE §7TEAM!", "- §6TDM §f- Kill §cRed §fteam!", 30, 20, 20)
      }
    })

    Bukkit.getServer.getPluginManager.callEvent(new GameStartEvent(this))
    WarsCoreAPI.playBattleSound(this)

    val captureParam = if(members.length >= 10) 1 else 3

    new BukkitRunnable {
      var TIME: Int = time

      override def run(): Unit = {
        if (
          members.length < 2 || // メンバーが一人
            redTeam.getSize <= 0 || // チーム0人
            blueTeam.getSize <= 0 || //
            TIME <= 0 || // 時間切れ
            disable || // 何か原因があって無効化
            captureData.forall(p => p.team == "red" || p.team == "blue")
        ) {
          // ゲーム強制終了
          end()
          cancel()
        } else {
          // それ以外
          if (TIME <= 5) members.map(_.player).foreach(f => f.playSound(f.getLocation, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0f))
          if (TIME == 60) state = GameState.PLAY2
          captureData.foreach(f => {
            f.location.getNearbyPlayers(3d, 6d).forEach(p => {
              val d = data(p)
              // TODO 確かに連続して占領が行われることはないが、指を加えて見ないといけない！！
              if(d.team != f.team) {
                // データ上は 1 ~ 100: スコアボードでは 0 ~ 100
                if (d.team == "red") {
                  f.count += captureParam
                } else {
                  f.count -= captureParam
                }
              }
            })
            // TODO ゴミみたいなコードの修正
            // 赤ちーむ
            if(f.count > 0) {
              // 赤の勝ち
              if(f.count >= 100 && f.team != "red") {
                occupy(f, Material.RED_STAINED_GLASS)
              } else if (f.team == "neutral") {
                // 中立から赤へ
                if(f.name.startsWith("§7")) {
                  sendActionBar("TEST => SUC R: A &' A &'")
                  scoreboard.resetScores(f.name)
                  val stripName = ChatColor.stripColor(f.name)
                  val name = Array(stripName.substring(0, stripName.length / 2), stripName.substring(stripName.length / 2, stripName.length))
                  f.name =ChatColor.RED + name(0) + ChatColor.GRAY + name(1)
                }
                sidebar.getScore(f.name).setScore(calcScore(f.count))
              } else if (f.team == "blue") {
                occupy(f)
              } else {
                sidebar.getScore(f.name).setScore(calcScore(f.count))
              }
            // 青ちーむ
            } else if (f.count < 0) {
              // 青の勝ち
              if(f.count <= -100 && f.team != "blue") {
                occupy(f, Material.BLUE_STAINED_GLASS)
              } else if (f.team == "neutral") {
                // 中立から赤へ
                if(f.name.startsWith("§7")) {
                  sendActionBar("TEST => SUC R: A &' A &'")
                  scoreboard.resetScores(f.name)
                  val stripName = ChatColor.stripColor(f.name)
                  val name = Array(stripName.substring(0, stripName.length / 2), stripName.substring(stripName.length / 2, stripName.length))
                  f.name =ChatColor.BLUE + name(0) + ChatColor.GRAY + name(1)
                }
                sidebar.getScore(f.name).setScore(calcScore(f.count))
              } else if (f.team == "red") {
                occupy(f)
              } else {
                sidebar.getScore(f.name).setScore(calcScore(f.count))
              }
            // 0なら!=>中立
            } else if (f.team != "neutral") {
              occupy(f)
            }
          })

          TIME -= 1
          val splitTime = WarsCoreAPI.splitToComponentTimes(TIME)
          bossbar.setProgress(((d: Double) => if(d < 0d) 0d else d)(TIME * 0.0016))
          bossbar.setTitle(s"(DOM) ${mapInfo.mapName} §7|| §a${splitTime._2} 分 ${splitTime._3} 秒")
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  private def calcScore(score: Int): Int = {
    Math.abs(score)
  }

  /**
   * ゲームを終了する
   */
  override def end(): Unit = {
    state = GameState.END
    world.setPVP(false)
    // ここで勝敗を決める
    // groupByでdraw: {}, red: {}, blue: {}となる
    val winner = captureData.groupBy(f => f.team).map(f => (f._1, f._2.length)) match {
      case f if f.getOrElse("red", 0) > f.getOrElse("blue", 0) =>
        "red"
      case f if f.getOrElse("blue", 0) > f.getOrElse("red", 0) =>
        "blue"
      case _ =>
        "neutral"
    }

    Bukkit.getPluginManager.callEvent(new GameEndEvent(this, winner))

    val endMsg = new ComponentBuilder("- = - = - = - = - = - = - = - = - = - = - = -\n\n").color(ChatColor.GRAY).underlined(true)
      .append("               Game Over!\n").bold(true).underlined(false).color(ChatColor.WHITE)

    if (winner == "red") {
      endMsg.append("              ")
        .append("Red Team").color(ChatColor.RED).bold(false).underlined(true)
        .append(" won!                \n").color(ChatColor.WHITE).underlined(false)
    } else if (winner == "blue") {
      endMsg.append("              ")
        .append("Blue Team").color(ChatColor.BLUE).bold(false).underlined(true)
        .append(" won!                \n").color(ChatColor.WHITE).underlined(false)
    } else {
      endMsg.append("                  ").reset()
        .append("              Draw").underlined(true)
        .append("                \n").reset()
    }

    sendMessage(endMsg.create())

    members.foreach(wp => {
      data.get(wp.player) match {
        case Some(d) =>
          if (winner == d.team) {
            //d.money += 500
            // EconomyUtil.give(wp.player, EconomyUtil.COIN, 30)
            d.win = true
          }
          wp.sendMessage(createResult(d, winner): _*)
          // ゲーム情報のリセット
          wp.game = None
          // スコアボードのリセット
          wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
          RankManager.giveExp(wp, d.calcExp())
        case _ =>
      }
    })
    //WarsCore.instance.database.updateTDMAsync(this)
    val beforeMembers = members.map(_.player)
    world.getPlayers.forEach(p => p.teleport(Bukkit.getWorlds.get(0).getSpawnLocation))
    sendMessage(ChatColor.BLUE + "10秒後に自動で試合に参加します")
    bossbar.removeAll()
    new BukkitRunnable {
      override def run(): Unit = {
        WorldLoader.asyncUnloadWorld(id)
        new BukkitRunnable {
          override def run(): Unit = {
            load(players = beforeMembers.filter(player => player.getWorld == world))
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
  override def join(wp: WPlayer): Boolean = {
    val event = new GameJoinEvent(this, wp)
    if (wp.game.isDefined) {
      wp.sendMessage("ほかのゲームに参加しています!")
    } else if (!loaded && state == GameState.DISABLE) {
      load()
    } else if (!state.join) {
      wp.player.sendMessage("§cゲームに参加できません!")
    } else if (members.length >= maxMember) {
      wp.player.sendMessage("§c人数が満員なので参加できません！")
    } else {
      event.setAccept(true)
      wp.sendMessage(
        s"マップ名: &a${mapInfo.mapName}\n" +
          s"製作者: &a${mapInfo.authors}\n" +
          s"退出する場合は&a/game quit&7もしくは&a/game leave\n" +
          "&a/invite <player>&fで他プレイヤーを招待することができます!"
      )
      wp.game = Some(this)
      // インベントリを変更
      WarsCoreAPI.changeWeaponInventory(wp)
      wp.player.setScoreboard(scoreboard)
      data.put(wp.player, new DOMData)
      bossbar.addPlayer(wp.player)
      members = members :+ wp
      sendMessage(s"§a${wp.player.getName}§fが参加しました (§a${members.length} §f/§a$maxMember§f)")
      state match {
        case GameState.PLAY =>
          setTeam(wp.player)
          new scheduler.BukkitRunnable {
            override def run(): Unit = {
              spawn(wp.player)
              if (redTeam.hasEntry(wp.player.getName)) {
                sendMessage(s"${wp.player.getName}が§cRED§fチームに参加しました")
              } else {
                sendMessage(s"${wp.player.getName}が§9BLUE§fチームに参加しました")
              }
            }
          }.runTaskLater(WarsCore.instance, 2L)
        case GameState.READY =>
          wp.player.teleport(spawnPoint)
        case GameState.WAIT =>
          wp.player.teleport(spawnPoint)
          if (members.length >= 2) {
            ready()
          }
        case _ =>
          return false
      }
    }
    Bukkit.getPluginManager.callEvent(event)
    event.isAccept
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
    sendMessage(s"${wp.player.getName} が退出しました")
    if (wp.player.isOnline) {
      if (wp.player.getGameMode == GameMode.SPECTATOR) wp.player.setGameMode(GameMode.SURVIVAL)
      // スコアボード情報をリセット
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
      wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
      // インベントリをリストア
      WarsCoreAPI.restoreLobbyInventory(wp.player)
    }
  }

  /**
   * プレイヤーが死亡したときのイベント
   *
   * @param e イベント
   */
  override def death(e: PlayerDeathEvent): Unit = {
    e.setCancelled(true)
    val victim = e.getEntity
    // 試合中のみのできごと
    if (state == GameState.PLAY || state == GameState.PLAY2) {
      val preEvent = (attacker: Player) => new GameDeathEvent(attacker=attacker, victim=victim, game=this)
      val vData: DOMData = data(victim)
      vData.death += 1
      Option(victim.getKiller) match {
        case Some(attacker) =>
          val aData = data(attacker)
          aData.kill += 1
          // 同じIPアドレスなら報酬をスキップする
          if (attacker.isOp || victim.getAddress != attacker.getAddress) {
            // EconomyUtil.give(attacker, EconomyUtil.COIN, 3)
          }
          WarsCore.instance.database.addItem(
            attacker.getUniqueId.toString,
            config.onKillItem
          )
          e.setShouldPlayDeathSound(true)
          e.setDeathSound(Sound.ENTITY_PLAYER_LEVELUP)
          e.setDeathSoundVolume(2f)
          // 赤チーム用のメッセージ
          if (aData.team == "red") {
            WarsCoreAPI.getAttackerWeaponName(attacker) match {
              case Some(name) =>
                sendMessage(s"§f0X §c${attacker.getName} §f[$name§f] §7-> §0Killed §7-> §9${victim.getName}")
              case None =>
                sendMessage(s"§f0X §c${attacker.getName} §7-> §0Killed §7-> §9${victim.getName}")
            }
            // 青チーム用のメッセージ
          } else {
            WarsCoreAPI.getAttackerWeaponName(attacker) match {
              case Some(name) =>
                sendMessage(s"§f0X §9${attacker.getName} §f[$name§f] §7-> §0Killed §7-> §c${victim.getName}")
              case None =>
                sendMessage(s"§f0X §9${attacker.getName} §7-> §0Killed §7-> §c${victim.getName}")
            }
          }
          Bukkit.getServer.getPluginManager.callEvent(preEvent(attacker))
        case None =>
          sendMessage(s"§f0X ${victim.getName} dead")
          Bukkit.getServer.getPluginManager.callEvent(preEvent(null))
      }
      // とにかく死んだのでリスポン処理
      spawn(victim, coolTime = true)
    } else {

    }
  }

  /**
   * プレイヤーがダメージを受けた時のイベント
   *
   * @param e イベント
   */
  override def damage(e: EntityDamageByEntityEvent): Unit = {
    // ここで、ダメージを受けたエンティティはPlayerであることがわかっている
    val victim = e.getEntity.asInstanceOf[Player]
    val vData = data(victim)
    val d = (attacker: Player) => data.get(attacker).foreach(aData => {
      aData.damage += e.getFinalDamage
      vData.damagedPlayer.add(attacker)
    })
    e.getDamager match {
      case attacker: Player => d(attacker)
      case arrow: Arrow =>
        arrow.getShooter match {
          case attacker: Player => d(attacker)
          case _ =>
        }
      case _ =>
    }
    vData.damaged += e.getFinalDamage
  }

  /**
   * ブロックを破壊するときに呼び出されるイベント
   *
   * @param e イベント
   */
  override def break(e: BlockBreakEvent): Unit = {
    if (!canBuild(e.getBlock.getLocation)) {
      e.getPlayer.sendActionBar(ChatColor.RED + "そのブロックを壊すことはできません！")
      e.setCancelled(true)
    }
  }

  /**
   * ブロックを設置するときに呼び出されるイベント
   *
   * @param e イベント
   */
  override def place(e: BlockPlaceEvent): Unit = {
    if (!canBuild(e.getBlock.getLocation)) {
      e.getPlayer.sendActionBar(ChatColor.RED + "その地点にブロックを置くことはできません！")
      e.setCancelled(true)
    }
  }

  /**
   * deathメソッドで状態はPLAY or PLAY2
   *
   * @param player Player
   */
  private def spawn(player: Player, coolTime: Boolean = false): Unit = {
    if (coolTime) player.setGameMode(GameMode.SPECTATOR)
    val d = data(player)
    var spawnTime: Int = 5
    new BukkitRunnable {
      override def run(): Unit = {
        if (!coolTime) {
          WarsCoreAPI.getWPlayer(player).changeInventory = true
          if (d.team == "red") {
            player.teleport(redPoint)
          } else {
            player.teleport(bluePoint)
          }
        } else if (player.getKiller != null) {
          player.setSpectatorTarget(player.getKiller)
        }
        if (coolTime) {
          WarsCoreAPI.freeze(player)
          new BukkitRunnable {
            override def run(): Unit = {
              if (state == GameState.PLAY || state == GameState.PLAY2) {
                if (0 >= spawnTime) {
                  WarsCoreAPI.unfreeze(player)
                  if (d.team == "red") {
                    player.teleport(redPoint)
                  } else {
                    player.teleport(bluePoint)
                  }
                  player.addPotionEffect(PotionEffectType.ABSORPTION.createEffect(100, 10), true)
                  player.setGameMode(GameMode.SURVIVAL)
                  cancel()
                } else {
                  player.sendActionBar(s"§bリスポーンするまであと§a$spawnTime§b秒")
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 2f)
                  spawnTime -= 1
                }
              } else {
                WarsCoreAPI.unfreeze(player)
                WarsCoreAPI.setActiveWeapons(player)
                player.setGameMode(GameMode.SURVIVAL)
                cancel()
              }
            }
          }.runTaskTimer(WarsCore.instance, 0L, 20L)
        }
      }
    }.runTaskLater(WarsCore.instance, 1L)
  }

  private def setCapturePoint(): Unit = {
    val spawn = mapInfo.locations.getOrElse("spawn", WeakLocation.empty)
    val red = mapInfo.locations.getOrElse("red", WeakLocation.empty)
    val blue = mapInfo.locations.getOrElse("blue", WeakLocation.empty)
    val a = mapInfo.locations.getOrElse("a", WeakLocation.empty)
    val b = mapInfo.locations.getOrElse("b", WeakLocation.empty)
    val c = mapInfo.locations.getOrElse("c", WeakLocation.empty)
    spawnPoint = spawn.getLocation(world)
    redPoint = red.getLocation(world)
    bluePoint = blue.getLocation(world)
    captureData = Vector(
      // -100 = 青, 100 = 赤, -99 ~ 99 = なし
      new CapturePoint(ChatColor.GRAY + "拠点 A", "neutral", 0, a.getLocation(world)),
      new CapturePoint(ChatColor.GRAY + "拠点 B", "neutral", 0, b.getLocation(world)),
      new CapturePoint(ChatColor.GRAY + "拠点 C", "neutral", 0, c.getLocation(world))
    )
  }

  private def setTeam(p: Player): Unit = {
    if (redTeam.getEntries.size() > blueTeam.getEntries.size()) {
      blueTeam.addEntry(p.getName)
      data(p).team = "blue"
    } else if (redTeam.getEntries.size() < blueTeam.getEntries.size()) {
      redTeam.addEntry(p.getName)
      data(p).team = "red"
    } else {
      WarsCoreAPI.random.nextInt(2) match {
        case 1 =>
          blueTeam.addEntry(p.getName)
          data(p).team = "blue"
        case 0 =>
          redTeam.addEntry(p.getName)
          data(p).team = "red"
      }
    }
  }

//TODO 報酬
  // TODO なぜか負の値になる
  private def occupy(point: CapturePoint, flag: Material = Material.WHITE_STAINED_GLASS): Unit = {
    scoreboard.resetScores(point.name)
    if (flag == Material.RED_STAINED_GLASS) {
      point.team = "red"
      point.name = ChatColor.RED + ChatColor.stripColor(point.name)
      sendMessage(ChatColor.RED + "赤チーム" + ChatColor.WHITE + "が占拠")
      sidebar.getScore(point.name).setScore(100)
      WarsCoreAPI.createFirework(point.location.clone().add(0d, 3d, 0d), Color.RED, FireworkEffect.Type.CREEPER)

    } else if (flag == Material.BLUE_STAINED_GLASS) {
      point.team = "blue"
      point.name = ChatColor.BLUE + ChatColor.stripColor(point.name)
      sendMessage(ChatColor.BLUE + "青チーム" + ChatColor.WHITE + "が占拠")
      sidebar.getScore(point.name).setScore(100)
      WarsCoreAPI.createFirework(point.location.clone().add(0d, 3d, 0d), Color.BLUE, FireworkEffect.Type.CREEPER)
    } else { // = 0
      point.team = "neutral"
      point.name = ChatColor.GRAY + ChatColor.stripColor(point.name)
      sendMessage(s"占領が解除？")
      sidebar.getScore(point.name).setScore(0)
    }
    val loc = point.location.clone().add(0, 1, 0)
    for (i <- -1 to 1) {
      for (j <- -1 to 1) {
        val block = world.getBlockAt(loc.getBlockX + i, loc.getBlockY, loc.getBlockZ + j)
        block.setType(flag)
      }
    }
  }

  private def canBuild(location: Location): Boolean = {
    captureData.exists(p => (location.getX >= p.location.getX - buildRange && location.getX <= p.location.getX + buildRange) &&
      (location.getY >= p.location.getY + 2 && location.getY <= p.location.getY + buildRange + 2) &&
      (location.getZ >= p.location.getZ - buildRange && location.getZ <= p.location.getZ + buildRange))
  }

  private def createResult(data: DOMData, winner: String): Array[BaseComponent] = {
    val comp = new ComponentBuilder("- = - = - = - = - = ").color(ChatColor.GRAY).underlined(true)
      .append("戦績").underlined(false).bold(true).color(ChatColor.AQUA)
      .append("- = - = - = - = - = \n\n").underlined(true).bold(false).color(ChatColor.GRAY)
      .append("* ").underlined(false).color(ChatColor.WHITE)
      .append("結果: ").color(ChatColor.GRAY)

    if (winner == "draw") {
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

    comp.append("* ")
      .append("K/D: ").color(ChatColor.GRAY)
      .append((data.kill / (data.death + 1).toDouble).toString).color(ChatColor.GREEN).bold(true)
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

  class CapturePoint(var name: String , var team: String, var count: Int, var location: Location)

  /**
   * Java用メソッド。Optionalではないためnullの可能性がある。
   * @param player データを持つプレイヤー。
   * @return 現時点のDOMのデータ。存在しないならnull
   */
  def getDataUnsafe(player: Player): DOMData = data(player)

  /**
   * 試合中の一時的なデータを管理するクラス
   */
  class DOMData {
    // 順に, キル, デス, アシスト, ダメージ量, 受けたダメージ量
    var kill, death, assist: Int = 0
    var damage, damaged: Double = 0d
    var win = false
    var damagedPlayer = mutable.Set.empty[Player]
    var team = ""

    def calcExp(): Int = {
      kill * 5 + death + assist + (if (win) 100 else 0)
    }
  }
}
