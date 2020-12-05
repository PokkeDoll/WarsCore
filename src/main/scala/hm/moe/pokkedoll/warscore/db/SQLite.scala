package hm.moe.pokkedoll.warscore.db

import java.sql.SQLException
import java.util.UUID

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.utils.VirtualInventory
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import scala.collection.mutable

/**
 * SQLite3でのDatabase実装
 */
class SQLite(plugin: WarsCore) extends Database {

  private val path = "database.db"
  private val config = new HikariConfig()

  config.setDriverClassName("org.sqlite.JDBC")
  config.setJdbcUrl(s"jdbc:sqlite:${plugin.getDataFolder.getAbsolutePath}/$path")
  config.setConnectionInitSql("SELECT 1")

  val hikari = new HikariDataSource(config)

  /**
   * インスタンス作成時に呼び出されるメソッド
   */
  def init(): Unit = {
    val c = hikari.getConnection()
    try {
      val st = c.createStatement()
      /* 基本情報これさえあれば困らないデータ */
      // TDM... play, win, kill, death, assist, damage: Int = _

      st.execute(
        """
          |CREATE TABLE IF NOT EXISTS player (
          | uuid TEXT PRIMARY KEY,
          | donateID INTEGER DEFAULT 0
          |);
          |
          |CREATE TABLE IF NOT EXISTS rank (
          | uuid TEXT PRIMARY KEY,
          | id INTEGER DEFAULT 1,
          | exp INTEGER DEFAULT 0,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS tag (
          | uuid TEXT PRIMARY KEY,
          | tagId TEXT DEFAULT "",
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS enderchest (
          | uuid TEXT PRIMARY KEY,
          | s0 BLOB,
          | s1 BLOB,
          | s2 BLOB,
          | s3 BLOB,
          | s4 BLOB,
          | s5 BLOB,
          | s6 BLOB,
          | s7 BLOB,
          | s8 BLOB,
          | s9 BLOB,
          | s10 BLOB,
          | s11 BLOB,
          | s12 BLOB,
          | s13 BLOB,
          | s14 BLOB,
          | s15 BLOB,
          | s16 BLOB,
          | s17 BLOB,
          | s18 BLOB,
          | s19 BLOB,
          | s20 BLOB,
          | s21 BLOB,
          | s22 BLOB,
          | s23 BLOB,
          | s24 BLOB,
          | s25 BLOB,
          | s26 BLOB,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS tdm (
          | uuid TEXT,
          | play INTEGER DEFAULT 0,
          | win INTEGER DEFAULT 0,
          | kill INTEGER DEFAULT 0,
          | death INTEGER DEFAULT 0,
          | assist INTEGER DEFAULT 0,
          | damage INTEGER DEFAULT 0
          |);
          |
          |CREATE TABLE IF NOT EXISTS tactics (
          | uuid TEXT,
          | play INTEGER DEFAULT 0,
          | win INTEGER DEFAULT 0,
          | FOREIGN KEY(uuid) REFERENCES player(uuid)
          | ON DELETE CASCADE ON UPDATE CASCADE
          |);
          |
          |CREATE TABLE IF NOT EXISTS tagContainer (
          | uuid TEXT,
          | tagId TEXT
          |);
          |
          |""".stripMargin)
    } catch {
      case e: SQLException => e.printStackTrace()
    }
  }

  //init()

  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   *
   * @param uuid UUID
   * @return データがあるならtrueを返す
   */
  override def hasUUID(uuid: UUID): Boolean = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("SELECT uuid FROM player WHERE uuid=?")
    try {
      ps.setString(1, uuid.toString)
      val rs = ps.executeQuery()
      rs.next()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * テーブルにデータを保存するメソッド
   */
  override def saveWPlayer(wp: WPlayer): Option[WPlayer] = None

  /**
   * データを登録するメソッド
   *
   * @param uuid UUID
   * @return
   */
  override def insert(uuid: UUID): Boolean = {
    val c = hikari.getConnection()
    val s = c.createStatement()
    try {
      val build = (table: String) => s"INSERT INTO $table(`uuid`) VALUES('${uuid.toString}')"
      s.executeUpdate(build("player"))
      s.executeUpdate(build("rank"))
      s.executeUpdate(build("enderchest"))
      s.executeUpdate(build("tdm"))
      s.executeUpdate(build("tactics"))
      true
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    } finally {
      s.close()
      c.close()
    }
  }

  override def getInt(table: String, column: String, uuid: String): Option[Int] = {
    val c = hikari.getConnection
    val ps = c.prepareStatement(s"SELECT $column FROM $table WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if (rs.next()) {
        Option(rs.getInt(column))
      } else {
        None
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        None
    } finally {
      ps.close()
      c.close()
    }
  }

  override def getStorage(id: Int, uuid: String): Option[String] = {
    val col = s"s$id"
    val c = hikari.getConnection
    val ps = c.prepareStatement(s"SELECT $col FROM `enderchest` WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if (rs.next()) {
        Option(rs.getString(col))
      } else {
        None
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        None
    } finally {
      ps.close()
      c.close()
    }
  }

  override def setStorage(id: Int, uuid: String, item: String): Unit = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement(s"UPDATE `enderchest` SET `s$id`=? WHERE `uuid`=?")
    try {
      ps.setString(1, item)
      ps.setString(2, uuid)
      ps.executeUpdate()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  def getPresentStorage(cond: String): Option[String] = {
    //val c = hikari.getConnection
    return null
  }

  override def getRankData(uuid: String): Option[(Int, Int)] = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("SELECT id, exp FROM rank WHERE uuid=?")
    try {
      ps.setString(1, uuid)
      val rs = ps.executeQuery()
      if (rs.next()) {
        Some(rs.getInt("id"), rs.getInt("exp"))
      } else {
        None
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        None
    } finally {
      ps.close()
      c.close()
    }
  }

  override def setRankData(uuid: String, data: (Int, Int)): Unit = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("UPDATE rank SET id=?, exp=? WHERE uuid=?")
    try {
      ps.setInt(1, data._1)
      ps.setInt(2, data._2)
      ps.setString(3, uuid)
      ps.executeUpdate()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  override def updateTDM(game: TeamDeathMatch): Boolean = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("UPDATE tdm SET kill=kill+?, death=death+?, assist=assist+?, damage=damage+?, damaged=damaged+?, win=win+?, play=play+1 WHERE uuid=?")
    try {
      game.data.map(f => (f._1.getUniqueId.toString, f._2)).foreach(f => {
        ps.setInt(1, f._2.kill)
        ps.setInt(2, f._2.death)
        ps.setInt(3, f._2.assist)
        ps.setInt(4, f._2.damage.toInt)
        ps.setInt(5, f._2.damaged.toInt)
        ps.setInt(6, if (f._2.win) 1 else 0)
        ps.setString(7, f._1)
        ps.executeUpdate()
      })
      true
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * タグを取得する
   *
   * @param uuid     UUIDを指定
   * @param callback 非同期で返される
   * @version 2
   * @since v1.3
   */
  override def getTags(uuid: String, callback: Callback[mutable.Buffer[(String, Boolean)]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {

        callback.async = true

        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT * FROM tag WHERE uuid=?")
        try {
          ps.setString(1, uuid)
          val rs = ps.executeQuery()
          var buffer = mutable.Buffer.empty[(String, Boolean)]
          while (rs.next()) {
            buffer.+=((rs.getString("tagId"), rs.getBoolean("use")))
          }
          callback.success(buffer)
        } catch {
          case e: SQLException =>
            callback.failure(e)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }

  /**
   * 設定しているタグを返す
   *
   * @param uuid     UUIDを指定
   * @param callback 非同期で返される
   * @version 2
   * @since v1.3
   */
  override def getTag(uuid: String, callback: Callback[String]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {

        callback.async = true

        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT tagId FROM tag WHERE uuid=? and use=1")
        try {
          ps.setString(1, uuid)
          val rs = ps.executeQuery()
          if (rs.next()) {
            callback.success(rs.getString("tagId"))
          } else {
            callback.success("")
          }
        } catch {
          case e: SQLException =>
            callback.failure(e)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }


  /**
   * タグをセットする
   *
   * @param uuid UUID
   * @param id   タグID
   */
  override def setTag(uuid: String, id: String): Unit = {
    val c = hikari.getConnection
    val ps = c.prepareStatement("UPDATE `tag` SET `tagId`=? WHERE `uuid`=?")
    try {
      ps.setString(1, id)
      ps.setString(2, uuid)
      ps.executeUpdate()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * タグコンテナにタグを追加する
   *
   * @param uuid UUID
   * @param id   タグID
   */
  override def addTag(uuid: String, id: String): Unit = {
    val c = hikari.getConnection
    val ps = c.prepareStatement("INSERT INTO `tagContainer` VALUES(?,?)")
    try {
      ps.setString(1, uuid)
      ps.setString(2, id)
      ps.executeUpdate()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  def gameLog(gameid: String, level: String, message: String): Unit = {
    val c = hikari.getConnection
    val ps = c.prepareStatement("INSERT INTO `gamelog` VALUES((select datetime()), ?, ?, ?)")
    try {
      ps.setString(1, gameid)
      ps.setString(2, level)
      ps.setString(3, message)
      ps.executeUpdate()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * 仮想インベントリを読み込む
   *
   * @param wp
   * @param col normalまたはgame
   */
  override def getVInventory(wp: WPlayer, col: String): Unit = {
    val c = hikari.getConnection
    val ps = c.prepareStatement("SELECT ? FROM `vinv` WHERE `uuid`=?")
    try {
      ps.setString(1, col)
      ps.setString(2, wp.player.getUniqueId.toString)
      val rs = ps.executeQuery()
      if (rs.next()) {
        rs.getString(col)
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * 仮想インベントリをセーブする
   *
   * @param wp
   * @param col normalまたはgame
   */
  override def setVInventory(wp: WPlayer, col: String): Unit = ???

  /**
   * WPプレイヤーの保存データを読み込む
   *
   * @version 2.0
   * @since v1.1.18
   * @param wp       対象のプレイヤー
   * @param callback 取得したデータを同期的に返す
   */
  override def loadWPlayer(wp: WPlayer, callback: Callback[WPlayer]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT player.uuid, rank.id, rank.exp, tag.tagId, vinv.normal, vinv.game FROM player JOIN rank ON player.uuid=rank.uuid JOIN tag ON player.uuid=tag.uuid and tag.use=1 JOIN vinv ON player.uuid=vinv.uuid WHERE player.uuid=?")
        try {
          ps.setString(1, wp.player.getUniqueId.toString)
          val rs = ps.executeQuery()
          if (rs.next()) {
            wp.rank = rs.getInt("id")
            wp.exp = rs.getInt("exp")
            wp.tag = rs.getString("tagId")
            wp.virtualNormalInventory = Option(VirtualInventory.from(rs.getString("normal")))
            wp.virtualGameInventory = Option(VirtualInventory.from(rs.getString("game")))
          } else {
            wp.rank = -9999
            wp.exp = -9999
            wp.tag = "Unknown"
            wp.virtualNormalInventory = Option(VirtualInventory.empty())
            wp.virtualGameInventory = Option(VirtualInventory.empty())
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(wp)
            }
          }.runTask(WarsCore.instance)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(WarsCore.instance)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }


  override def close(): Unit = hikari.close()

  /**
   * アイテムを読み込む
   *
   * @param uuid
   * @param baseSlot ベースページ (page - 1) * 45 で求まる
   * @param callback | String Type
   *                 | Array[Byte] アイテムのRAWデータ
   *                 | Int slot
   *                 | Int use!?
   */
  override def getPagedWeaponStorage(uuid: String, baseSlot: Int, callback: Callback[mutable.Buffer[(Int, Array[Byte], Int)]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT * FROM weapon WHERE uuid=? and ?<=slot and slot<?")
        try {
          ps.setString(1, uuid)
          ps.setInt(2, baseSlot)
          ps.setInt(3, baseSlot + 45)
          val rs = ps.executeQuery()
          var buffer = mutable.Buffer.empty[(Int, Array[Byte], Int)]
          while (rs.next()) {
            buffer.+=((rs.getInt("slot"), rs.getBytes("data"), rs.getInt("use")))
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(buffer)
            }
          }.runTask(WarsCore.instance)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(WarsCore.instance)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }

  /**
   * アイテムを保存する。
   *
   * @param uuid
   * @param baseSlot
   * @param contents
   */
  def setPagedWeaponStorage(uuid: String, baseSlot: Int, contents: Map[Boolean, Seq[(Int, ItemStack)]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        // 使われてないスロットの削除を行う
        val ps1 = c.prepareStatement("DELETE FROM weapon WHERE uuid=? and slot=?")
        try {
          ps1.setString(1, uuid)
          contents.get(true) match {
            case Some(content) =>
              content.foreach(f => {
                ps1.setInt(2, baseSlot + f._1)
                ps1.executeUpdate()
              })
            case None =>
          }
        } catch {
          case e: SQLException =>
            e.printStackTrace()
        } finally {
          ps1.close()
        }
        // 順にUUID, スロット, データ, 使ってるか(特定のエンチャントされてるか)(関係なし)
        val ps2 = c.prepareStatement("REPLACE INTO weapon(uuid, slot, data) VALUES(?, ?, ?)")
        try {
          ps2.setString(1, uuid)
          contents.get(false) match {
            case Some(content) =>
              // 呪い = useが0以外 = 無視！
              content.filterNot(pred => pred._2.containsEnchantment(Enchantment.BINDING_CURSE)).foreach(f => {
                ps2.setInt(2, baseSlot + f._1)
                //println(f._2.serializeAsBytes().mkString("Array(", ", ", ")"))
                ps2.setBytes(3, f._2.serializeAsBytes())
                ps2.executeUpdate()
                //println(i)
              })
            case None =>
          }
        } catch {
          case e: SQLException =>
            e.printStackTrace()
        } finally {
          ps2.close()
        }
        c.close()
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }

  /**
   * 武器を設定する
   *
   * @since v1.3.4
   * @param uuid
   * @param slot
   */
  override def setPagedWeapon(uuid: String, slot: Int, callback: Callback[Unit]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        s.addBatch(s"UPDATE weapon SET use=0 WHERE uuid='$uuid' and use=1")
        s.addBatch(s"UPDATE weapon SET use=1 WHERE uuid='$uuid' and slot=$slot")
        try {
          s.executeBatch()
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success()
            }
          }.runTask(WarsCore.instance)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(WarsCore.instance)
        } finally {
          s.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }

  /**
   * 現在使用している(use=1)の武器を読み込む
   *
   * @param uuid
   * @param callback
   */
  override def getWeapon(uuid: String, callback: Callback[mutable.Buffer[Array[Byte]]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT data FROM weapon WHERE uuid=? and use=1")
        try {
          ps.setString(1, uuid)
          var buffer = mutable.Buffer.empty[Array[Byte]]
          val rs = ps.executeQuery()
          while(rs.next()) {
            buffer.+=(rs.getBytes("data"))
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(buffer)
            }
          }.runTask(WarsCore.instance)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(WarsCore.instance)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }
}
