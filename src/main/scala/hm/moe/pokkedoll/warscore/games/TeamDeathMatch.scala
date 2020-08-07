package hm.moe.pokkedoll.warscore.games
import java.util

import hm.moe.pokkedoll.warscore.utils.{MapInfo, WorldLoader}
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import org.bukkit.boss.{BarColor, BarStyle, BossBar}
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.{PotionEffect, PotionEffectType}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Team
import org.bukkit.{Bukkit, ChatColor, GameMode, Location, Sound, World, scheduler}

import scala.collection.mutable

/**
 * 8vs8で行うゲームモード <br>
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
  override val description: String = "説明不要！とにかく敵を倒そう"
  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = 10
  /**
   * ワールド。でも使うかわからん...
   */
  override var world: World = _

  override var mapInfo: MapInfo = _

  override val time: Int = 600

  var redTeam: Team = WarsCoreAPI.scoreboard.registerNewTeam(s"$id-red")
  redTeam.setColor(ChatColor.RED)

  val blueTeam: Team = WarsCoreAPI.scoreboard.registerNewTeam(s"$id-blue")
  blueTeam.setColor(ChatColor.BLUE)

  WarsCoreAPI.setBaseTeam(redTeam); WarsCoreAPI.setBaseTeam(blueTeam)

  var redPoint = 0; var bluePoint = 0

  // TODO 余裕があればキュー機能を復活させる

  var locationData: (Location, Location, Location) = _

  val data = mutable.HashMap.empty[Player, TDMData]

  private def setLocationData(): Unit = {
    val spawn = mapInfo.locations.getOrElse("spawn", (0d,0d,0d,0f,0f))
    val red = mapInfo.locations.getOrElse("red", (0d,0d,0d,0f,0f))
    val blue = mapInfo.locations.getOrElse("blue", (0d,0d,0d,0f,0f))
    locationData = (new Location(world, spawn._1, spawn._2, spawn._3, spawn._4, spawn._5), new Location(world, red._1, red._2, red._3, red._4, red._5), new Location(world, blue._1, blue._2, blue._3, blue._4, blue._5))
  }

  @Deprecated
  private def getAttackerWeaponName(player: Player): Option[String] = {
    val item = player.getInventory.getItemInMainHand
    if(item == null) None
    else {
      val meta = item.getItemMeta
      if(meta.hasDisplayName) Some(meta.getDisplayName) else Some(item.getType.toString)
    }
  }

  private def setTeam(p: Player): Unit = {
    if(state == GameState.PLAY) {
      if(checkHelp()) {

      }
    }
    if(redTeam.getEntries.size() > blueTeam.getEntries.size()) {
      blueTeam.addEntry(p.getName)
    } else if(redTeam.getEntries.size() < blueTeam.getEntries.size()) {
      redTeam.addEntry(p.getName)
    } else {
      WarsCoreAPI.random.nextInt(1) match {
        case 1 =>
          blueTeam.addEntry(p.getName)
        case 0 =>
          redTeam.addEntry(p.getName)
      }
    }
  }

  private def checkHelp(): Boolean = {
    // TODO チェック機能の作成
    false
  }

  private val TIME = 600

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

  override def init(): Unit = {
    if(state!=GameState.INIT) state = GameState.INIT

    // getPlayer().clear()よりよいはず
    bossbar.removeAll()
    bossbar.setProgress(1d)
    bossbar.setTitle(s"(TDM) ${mapInfo.mapName} §7|| §a-1")

    redTeam.getEntries.forEach(f => redTeam.removeEntry(f))
    blueTeam.getEntries.forEach(f => blueTeam.removeEntry(f))
    redPoint = 0; bluePoint = 0

    setLocationData()

    data.clear()

    members = Vector.empty[WPlayer]

    world.setPVP(false)

    // 実に待機状態...! Joinされるまで待つ
    state = GameState.WAIT
  }

  override def ready(): Unit = {
    state = GameState.READY
    new BukkitRunnable {
      val removeProgress: Double = 1d/40d
      var count = 40
      override def run(): Unit = {
        if(members.length < 2) {
          sendMessage("§c人数が足りないため待機状態に戻ります")
          state = GameState.WAIT
          cancel()
        } else if (count <= 0) {
          play()
          cancel()
        } else {
          bossbar.setTitle(s"§fあと§a${count}§f秒で試合が始まります!")
          bossbar.setProgress(bossbar.getProgress - removeProgress)
          count -= 1
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }


  override def play(): Unit = {
    state = GameState.PLAY
    world.setPVP(true)
    // チーム決め + 移動
    members.map(_.player).foreach(p => {
      setTeam(p)
      spawn(p)
      if(redTeam.hasEntry(p.getName)) {
        p.sendTitle("§7YOU ARE §cRED §7TEAM!", "- §6TDM §f- Kill §9Blue §fteam!", 30, 20, 20)
      } else {
        p.sendTitle("§7YOU ARE §9BLUE §7TEAM!", "- §6TDM §f- Kill §cRed §fteam!", 30, 20, 20)
      }
    })
    new BukkitRunnable {
      var time: Int = TIME
      override def run(): Unit = {
        if(
          members.length < 2 ||     // メンバーが一人
          redTeam.getSize <= 0 ||   // チーム0人
          blueTeam.getSize <= 0 ||  //
          time <= 0 ||              // 時間切れ
          disable                   // 何か原因があって無効化
        ) {
          // ゲーム強制終了
          end()
          cancel()
        } else {
          // それ以外
          if(time <= 5) members.map(_.player).foreach(f => f.playSound(f.getLocation, Sound.BLOCK_NOTE_HAT, 1f, 0f))
          if(time == 60) state = GameState.PLAY2
          time -= 1
          bossbar.setProgress(time * 0.0016)
          bossbar.setTitle(s"(TDM) ${mapInfo.mapName} §7|| §a$time")
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 20L)
  }

  override def end(): Unit = {
    state = GameState.END
    world.setPVP(false)
    // ここで勝敗を決める
    val winner = if(redPoint > bluePoint) "red" else if (redPoint < bluePoint) "blue" else "draw"
    sendMessage(
      "§7==========================================\n" +
      "§7                Game Over!                \n" +
      (
        if(winner=="red") "              §cRed Team §7won!                \n"
        else if (winner=="blue") "              §9Blue Team §7won!                \n"
        else "                §fDraw                \n"
      ) +
      "§7==========================================\n"
    )
    // TODO Skriptに沿ってMVPを決定する
    sendMessage("// TODO MVP作成")
    members.foreach(wp => {
      data.get(wp.player) match {
        case Some(d) =>
          if((winner=="red" && redTeam.hasEntry(wp.player.getName)) && (winner=="blue" && blueTeam.hasEntry(wp.player.getName))) {
            d.money += 500
            wp.win += 1
          }
          // 統計処理
          wp.kill += d.kill
          wp.death += d.death
          wp.assist += d.assist
          wp.damage += d.damage
          wp.sendMessage(
            "§7= = = = = = = = &b戦績 §7= = = = = = = =\n" +
            s"* §cRed §7Team: &a${redPoint} §7| §9Blue Team: &a${bluePoint}\n" +
            s"* §7Kill: &a${d.kill}\n" +
            s"* §7Death: &a%{game::death::%loop-value%}%\n" +
            s"* §7K/D: &a%{game::kill::%loop-value%} / {game::death::%loop-value%}%\n" +
            s"* §7Money: &a%{game::money::%loop-value%}%\n" +
            "§b5秒後にロビーに戻ります..."
          )
          // ゲーム情報のリセット
          wp.game = None
          // スコアボードのリセット
          wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
        case _ =>
      }
    })
    bossbar.removeAll()
    new BukkitRunnable {
      override def run(): Unit = {
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

  override def death(e: PlayerDeathEvent): Unit = {
    e.setCancelled(true)
    val victim = e.getEntity
    // 試合中のみのできごと
    if(state == GameState.PLAY || state == GameState.PLAY2) {
      val vData: TDMData = data(victim)
      vData.death += 1
      // WarsCoreAPI.getAttacker(victim.getLastDamageCause)
       Option(victim.getKiller) match {
        case Some(attacker) =>
          val aData = data(attacker)
          aData.kill += 1
          // 同じIPアドレスなら報酬をスキップする
          if(!attacker.isOp && victim.getAddress != attacker.getAddress) aData.money += 50
          e.setShouldPlayDeathSound(true)
          e.setDeathSound(Sound.ENTITY_PLAYER_LEVELUP)
          e.setDeathSoundVolume(2f)
          if(redTeam.hasEntry(attacker.getName)) {
            redPoint += 1
            WarsCoreAPI.getAttackerWeaponName(attacker) match {
              case Some(name) =>
                sendMessage(s"§f0X §c${attacker.getName} §f[${name}§f] §7-> §0Killed §7-> §9${victim.getName}")
              case None =>
                sendMessage(s"§f0X §c${attacker.getName} §7-> §0Killed §7-> §9${victim.getName}")
            }
          } else {
            bluePoint += 1
            WarsCoreAPI.getAttackerWeaponName(attacker) match {
              case Some(name) =>
                sendMessage(s"§f0X §9${attacker.getName} §f[${name}§f] §7-> §0Killed §7-> §c${victim.getName}")
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
   * deathメソッドで状態はPLAY or PLAY2
   * @param player
   */
  private def spawn(player: Player, coolTime: Boolean = false): Unit = {
    if(coolTime) player.setGameMode(GameMode.SPECTATOR)
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 5), true)
    var spawnTime: Int = 5
    new BukkitRunnable {
      override def run(): Unit = {
        if(redTeam.hasEntry(player.getName)) {
          player.teleport(locationData._2)
        } else {
          player.teleport(locationData._3)
        }
        if(coolTime) {
          WarsCoreAPI.freeze(player)
          new BukkitRunnable {
            override def run(): Unit = {
              if(state == GameState.PLAY || state == GameState.PLAY2) {
                if(0 >= spawnTime) {
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
        }
      }
    }.runTaskLater(WarsCore.instance, 1L)
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
      wp.player.setScoreboard(WarsCoreAPI.scoreboard)
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
              if(redTeam.hasEntry(wp.player.getName)) {
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
    val team = WarsCoreAPI.scoreboard.getEntryTeam(wp.player.getName)
    if(team != null) {
      // チームから削除
      team.removeEntry(wp.player.getName)
      // メンバーから削除
      members = members.filterNot(_ eq wp)
      // ボスバーから削除
      bossbar.removePlayer(wp.player)
      // ゲーム情報をリセット
      wp.game = None
      // スコアボード情報をリセット
      wp.player.setScoreboard(WarsCoreAPI.scoreboards(wp.player))
      sendMessage(s"${wp.player.getName} が退出しました")
      if(wp.player.isOnline) {
        wp.player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
      }
    }
  }

  class TDMData {
    // 順に, 金！, キル, デス, アシスト, ダメージ量, 受けたダメージ量
    var money, kill, death, assist, damage: Int = 0
    var damaged = mutable.Set.empty[Player]
  }
}
