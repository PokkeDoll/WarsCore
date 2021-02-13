package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.events.GameEndEvent
import hm.moe.pokkedoll.warscore.utils.{GameConfig, MapInfo, WeakLocation, WorldLoader}
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.Player
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit._

/**
 * 1vs1 で行うゲームモード<br>
 * 1点 = キル<br>
 * 5本中3点先取で勝利となる<br>
 * あまり重要ではないので新機能はどんどん入れていく
 *
 * @version 2.0
 * @author Emorard
 */
class Tactics(override val id: String) extends Game {
  /**
   * 読み込むワールドのID.  最初は必ず0
   */
  var worldId: String = s"$id-0"

  /**
   * ゲームの構成
   */
  override val config: GameConfig = GameConfig.getConfig("tactics")

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

  var first: Int = 0
  var second: Int = 0

  var locationData: (Location, Location, Location) = _

  var turn: Int = 1

  var stop = false

  private def setLocationData(): Unit = {
    val spawn = mapInfo.locations.getOrElse("spawn", WeakLocation.empty)
    val red = mapInfo.locations.getOrElse("red", WeakLocation.empty)
    val blue = mapInfo.locations.getOrElse("blue", WeakLocation.empty)
    locationData = (
      spawn.getLocation(world),
      red.getLocation(world),
      blue.getLocation(world))
  }

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

  override def init(): Unit = {
    state = GameState.INIT

    // getPlayer().clear()よりよいはず
    bossbar.removeAll()
    bossbar.setProgress(1d)
    bossbar.setTitle(s"${mapInfo.mapName} §7|| §a-1")

    first = 0
    second = 0

    setLocationData()

    members = Vector.empty[WPlayer]

    world.setPVP(false)

    // 実に待機状態...! Joinされるまで待つ
    state = GameState.WAIT
  }

  override def ready(): Unit = {
    sendMessage("§a10秒後に試合を始めます！")
    new BukkitRunnable {
      override def run(): Unit = {
        play()
      }
    }.runTaskLater(WarsCore.instance, 200L)
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
      s"${mapInfo.mapName} §7|| §a$turn §7|| §a${members.head.player.getName}: $first §f: §a${members.last.player.getName}: $second"
    )
    new BukkitRunnable {
      override def run(): Unit = {
        if (members.length < 2 || disable) {
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
    if (first >= 3 || second >= 3) {
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
    Bukkit.getPluginManager.callEvent(new GameEndEvent(this, winner))
    sendMessage(
      "§7==========================================\n" +
        "§7                Game Over!                \n" +
        s"              §e$winner §7won!                \n" +
        "§7==========================================\n" +
        ChatColor.of("#000080") + "(> 1.16) Navy Color"
    )
    members.foreach(wp => {
      if (wp.player.getGameMode == GameMode.SPECTATOR) {
        wp.player.setGameMode(GameMode.SURVIVAL)
        WarsCoreAPI.unfreeze(wp.player)
      }
      wp.game = None
      WarsCoreAPI.restoreLobbyInventory(wp.player)
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
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

  override def join(wp: WPlayer): Boolean = {
    if (wp.game.isDefined) {
      wp.sendMessage("ほかのゲームに参加しています!")
      false
    } else if (!loaded && state == GameState.DISABLE) {
      load(Vector(wp.player))
      false
    } else if (!state.join) {
      wp.player.sendMessage("§cゲームに参加できません!")
      false
    } else if (members.length >= maxMember) {
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
      wp.player.sendMessage("テスト: インベントリをクリアしてWPを獲得")
      WarsCoreAPI.changeWeaponInventory(wp)

      wp.player.setScoreboard(WarsCoreAPI.scoreboard)
      bossbar.addPlayer(wp.player)
      members = members :+ wp
      sendMessage(s"§a${wp.player.getName}§fが参加しました (§a${members.length} §f/§a$maxMember§f)")
      state match {
        case GameState.WAIT =>
          wp.player.teleport(locationData._1)
          if (members.length >= 2) {
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
    if (wp.player.isOnline) {
      // スコアボードをリセット
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
      wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
      WarsCoreAPI.restoreLobbyInventory(wp.player)
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
          WarsCore.instance.database.addItem(
            attacker.getUniqueId.toString,
            config.onKillItem:_*
          )
          sendMessage(s"${attacker.getName}が1ポイント獲得しました")
          e.setShouldPlayDeathSound(true)
          e.setDeathSound(Sound.ENTITY_PLAYER_LEVELUP)
          e.setDeathSoundVolume(2f)
          WarsCoreAPI.getAttackerWeaponName(attacker) match {
            case Some(name) =>
              sendMessage(s"§f0X §c${attacker.getName} §f[$name§f] §7-> §0Killed §7-> §9${victim.getName}")
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
          if (player.getGameMode != GameMode.SPECTATOR) {
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
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 2f)
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

  /**
   * ブロックを破壊するときに呼び出されるイベント
   *
   * @param e BlockBreakEvent
   */
  override def break(e: BlockBreakEvent): Unit = {}

  /**
   * ブロックを設置するときに呼び出されるイベント
   *
   * @param e BlockPlaceEvent
   */
  override def place(e: BlockPlaceEvent): Unit = {}

  /**
   * プレイヤーがダメージを受けた時のイベント
   *
   * @param e EntityDamageByEntityEvent
   */
  override def damage(e: EntityDamageByEntityEvent): Unit = {}
}
