package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{MapInfo, WorldLoader}
import org.bukkit.{Bukkit, GameMode, Location, Sound, World, scheduler}
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.{PotionEffect, PotionEffectType}
import org.bukkit.scheduler.BukkitRunnable

/**
 * 1vs1 で行うゲームモード<br>
 * 1点 = キル<br>
 * 5本中3点先取で勝利となる<br>
 *
 * @author Emorard
 */
class Tactics(override val id: String) extends Game {
  /**
   * ゲームのタイトル
   */
  override val title: String = "タクティクス"
  /**
   * ボスバー
   */
  override val bossbar: BossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID)
  /**
   * ゲームの説明分
   */
  override val description: String = "1 vs 1で行うゲームモード"
  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = 2
  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _
  override var mapInfo: MapInfo = _
  /**
   * 試合時間
   */
  override val time: Int = -1

  var first: Int = 0;
  var second: Int = 0

  var locationData: (Location, Location, Location) = _

  var turn: Int = 1

  var stop = false

  private def setLocationData(): Unit = {
    val spawn = mapInfo.locations.getOrElse("spawn", (0d, 0d, 0d, 0f, 0f))
    val red = mapInfo.locations.getOrElse("red", (0d, 0d, 0d, 0f, 0f))
    val blue = mapInfo.locations.getOrElse("blue", (0d, 0d, 0d, 0f, 0f))
    locationData = (new Location(world, spawn._1, spawn._2, spawn._3, spawn._4, spawn._5), new Location(world, red._1, red._2, red._3, red._4, red._5), new Location(world, blue._1, blue._2, blue._3, blue._4, blue._5))
  }

  override def load(): Unit = {
    state = GameState.INIT
    val worlds = WarsCoreAPI.mapinfo.filter(_.gameId == "tdm")
    val info = scala.util.Random.shuffle(worlds).head
    WorldLoader.syncLoadWorld(s"worlds/${info.mapId}", id) match {
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

  override def init(): Unit = {
    if (state != GameState.INIT) state = GameState.INIT

    // getPlayer().clear()よりよいはず
    bossbar.removeAll()
    bossbar.setProgress(1d)
    bossbar.setTitle(s"(TDM) ${mapInfo.mapName} §7|| §a-1")

    first = 0;
    second = 0

    setLocationData()

    members = Vector.empty[WPlayer]

    world.setPVP(false)

    // 実に待機状態...! Joinされるまで待つ
    state = GameState.WAIT
  }

  override def ready(): Unit = {
    sendMessage("§a5秒後に試合を始めます！")
    new BukkitRunnable {
      override def run(): Unit = {
        play()
      }
    }.runTaskLater(WarsCore.instance, 100L)
  }

  override def play(): Unit = {
    members.foreach(f => f.player.sendTitle("§7Start!", "- §6Tactics §f- Kill the enemy!", 30, 20, 20))
    turnPlay()
    spawn()
  }

  /**
   * ターンごとのplay
   */
  private def turnPlay(): Unit = {
    state = GameState.PLAY2
    world.setPVP(true)
    bossbar.setTitle(
      s"(Tactics) ${mapInfo.mapName} §7|| §a$turn §7|| §a${members.head.player.getName}: $first §f: §a${members.last.player.getName}: $second"
    )
    new BukkitRunnable {
      override def run(): Unit = {
        if(members.length < 2 || disable) {
          end()
          cancel()
        } else if (stop) {
          turnEnd()
          cancel()
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  /**
   * ターンごとのend
   */
  private def turnEnd(): Unit = {
    world.setPVP(false)
    state = GameState.END
    if (first >= 3 | second >= 3) {
      end()
    } else {
      turn += 1
      stop = false
      turnPlay()
      spawn()
    }
  }

  override def end(): Unit = {
    state = GameState.END
    world.setPVP(false)
    // ここで勝敗を決める
    val winner = if (first > second) members.head.player.getName else members.last.player.getName
    sendMessage(
      "§7==========================================\n" +
        "§7                Game Over!                \n" +
        s"              §e$winner §7won!                \n" +
        "§7==========================================\n" +
        "§95秒後にロビーに戻ります..."
    )
    members.foreach(wp => {
      if(wp.player.getGameMode == GameMode.SPECTATOR) {
        wp.player.setGameMode(GameMode.SURVIVAL)
        WarsCoreAPI.unfreeze(wp.player)
      }
      wp.game = None
    })
    /* ワールド削除処理 */
    new BukkitRunnable {
      override def run(): Unit = {
        bossbar.removeAll()
        delete()
      }
    }.runTaskLater(WarsCore.instance, 100L)
  }

  override def delete(): Unit = {
    WorldLoader.syncUnloadWorld(id)
    new BukkitRunnable {
      override def run(): Unit = {
        load()
      }
    }.runTaskLater(WarsCore.instance, 100L)
  }

  override def join(wp: WPlayer): Boolean = {
    if(wp.game.isDefined) {
      wp.sendMessage("ほかのゲームに参加しています!")
      false
    } else if(!loaded && state == GameState.DISABLE) {
      load()
      false
    } else if(!state.join) {
      wp.player.sendMessage("§cゲームに参加できません!")
      false
    } else if(members.length >= maxMember) {
      wp.player.sendMessage("§c人数が満員なので参加できません！")
      false
    } else {
      wp.sendMessage(
        s"マップ名: §a${mapInfo.mapName}\n" +
          s"製作者: §a${mapInfo.authors}\n" +
          s"退出する場合は§a/hub§もしくは§a/game leave\n" +
          "§a/invite <player>§fで他プレイヤーを招待することができます!"
      )
      wp.game = Some(this)
      bossbar.addPlayer(wp.player)
      members = members :+ wp
      sendMessage(s"§a${wp.player.getName}§fが参加しました (§a${members.length} §f/§a${maxMember}§f)")
      state match {
        case GameState.WAIT =>
          wp.player.teleport(locationData._1)
          if(members.length >= 2) {
            ready()
          }
        case _ =>
          return false
      }
      true
    }
  }

  override def hub(wp: WPlayer): Unit = {
    // メンバーから削除
    members = members.filterNot(_ eq wp)
    // ボスバーから削除
    bossbar.removePlayer(wp.player)
    // ゲーム情報をリセット
    wp.game = None
    sendMessage(s"${wp.player.getName} が退出しました")
    if(wp.player.isOnline) {
      wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
    }
  }

  override def death(e: PlayerDeathEvent): Unit = {
    e.setCancelled(true)
    val victim = e.getEntity
    // 試合中のみのできごと
    if (!stop && state == GameState.PLAY2) {
      stop = true
      // WarsCoreAPI.getAttacker(victim.getLastDamageCause)
      Option(victim.getKiller) match {
        case Some(attacker) =>
          if (members.head.player == victim) {
            second += 1
          } else {
            first += 1
          }
          sendMessage(s"${attacker.getName}が1ポイント獲得しました")
          e.setShouldPlayDeathSound(true)
          e.setDeathSound(Sound.ENTITY_PLAYER_LEVELUP)
          e.setDeathSoundVolume(2f)
          WarsCoreAPI.getAttackerWeaponName(attacker) match {
            case Some(name) =>
              sendMessage(s"§f0X §c${attacker.getName} §f[${name}§f] §7-> §0Killed §7-> §9${victim.getName}")
            case None =>
              sendMessage(s"§f0X §c${attacker.getName} §7-> §0Killed §7-> §9${victim.getName}")
          }
        case None =>
          sendMessage(s"§f0X ${victim.getName} dead")
          if (members.head.player == victim) {
            first -= 1
          } else {
            second -= 1
          }
          sendMessage(s"${victim.getName}は1ポイント没収されました")
      }
      teleport(victim)
    }
  }

  private def teleport(player: Player): Unit = {
    player.setGameMode(GameMode.SPECTATOR)
    new BukkitRunnable {
      override def run(): Unit = {
        if (members.head.player == player) {
          player.teleport(locationData._2)
        } else {
          player.teleport(locationData._3)
        }
        WarsCoreAPI.freeze(player)
      }
    }.runTaskLater(WarsCore.instance, 1L)
  }

  private def spawn(): Unit = {
    var spawnTime = 5
    new BukkitRunnable {
      override def run(): Unit = {
        members.foreach(wp => {
          val player = wp.player
          if(player.getGameMode != GameMode.SPECTATOR) {
            if (members.head == wp) {
              player.teleport(locationData._2)
            } else {
              player.teleport(locationData._3)
            }
            WarsCoreAPI.freeze(player)
          }
          new BukkitRunnable {
            override def run(): Unit = {
              if (state == GameState.PLAY || state == GameState.PLAY2) {
                if (0 >= spawnTime) {
                  WarsCoreAPI.unfreeze(player)
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
        })
      }
    }.runTaskLater(WarsCore.instance, 1L)
  }
}
