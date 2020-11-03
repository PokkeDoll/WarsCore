package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{EconomyUtil, MapInfo, RankManager, WorldLoader}
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
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
   * 赤、青、A,B,C
   */
  var captureData: Vector[CapturePoint] = _

  val data = mutable.HashMap.empty[Player, DOMData]

  val buildRange = 6

  /**
   * ゲームを読み込む
   */
  override def load(): Unit = {
    state = GameState.INIT
    //WarsCoreAPI.mapinfo.foreach(f => println(s"LKIST! + ${f.gameId} ${f.mapId}"))
    val worlds = WarsCoreAPI.mapinfo.filter(_.gameId == "dom")
    val info = scala.util.Random.shuffle(worlds).head
    WorldLoader.syncLoadWorld(s"doms/${info.mapId}", id) match {
      case Some(world) =>
        mapInfo = info
        // 一応代入したが別のインスタンス(load, init)中にworldを参照するのは危険！読み込みエラーとなってコンソールを汚しまくる
        this.world = world
        //
        if (!loaded) loaded = true
        // 無効化を解除
        if (disable) disable = false
        // 読み込みに成功したので次のステージへ
        init()
      case None =>
        WarsCore.instance.getLogger.severe(s"World loading failed at ${info.mapId} on $id!")
        // 通常失敗することはないので不具合を拡大させないために無効化する
        state = GameState.DISABLE
    }
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

    WarsCoreAPI.playBattleSound(this)

    // 効率化のため割り算を先に計算する
    val div = 1 / 100.0

    new BukkitRunnable {
      var TIME: Int = time

      override def run(): Unit = {
        if (
          members.length < 2 || // メンバーが一人
            redTeam.getSize <= 0 || // チーム0人
            blueTeam.getSize <= 0 || //
            time <= 0 || // 時間切れ
            disable || // 何か原因があって無効化
            captureData(0).team == "blue" || // 敵陣を占領したとき
            captureData(1).team == "red"
        ) {
          // TODO 試合が終わらない。(版木愛の値)
          /*
                  at java.lang.Thread.run(Thread.java:834) [?:?]
[01:48:04 WARN]: [WarsCore] Task #12726 for WarsCore v0.39.18 generated an exception
java.lang.IllegalArgumentException: Progress must be between 0.0 and 1.0 (-0.312)
        at com.google.common.base.Preconditions.checkArgument(Preconditions.java:191) ~[patched_1.12.2.jar:git-Paper-1618]
        at org.bukkit.craftbukkit.v1_12_R1.boss.CraftBossBar.setProgress(CraftBossBar.java:123) ~[patched_1.12.2.jar:git-Paper-1618]
        at hm.moe.pokkedoll.warscore.games.Domination$$anon$2.run(Domination.scala:251) ~[?:?]
        at org.bukkit.craftbukkit.v1_12_R1.scheduler.CraftTask.run(CraftTask.java:64) ~[patched_1.12.2.jar:git-Paper-1618]
        at org.bukkit.craftbukkit.v1_12_R1.scheduler.CraftScheduler.mainThreadHeartbeat(CraftScheduler.java:423) ~[patched_1.12.2.jar:git-Paper-1618]
        at net.minecraft.server.v1_12_R1.MinecraftServer.D(MinecraftServer.java:840) ~[patched_1.12.2.jar:git-Paper-1618]
        at net.minecraft.server.v1_12_R1.DedicatedServer.D(DedicatedServer.java:423) ~[patched_1.12.2.jar:git-Paper-1618]
        at net.minecraft.server.v1_12_R1.MinecraftServer.C(MinecraftServer.java:774) ~[patched_1.12.2.jar:git-Paper-1618]
        at net.minecraft.server.v1_12_R1.MinecraftServer.run(MinecraftServer.java:666) ~[patched_1.12.2.jar:git-Paper-1618]
        at java.lang.Thread.run(Thread.java:834) [?:?]
           */
          // ゲーム強制終了
          end()
          cancel()
        } else {
          // それ以外
          if (time <= 5) members.map(_.player).foreach(f => f.playSound(f.getLocation, Sound.BLOCK_NOTE_HAT, 1f, 0f))
          if (time == 60) state = GameState.PLAY2
          captureData.foreach(f => {
            f.location.getNearbyPlayers(3d, 6d).forEach(p => {
              val d = data(p)
              // TODO 確かに連続して占領が行われることはないが、指を加えて見ないといけない！！
              if(d.team != f.team) {
                // 101 ~ 200 赤の範囲
                if (d.team == "red") {
                  f.count += 1
                  val score = (((f.count - 100) * div) * 100.0).toInt
                  sidebar.getScore(f.name).setScore(score)
                  if (f.count == 100) {
                    occupy(f, 0.toByte)
                  } else if (f.count >= 200) {
                    //occupy("red", ChatColor.RED + "赤チーム", Color.RED, 14.toByte)
                    occupy(f, 14.toByte)
                  }
                  // 0 ~ 99 青の範囲
                } else {
                  f.count -= 1
                  val score = (((f.count - 100) * div) * 100.0).toInt
                  sidebar.getScore(f.name).setScore(score)
                  if (f.count == 100) {
                    occupy(f, 0.toByte)
                  } else if (0 >= f.count) {
                    //occupy("blue", ChatColor.BLUE + "青チーム", Color.BLUE, 11.toByte)
                    occupy(f, 11.toByte)
                  }
                }
              }
              // TODO スコアボード表示
            })
          })

          TIME -= 1
          val splitTime = WarsCoreAPI.splitToComponentTimes(TIME)
          bossbar.setProgress(((d: Double) => if(d < 0d) 0d else d)(TIME * 0.0016))
          bossbar.setTitle(s"(DOM) ${mapInfo.mapName} §7|| §a${splitTime._2} 分 ${splitTime._3} 秒")
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  /**
   * ゲームを終了する
   */
  override def end(): Unit = {
    state = GameState.END
    world.setPVP(false)
    // ここで勝敗を決める
    //TODO 勝敗を決める
    val winner = "??????????????????????????"
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
        .append("?????????????????????????????????????????????????????").underlined(true)
        .append("                \n").reset()
    }

    sendMessage(endMsg.create())

    members.foreach(wp => {
      data.get(wp.player) match {
        case Some(d) =>
          if (winner == d.team) {
            //d.money += 500
            EconomyUtil.give(wp.player, EconomyUtil.COIN, 30)
            d.win = true
          }
          //wp.sendMessage(createResult(d, winner): _*)
          // ゲーム情報のリセット
          wp.game = None
          // スコアボードのリセット
          wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
          RankManager.giveExp(wp, d.calcExp())
        case _ =>
      }
    })
    //WarsCore.instance.database.updateTDMAsync(this)
    sendMessage("&95秒後にロビーに戻ります...")
    bossbar.removeAll()
    new BukkitRunnable {
      override def run(): Unit = {
        delete()
      }
    }.runTaskLater(WarsCore.instance, 100L)
  }

  /**
   * ゲームを削除する
   */
  override def delete(): Unit = {
    WorldLoader.syncUnloadWorld(id)
    new BukkitRunnable {
      override def run(): Unit = {
        load()
      }
    }.runTaskLater(WarsCore.instance, 200L)
  }

  /**
   * プレイヤーがゲームに参加するときのメソッド
   *
   * @param wp プレイヤー
   * @return 参加できる場合
   */
  override def join(wp: WPlayer): Boolean = {
    if (wp.game.isDefined) {
      wp.sendMessage("ほかのゲームに参加しています!")
      false
    } else if (!loaded && state == GameState.DISABLE) {
      load()
      false
    } else if (!state.join) {
      wp.player.sendMessage("§cゲームに参加できません!")
      false
    } else if (members.length >= maxMember) {
      wp.player.sendMessage("§c人数が満員なので参加できません！")
      false
    } else {
      wp.sendMessage(
        s"マップ名: &a${mapInfo.mapName}\n" +
          s"製作者: &a${mapInfo.authors}\n" +
          s"退出する場合は&a/game quit&7もしくは&a/game leave\n" +
          "&a/invite <player>&fで他プレイヤーを招待することができます!"
      )
      wp.game = Some(this)
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
      true
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
    sendMessage(s"${wp.player.getName} が退出しました")
    if (wp.player.isOnline) {
      if (wp.player.getGameMode == GameMode.SPECTATOR) wp.player.setGameMode(GameMode.SURVIVAL)
      // スコアボード情報をリセット
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
      wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
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
      val vData: DOMData = data(victim)
      vData.death += 1
      Option(victim.getKiller) match {
        case Some(attacker) =>
          val aData = data(attacker)
          aData.kill += 1
          // 同じIPアドレスなら報酬をスキップする
          if (attacker.isOp || victim.getAddress != attacker.getAddress) {
            EconomyUtil.give(attacker, EconomyUtil.COIN, 3)
          }
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
        case None =>
          sendMessage(s"§f0X ${victim.getName} dead")
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
          if (d.team == "red") {
            player.teleport(captureData(0).location)
          } else {
            player.teleport(captureData(1).location)
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
                    player.teleport(captureData(0).location)
                  } else {
                    player.teleport(captureData(1).location)
                  }
                  player.addPotionEffect(PotionEffectType.ABSORPTION.createEffect(100, 10), true)
                  WarsCoreAPI.setChangeInventory(WarsCoreAPI.getWPlayer(player))
                  player.setGameMode(GameMode.SURVIVAL)
                  cancel()
                } else {
                  player.sendActionBar(s"§bリスポーンするまであと§a$spawnTime§b秒")
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 1f, 2f)
                  spawnTime -= 1
                }
              } else {
                WarsCoreAPI.unfreeze(player)
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
    val spawn = mapInfo.locations.getOrElse("spawn", (0d, 0d, 0d, 0f, 0f))
    val red = mapInfo.locations.getOrElse("red", (0d, 0d, 0d, 0f, 0f))
    val blue = mapInfo.locations.getOrElse("blue", (0d, 0d, 0d, 0f, 0f))
    val a = mapInfo.locations.getOrElse("a", (0d, 0d, 0d, 0f, 0f))
    val b = mapInfo.locations.getOrElse("b", (0d, 0d, 0d, 0f, 0f))
    val c = mapInfo.locations.getOrElse("c", (0d, 0d, 0d, 0f, 0f))
    spawnPoint = new Location(world, spawn._1, spawn._2, spawn._3, spawn._4, spawn._5)
    captureData = Vector(
      new CapturePoint(ChatColor.RED + "拠点 赤軍", "red", 200, new Location(world, red._1, red._2, red._3, red._4, red._5)),
      new CapturePoint(ChatColor.BLUE + "拠点 青軍", "blue", 0, new Location(world, blue._1, blue._2, blue._3, blue._4, blue._5)),
      new CapturePoint(ChatColor.GRAY + "拠点 A", "a", 100, new Location(world, a._1, a._2, a._3, a._4, a._5)),
      new CapturePoint(ChatColor.GRAY + "拠点 B", "b", 100, new Location(world, b._1, b._2, b._3, b._4, b._5)),
      new CapturePoint(ChatColor.GRAY + "拠点 C", "c", 100, new Location(world, c._1, c._2, c._3, c._4, c._5))
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
      WarsCoreAPI.random.nextInt(1) match {
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
  private def occupy(point: CapturePoint, flag: Byte): Unit = {
    scoreboard.resetScores(point.name)
    val color =
    if (flag == 14) {
      point.team = "red"
      point.name = ChatColor.RED + ChatColor.stripColor(point.name)
      sendMessage(ChatColor.RED + "赤チーム" + ChatColor.WHITE + "が占拠")
      Color.RED
    } else if (flag == 11) {
      point.team = "blue"
      point.name = ChatColor.BLUE + ChatColor.stripColor(point.name)
      sendMessage(ChatColor.BLUE + "青チーム" + ChatColor.WHITE + "が占拠")
      Color.BLUE
    } else { // = 0
      point.team = ""
      point.name = ChatColor.GRAY + ChatColor.stripColor(point.name)
      sendMessage(s"占領が解除？")
      Color.WHITE
    }
    sidebar.getScore(point.name).setScore(100)

    val fwl = point.location.clone().add(0d, 3d, 0d)
    val fw = world.spawnEntity(fwl, EntityType.FIREWORK).asInstanceOf[Firework]
    val meta = fw.getFireworkMeta
    meta.addEffect(FireworkEffect.builder().withColor(color).`with`(FireworkEffect.Type.CREEPER).build())
    fw.setFireworkMeta(meta)
    val loc = point.location.clone().add(0, 1, 0)
    for (i <- -1 to 1) {
      for (j <- -1 to 1) {
        val block = world.getBlockAt(loc.getBlockX + i, loc.getBlockY, loc.getBlockZ + j)
        block.setType(Material.STAINED_GLASS)
        block.setData(flag)
      }
    }
  }

  private def canBuild(location: Location): Boolean = {
    captureData.exists(p => (location.getX >= p.location.getX - buildRange && location.getX <= p.location.getX + buildRange) &&
      (location.getY >= p.location.getY + 2 && location.getY <= p.location.getY + buildRange + 2) &&
      (location.getZ >= p.location.getZ - buildRange && location.getZ <= p.location.getZ + buildRange))
  }

  class CapturePoint(var name: String , var team: String, var count: Int, var location: Location)

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
