package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.utils.{EconomyUtil, MapInfo, RankManager, WorldLoader}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{BaseComponent, ComponentBuilder, HoverEvent}
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.{Arrow, EntityType, Firework, Player}
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Objective, Team}
import org.bukkit._
import org.bukkit.potion.PotionEffectType

import scala.collection.mutable

/**
 * 10vs10で行うゲームモード <br>
 * 1点 = 1キル<br>
 * 10分 or 50点先取で勝利<br>
 *
 * @author Emorard
 * @version 1.0
 */
class TeamDeathMatch(override val id: String) extends Game {

  /**
   * ゲームのタイトル
   */
  override val title: String = "チームデスマッチ"

  /**
   * ボスバー
   */
  val bossbar: BossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID)

  /**
   * ゲームの説明分
   */
  override val description: String = "2チームに分かれてポイント競います。"

  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = 20

  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _

  override var mapInfo: MapInfo = _

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

  WarsCoreAPI.setBaseTeam(redTeam);
  WarsCoreAPI.setBaseTeam(blueTeam)

  var redPoint = 0;
  var bluePoint = 0

  // TODO 余裕があればキュー機能を復活させる

  // スポーン, 赤チーム, 青チーム, 中心
  var locationData: (Location, Location, Location, Location) = _

  val data = mutable.HashMap.empty[Player, TDMData]

  /**
   * 中央を取ったチーム
   */
  private var center = "none"

  private var centerCount = 100

  private val buildRange = 9

  private val TIME = 600

  /**
   * ゲームを読み込む
   */
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
        if(!loaded) loaded = true
        // 無効化を解除
        if(disable) disable = false
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
    bossbar.setTitle(s"(TDM) ${mapInfo.mapName} §7|| §a-1")

    redTeam.getEntries.forEach(f => redTeam.removeEntry(f))
    blueTeam.getEntries.forEach(f => blueTeam.removeEntry(f))
    redPoint = 0
    bluePoint = 0

    center = "none"
    // 100にして考える(前回は50)
    centerCount = 100

    sidebar.getScore(ChatColor.RED + "赤チーム キル数:").setScore(0)
    sidebar.getScore(ChatColor.RED + "赤チーム 占領率:").setScore(0)
    sidebar.getScore(ChatColor.BLUE + "青チーム キル数:").setScore(0)
    sidebar.getScore(ChatColor.BLUE + "青チーム 占領率:").setScore(0)

    setLocationData()

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
            bossbar.setTitle(s"§fあと§a${count}§f秒で試合が始まります!")
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
    WarsCoreAPI.noticeStartGame(this)
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
      var time: Int = TIME

      override def run(): Unit = {
        if (
          members.length < 2 || // メンバーが一人
            redTeam.getSize <= 0 || // チーム0人
            blueTeam.getSize <= 0 || //
            time <= 0 || // 時間切れ
            disable // 何か原因があって無効化
        ) {
          // ゲーム強制終了
          end()
          cancel()
        } else {
          // それ以外
          if (time <= 5) members.map(_.player).foreach(f => f.playSound(f.getLocation, Sound.BLOCK_NOTE_HAT, 1f, 0f))
          if (time == 60) state = GameState.PLAY2
          if (center == "none") {
            locationData._4.getNearbyPlayers(3d, 6d).forEach(p => {
              if (redTeam.hasEntry(p.getName)) {
                centerCount += 1
                if (centerCount >= 200) {
                  occupy("red", ChatColor.RED + "赤チーム", Color.RED, 14.toByte)
                  redPoint += 20
                }
              } else {
                centerCount -= 1
                if (0 >= centerCount) {
                  occupy("blue", ChatColor.BLUE + "青チーム", Color.BLUE, 11.toByte)
                  bluePoint += 20
                }
              }
              members.map(_.player).foreach(_.sendActionBar(ChatColor.translateAlternateColorCodes('&',
                // 赤が優勢
                if (centerCount > 100) {
                  val score = (((centerCount - 100) * div) * 100.0).toInt
                  sidebar.getScore(ChatColor.RED + "赤チーム 占領率:").setScore(score)
                  s"&c赤チームが中央を占拠しています... &l$score%"

                } else if (100 > centerCount) {
                  val score = (((100 - centerCount) * div) * 100 ).toInt
                  sidebar.getScore(ChatColor.BLUE + "青チーム 占領率:").setScore(score)
                  s"&9青チームが中央を占拠しています... &l$score%"
                } else ""
              )))
            })
          }
          time -= 1
          val splitTime = WarsCoreAPI.splitToComponentTimes(time)
          bossbar.setProgress(time * 0.0016)
          bossbar.setTitle(s"(TDM) ${mapInfo.mapName} §7|| §a${splitTime._2} 分 ${splitTime._3} 秒")
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
    val winner = if (redPoint > bluePoint) "red" else if (redPoint < bluePoint) "blue" else "draw"
    val endMsg = new ComponentBuilder("- = - = - = - = - = - = - = - = - = - = - = -\n\n").color(ChatColor.GRAY).underlined(true)
      .append("             Game Over!\n").bold(true).underlined(false).color(ChatColor.WHITE)

    if(winner == "red") {
      endMsg.append("            ")
        .append("Red Team").color(ChatColor.RED).bold(false).underlined(true)
        .append(" won!                \n").color(ChatColor.WHITE).underlined(false)
    } else if (winner == "blue") {
      endMsg.append("            ")
        .append("Blue Team").color(ChatColor.BLUE).bold(false).underlined(true)
        .append(" won!                \n").color(ChatColor.WHITE).underlined(false)
    } else {
      endMsg.append("                ").reset()
        .append("Draw").underlined(true)
        .append("                \n").reset()
    }

    sendMessage(endMsg.create())

    // TODO Skriptに沿ってMVPを決定する
    // sendMessage("// TODO MVP作成")
    members.foreach(wp => {
      data.get(wp.player) match {
        case Some(d) =>
          if (winner == d.team) {
            //d.money += 500
            EconomyUtil.give(wp.player, EconomyUtil.COIN, 30)
            d.win = true
          }
          wp.sendMessage(createResult(d, winner):_*)
          // ゲーム情報のリセット
          wp.game = None
          // スコアボードのリセット
          wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
          RankManager.giveExp(wp, d.calcExp())
        case _ =>
      }
    })
    WarsCore.instance.database.updateTDMAsync(this)
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
    }.runTaskLater(WarsCore.instance, 100L)
  }


  /**
   * プレイヤーがゲームに参加するときのメソッド
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
      data.put(wp.player, new TDMData)
      bossbar.addPlayer(wp.player)
      members = members :+ wp
      sendMessage(s"§a${wp.player.getName}§fが参加しました (§a${members.length} §f/§a${maxMember}§f)")
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
          wp.player.teleport(locationData._1)
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


  /**
   * プレイヤーがゲームから抜けたときのメソッド
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
      if(wp.player.getGameMode == GameMode.SPECTATOR) wp.player.setGameMode(GameMode.SURVIVAL)
      // スコアボード情報をリセット
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
      wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
    }
  }


  /**
   * プレイヤーが死亡したときのイベント
   * @param e イベント
   */
  override def death(e: PlayerDeathEvent): Unit = {
    e.setCancelled(true)
    val victim = e.getEntity
    // 試合中のみのできごと
    if (state == GameState.PLAY || state == GameState.PLAY2) {
      val vData: TDMData = data(victim)
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
          if (redTeam.hasEntry(attacker.getName)) {
            redPoint += 1
            sidebar.getScore(ChatColor.RED + "赤チーム キル数:").setScore(redPoint)
            WarsCoreAPI.getAttackerWeaponName(attacker) match {
              case Some(name) =>
                sendMessage(s"§f0X §c${attacker.getName} §f[${name}§f] §7-> §0Killed §7-> §9${victim.getName}")
              case None =>
                sendMessage(s"§f0X §c${attacker.getName} §7-> §0Killed §7-> §9${victim.getName}")
            }
          // 青チーム用のメッセージ
          } else {
            bluePoint += 1
            sidebar.getScore(ChatColor.BLUE + "青チーム キル数:").setScore(bluePoint)
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
      case arrow: Arrow  =>
        arrow.getShooter match {
          case attacker: Player => d(attacker)
          case _ =>
        }
      case _ =>
    }
    vData.damaged += e.getFinalDamage
  }


  /**
   * deathメソッドで状態はPLAY or PLAY2
   * @param player
   */
  private def spawn(player: Player, coolTime: Boolean = false): Unit = {
    if (coolTime) player.setGameMode(GameMode.SPECTATOR)

    var spawnTime: Int = 5
    new BukkitRunnable {
      override def run(): Unit = {
        if (!coolTime) {
          if (redTeam.hasEntry(player.getName)) {
            player.teleport(locationData._2)
          } else {
            player.teleport(locationData._3)
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
                  if (redTeam.hasEntry(player.getName)) {
                    player.teleport(locationData._2)
                  } else {
                    player.teleport(locationData._3)
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


  /**
   * ブロックを破壊するときに呼び出されるイベント
   * @param e イベント
   */
  override def break(e: BlockBreakEvent): Unit = {
    if(!canBuild(e.getBlock.getLocation)) {
      e.getPlayer.sendActionBar(ChatColor.RED +  "そのブロックを壊すことはできません！")
      e.setCancelled(true)
    }
  }


  /**
   * ブロックを設置するときに呼び出されるイベント
   * @param e イベント
   */
  override def place(e: BlockPlaceEvent): Unit = {
    if(!canBuild(e.getBlock.getLocation)) {
      e.getPlayer.sendActionBar(ChatColor.RED + "その地点にブロックを置くことはできません！")
      e.setCancelled(true)
    }
  }


  private def setLocationData(): Unit = {
    val spawn = mapInfo.locations.getOrElse("spawn", (0d, 0d, 0d, 0f, 0f))
    val red = mapInfo.locations.getOrElse("red", (0d, 0d, 0d, 0f, 0f))
    val blue = mapInfo.locations.getOrElse("blue", (0d, 0d, 0d, 0f, 0f))
    val center = mapInfo.locations.getOrElse("center", (0d, 0d, 0d, 0f, 0f))
    locationData = (
      new Location(world, spawn._1, spawn._2, spawn._3, spawn._4, spawn._5), // Spawn
      new Location(world, red._1, red._2, red._3, red._4, red._5), // Red
      new Location(world, blue._1, blue._2, blue._3, blue._4, blue._5), // Blue
      new Location(world, center._1, center._2, center._3, center._4, center._5) // Center
    )
  }


  private def setTeam(p: Player): Unit = {
    if (state == GameState.PLAY) {
      if (checkHelp()) {
        //TODO CheckHelpの実装
      }
    }
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


  private def checkHelp(): Boolean = {
    // TODO チェック機能の作成
    false
  }


  private def occupy(team: String, prefix: String, color: Color, data: Byte): Unit = {
    if (center == "none") {
      center = team

      sendMessage(prefix + ChatColor.WHITE + "が中央を占拠し、" + ChatColor.GREEN + "20ポイント" + ChatColor.WHITE + "を獲得しました！")
      val fwl = locationData._4.clone().add(0d, 3d, 0d)
      val fw = world.spawnEntity(fwl, EntityType.FIREWORK).asInstanceOf[Firework]
      val meta = fw.getFireworkMeta
      meta.addEffect(FireworkEffect.builder().withColor(color).`with`(FireworkEffect.Type.CREEPER).build())
      fw.setFireworkMeta(meta)
      val loc = locationData._4.clone().add(0, 1, 0)
      for (i <- -1 to 1) {
        for (j <- -1 to 1) {
          val block = world.getBlockAt(loc.getBlockX + i, loc.getBlockY, loc.getBlockZ + j)
          block.setType(Material.STAINED_GLASS)
          block.setData(data)
        }
      }
      // プレイヤーへの報酬
      this.data.filter(pred => pred._2.team == team).keys.foreach(f => {
        EconomyUtil.give(f, EconomyUtil.COIN, 15)
        f.playSound(f.getLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        f.sendMessage(ChatColor.BLUE + "占領ボーナス！ 15コインを獲得しました")
      })
    }
  }


  private def canBuild(location: Location): Boolean = {
    val center = locationData._4
    (location.getX >= center.getX - buildRange && location.getX <= center.getX + buildRange) &&
      (location.getY >= center.getY + 2 && location.getY <= center.getY + buildRange*2) &&
      (location.getZ >= center.getZ - buildRange && location.getZ <= center.getZ + buildRange)
  }


  private val helpTeamPoint = new ComponentBuilder("各チームの獲得ポイントです").color(ChatColor.GREEN).create()


  private def createResult(data: TDMData, winner: String): Array[BaseComponent] = {
    val comp = new ComponentBuilder("- = - = - = - = - = ").color(ChatColor.GRAY).underlined(true)
      .append("戦績").underlined(false).bold(true).color(ChatColor.AQUA)
      .append("- = - = - = - = - = \n\n").underlined(true).bold(false).color(ChatColor.GRAY)
      .append("* ").underlined(false).color(ChatColor.WHITE)
      .append("結果: ").color(ChatColor.GRAY)

    if(winner == "draw") {
      comp.append("引き分け\n")
    } else if (winner == data.team) {
      comp.append("勝利").color(ChatColor.YELLOW).bold(true).append("\n").bold(false)
    } else {
      comp.append("敗北").color(ChatColor.BLUE).append("\n")
    }

    comp.append("* ").color(ChatColor.WHITE).append("赤チーム: ").color(ChatColor.RED)
      .append(redPoint.toString).color(ChatColor.GREEN).bold(true)
      .append("     |     ").color(ChatColor.GRAY).bold(false)
      .append("青チーム: ").color(ChatColor.BLUE)
      .append(bluePoint.toString).color(ChatColor.GREEN).bold(true)
      .append("        [?]").bold(false).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, helpTeamPoint))
      .append("\n").color(ChatColor.RESET)

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
      .append((data.kill / (data.death + 1)).toString).color(ChatColor.GREEN).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)
/*
    comp.append("* ")
      .append("アシスト: ").color(ChatColor.GRAY)
      .append(data.assist.toString).color(ChatColor.GREEN).bold(true)
      .append("\n").color(ChatColor.RESET).bold(false)
*/
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

  /**
   * 試合中の一時的なデータを管理するクラス
   */
  class TDMData {
    // 順に, キル, デス, アシスト, ダメージ量, 受けたダメージ量
    var kill, death, assist: Int = 0
    var damage, damaged: Double = 0d
    var win = false
    var damagedPlayer = mutable.Set.empty[Player]
    var team = ""

    def calcExp(): Int = {
      kill * 5 + death + assist + (if(win) 100 else 0)
    }
  }
}
