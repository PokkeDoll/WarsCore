package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.events.{GameDeathEvent, GameEndEvent, GameJoinEvent, GameStartEvent}
import hm.moe.pokkedoll.warscore.utils._
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{BaseComponent, ComponentBuilder, HoverEvent}
import org.bukkit._
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.{Arrow, Entity, Player}
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.{DisplaySlot, Objective, Team}

import scala.collection.mutable

/**
 * 4vs4で行うゲームモード <br>
 * version2.0では 10 vs 10というわけではない。<br>
 * version3.0ではコールバックシステムに対応。 <br>
 * 1点 = 1キル<br>
 * 10分 or 50点先取で勝利<br>
 *
 * @author Emorard
 * @version 3.0
 */
class TeamDeathMatch4(override val id: String) extends Game {
  /**
   * 読み込むワールドのID.  最初は必ず0
   */
  override var worldId: String = s"$id"

  /**
   * ゲームの構成
   */
  override val config: GameConfig = GameConfig.getConfig("tdm4")

  /**
   * ゲームのタイトル
   */
  override val title: String = "チームデスマッチ4"

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
  override val maxMember: Int = 8

  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _

  override var mapInfo: MapInfo = _

  override val time: Int = 600

  private val scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

  val sidebar: Objective = scoreboard.registerNewObjective("sidebar", "dummy")
  sidebar.setDisplayName(ChatColor.GOLD + title)
  sidebar.setDisplaySlot(DisplaySlot.SIDEBAR)

  var redTeam: Team = scoreboard.registerNewTeam(s"$id-red")
  redTeam.setColor(org.bukkit.ChatColor.RED)
  redTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)


  val blueTeam: Team = scoreboard.registerNewTeam(s"$id-blue")
  blueTeam.setColor(org.bukkit.ChatColor.BLUE)
  blueTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)

  WarsCoreAPI.setBaseTeam(redTeam)
  WarsCoreAPI.setBaseTeam(blueTeam)

  var redPoint = 0
  var bluePoint = 0

  // TODO 余裕があればキュー機能を復活させる

  // スポーン, 赤チーム, 青チーム, 中心
  var locationData: (Location, Location, Location, Location) = _

  val data = mutable.HashMap.empty[Player, TDMData]

  private val TIME = 600

  /**
   * ゲームを初期化する
   */
  override def init(): Unit = {
    state = GameState.INIT

    bossbar.removeAll()
    bossbar.setProgress(1d)
    bossbar.setTitle(s"${mapInfo.mapName} §7|| §a-1")

    redTeam.getEntries.forEach(f => redTeam.removeEntry(f))
    blueTeam.getEntries.forEach(f => blueTeam.removeEntry(f))
    redPoint = 0
    bluePoint = 0

    sidebar.getScore(ChatColor.WHITE + "Room: " + ChatColor.GOLD + id).setScore(99)
    sidebar.getScore("").setScore(98)
    sidebar.getScore(ChatColor.RED + "赤チーム キル数:").setScore(0)
    sidebar.getScore(ChatColor.BLUE + "青チーム キル数:").setScore(0)

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
        } else if(members.map(_.player).forall(_.isSneaking)) {
          count = 0
          sendMessage(ChatColor.BLUE + "カウントをスキップします...")
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
          if (time <= 5) members.map(_.player).foreach(f => f.playSound(f.getLocation, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0f))
          if (time == 60) state = GameState.PLAY2
          time -= 1
          val splitTime = WarsCoreAPI.splitToComponentTimes(time)
          bossbar.setProgress(time * 0.0016)
          bossbar.setTitle(s"${mapInfo.mapName} §7|| §a${splitTime._2} 分 ${splitTime._3} 秒")
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }


  /**
   * ゲームを終了する
   */
  // TODO addResultの追加
  override def end(): Unit = {
    state = GameState.END
    world.setPVP(false)
    // ここで勝敗を決める
    val winner = if (redPoint > bluePoint) "red" else if (redPoint < bluePoint) "blue" else "draw"

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
        .append("Draw").underlined(true)
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
          // インベントリのリストア
          WarsCoreAPI.restoreLobbyInventory(wp.player)
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
      load(players = Vector(wp.player))
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
          "&a/invite <player>&fで他プレイヤーを招待することができます!")
      wp.game = Some(this)
      // インベントリを変更
      WarsCoreAPI.changeWeaponInventory(wp)

      wp.player.setScoreboard(scoreboard)
      data.put(wp.player, new TDMData)
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
          wp.player.teleport(locationData._1)
        case GameState.WAIT =>
          wp.player.teleport(locationData._1)
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
      val preEvent = (attacker: Player) => new GameDeathEvent(attacker = attacker, victim = victim, game = this)
      val vData: TDMData = data(victim)
      vData.death += 1
      Option(victim.getKiller) match {
        case Some(attacker) =>
          val aData = data(attacker)
          aData.kill += 1
          /*
          // 同じIPアドレスなら報酬をスキップする
          if (attacker.isOp || victim.getAddress != attacker.getAddress) {
            EconomyUtil.give(attacker, EconomyUtil.COIN, 3)
          }
           */

          reward(attacker, GameRewardType.KILL)

          WarsCoreAPI.debug(attacker, "アイテムを獲得 > '/wc storage' or エンダーチェストで確認")
          e.setShouldPlayDeathSound(true)
          e.setDeathSound(Sound.ENTITY_PLAYER_LEVELUP)
          e.setDeathSoundVolume(2f)
          // 赤チーム用のメッセージ
          if (redTeam.hasEntry(attacker.getName)) {
            redPoint += 1
            sidebar.getScore(ChatColor.RED + "赤チーム キル数:").setScore(redPoint)
            WarsCoreAPI.getAttackerWeaponName(attacker) match {
              case Some(name) =>
                sendMessage(s"§f0X §c${attacker.getName} §f[$name§f] §7-> §0Killed §7-> §9${victim.getName}")
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
          Bukkit.getServer.getPluginManager.callEvent(preEvent(attacker))
        case None =>
          sendMessage(s"§f0X ${victim.getName} dead")
          Bukkit.getServer.getPluginManager.callEvent(preEvent(null))
      }
      val item = victim.getInventory.getItemInMainHand
      if(item != null && item.getType != Material.AIR && WarsCore.instance.getCSUtility.getWeaponTitle(item) != null) {
        val dropItem = world.dropItem(victim.getLocation(), item)
        // 30秒で消滅するように
        dropItem.setWillAge(true)
        dropItem.asInstanceOf[Entity].setTicksLived(5400)
      }
      // とにかく死んだのでリスポン処理
      spawn(victim, coolTime = true)
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
   * deathメソッドで状態はPLAY or PLAY2
   *
   * @param player Player
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
                  WarsCoreAPI.setActiveWeapons(player)
                  if (redTeam.hasEntry(player.getName)) {
                    player.teleport(locationData._2)
                  } else {
                    player.teleport(locationData._3)
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


  /**
   * ブロックを破壊するときに呼び出されるイベント
   *
   * @param e イベント
   */
  override def break(e: BlockBreakEvent): Unit = {
    e.setCancelled(true)
  }


  /**
   * ブロックを設置するときに呼び出されるイベント
   *
   * @param e イベント
   */
  override def place(e: BlockPlaceEvent): Unit = {
    e.setCancelled(true)
  }


  private def setLocationData(): Unit = {
    val spawn = mapInfo.locations.getOrElse("spawn", WeakLocation.empty)
    val red = mapInfo.locations.getOrElse("red", WeakLocation.empty)
    val blue = mapInfo.locations.getOrElse("blue", WeakLocation.empty)
    val center = mapInfo.locations.getOrElse("center", WeakLocation.empty)
    locationData = (
      spawn.getLocation(world),
      red.getLocation(world),
      blue.getLocation(world),
      center.getLocation(world)
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

  private val helpTeamPoint = new ComponentBuilder("各チームの獲得ポイントです").color(ChatColor.GREEN).create()


  private def createResult(data: TDMData, winner: String): Array[BaseComponent] = {
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
      .append((data.kill / (if(data.death == 0) 1 else data.death).toDouble).toString).color(ChatColor.GREEN).bold(true)
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
   * Java用メソッド。Optionalではないためnullの可能性がある。
   * @param player データを持つプレイヤー。
   * @return 現時点のTDMのデータ。存在しないならnull
   */
  def getDataUnsafe(player: Player): TDMData = data(player)

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
      kill * 5 + death + assist + (if (win) 100 else 0)
    }
  }
}
