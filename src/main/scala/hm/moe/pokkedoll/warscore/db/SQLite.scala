package hm.moe.pokkedoll.warscore.db

import java.sql.{ResultSet, SQLException}

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.ui.WeaponUI
import hm.moe.pokkedoll.warscore.utils.ShopUtil.Shop
import hm.moe.pokkedoll.warscore.utils.{Item, ShopUtil}
import hm.moe.pokkedoll.warscore.utils.TagUtil.UserTagInfo
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import scala.collection.mutable

/**
 * SQLite3でのDatabase実装
 */
class SQLite(private val plugin: WarsCore) extends Database {

  private val path = "database.db"
  private val config = new HikariConfig()

  config.setDriverClassName("org.sqlite.JDBC")
  config.setJdbcUrl(s"jdbc:sqlite:${plugin.getDataFolder.getAbsolutePath}/$path")
  config.setConnectionInitSql("SELECT 1")

  val hikari = new HikariDataSource(config)

  /**
   * データベースに自分のデータがあるか確認するメソッド
   *
   * @since v1.4.1
   * @param uuid 対象のUUID
   * @return UUIDが存在すればtrue
   */
  override def hasUUID(uuid: String): Boolean = {
    val c = hikari.getConnection()
    val ps = c.prepareStatement("SELECT uuid FROM player WHERE uuid=?")
    try {
      ps.setString(1, uuid)
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
   * データを登録するメソッド
   *
   * @param uuid UUID
   * @return
   */
  override def insert(uuid: String): Boolean = {
    val c = hikari.getConnection()
    val s = c.createStatement()
    try {
      s.addBatch(s"INSERT INTO player(uuid) VALUES('$uuid')")
      s.addBatch(s"INSERT INTO rank(uuid) VALUES('$uuid')")
      s.addBatch(s"INSERT INTO tag(uuid, use) VALUES('$uuid', 1)")
      s.addBatch(s"INSERT INTO tdm(uuid) VALUES('$uuid')")
      s.addBatch(s"INSERT INTO tactics(uuid) VALUES('$uuid')")
      s.executeBatch()
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
  override def getTags(uuid: String, callback: Callback[Vector[UserTagInfo]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {

        callback.async = true

        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT * FROM tag WHERE uuid=?")
        try {
          ps.setString(1, uuid)
          val rs = ps.executeQuery()
          var vec = Vector.empty[UserTagInfo]
          while (rs.next()) {
            vec :+= new UserTagInfo(rs.getString("tagId"), rs.getBoolean("use"))
          }
          callback.success(vec)
        } catch {
          case e: SQLException =>
            callback.failure(e)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
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
    }.runTaskAsynchronously(plugin)
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
        val ps = c.prepareStatement("SELECT player.uuid, player.disconnect, rank.id, rank.exp, tag.tagId FROM player JOIN rank ON player.uuid=rank.uuid JOIN tag ON player.uuid=tag.uuid and tag.use=1 WHERE player.uuid=?")
        try {
          ps.setString(1, wp.player.getUniqueId.toString)
          val rs = ps.executeQuery()

          if (rs.next()) {
            //println("true!")
            wp.rank = rs.getInt("id")
            wp.exp = rs.getInt("exp")
            wp.tag = rs.getString("tagId")
            wp.disconnect = ((i: Int) => (if (i == 1) true else false)) (rs.getInt("disconnect"))
          } else {
            // println("false!!!")
            wp.rank = -9999
            wp.exp = -9999
            wp.tag = "Unknown"
            wp.disconnect = false
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(wp)
            }
          }.runTask(plugin)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(plugin)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }


  override def close(): Unit = hikari.close()

  /**
   * 仮のインベントリ(ロビーのインベントリを取得する
   *
   * @version v1.3.15
   * @param uuid     対象のUUID
   * @param callback (スロット番号, シリアライズされたアイテムスタック)のタプル
   */
  override def getVInv(uuid: String, callback: Callback[mutable.Buffer[(Int, Array[Byte])]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT slot, data FROM vinv WHERE uuid=?")
        try {
          ps.setString(1, uuid)
          val rs = ps.executeQuery()
          var buffer = mutable.Buffer.empty[(Int, Array[Byte])]
          while (rs.next()) {
            buffer.+=((rs.getInt("slot"), rs.getBytes("data")))
          }
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(buffer)
            }
          }.runTask(plugin)
        } catch {
          case e: SQLException =>
            new BukkitRunnable {
              override def run(): Unit = {
                callback.failure(e)
              }
            }.runTask(plugin)
        } finally {
          ps.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.15
   * @param uuid
   * @param contents
   */
  override def setVInv(uuid: String, contents: Array[ItemStack], callback: Callback[Unit]): Unit = {
    if (contents.isEmpty) {
      callback.success()
    } else {
      new BukkitRunnable {
        override def run(): Unit = {
          val c = hikari.getConnection
          val s = c.createStatement()
          val ps = c.prepareStatement("INSERT INTO vinv VALUES(?, ?, ?)")
          try {
            s.executeUpdate(s"DELETE FROM vinv WHERE uuid='$uuid'")
            ps.setString(1, uuid)
            contents.indices.map(f => (f, contents(f)))
              .filterNot(f => f._2 == null || f._2.getType == Material.AIR)
              .foreach(f => {
                ps.setInt(2, f._1)
                ps.setBytes(3, f._2.serializeAsBytes())
                ps.addBatch()
              })
            ps.executeBatch()
            new BukkitRunnable {
              override def run(): Unit = {
                callback.success()
              }
            }.runTask(plugin)
          } catch {
            case e: SQLException =>
              new BukkitRunnable {
                override def run(): Unit = {
                  callback.failure(e)
                }
              }.runTask(plugin)
          } finally {
            s.close()
            ps.close()
            c.close()
          }
        }
      }.runTaskAsynchronously(plugin)
    }
  }

  /**
   * 試合中に切断した場合(Gameインスタンスが設定している場合)にtrueにする
   *
   * @version v1.3.22
   * @param uuid 対象のUUID
   */
  def setDisconnect(uuid: String, disconnect: Boolean): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("UPDATE player SET disconnect=? WHERE uuid=?")
        try {
          ps.setBoolean(1, disconnect)
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
    }.runTaskAsynchronously(plugin)
  }

  /**
   * データベースから未加工のデータを取得する
   *
   * @param uuid   UUID
   * @param offset 取得を始める番号
   * @return (type, name, amount, use)の組
   */
  override def getOriginalItem(uuid: String, offset: Int): List[(String, String, Int, Boolean)] = {
    val c = hikari.getConnection
    val s = c.createStatement()
    var list = List.empty[(String, String, Int, Boolean)]
    try {
      val rs = s.executeQuery(s"SELECT type, name, amount, use FROM weapon WHERE uuid='$uuid' LIMIT 45 OFFSET $offset")
      while (rs.next()) {
        list :+= (rs.getString("type"), rs.getString("name"), rs.getInt("amount"), rs.getBoolean("use"))
      }
      rs.close()
      list
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        list
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * 武器を取得する
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @return 武器たち
   */
  override def getWeapons(uuid: String, t: String): Seq[Item] = {
    val c = hikari.getConnection
    val s = c.createStatement()
    var seq = IndexedSeq.empty[Item]
    try {
      val rs = s.executeQuery(s"SELECT name, amount FROM weapon WHERE uuid='$uuid' and type='$t'")
      while (rs.next()) {
        seq :+= new Item(rs.getString(1), rs.getInt("amount"))
      }
      rs.close()
      seq
    } catch {
      case _: SQLException =>
        seq
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * 現在設定されている武器のリストを取得する
   *
   * @param uuid 対象のUUID
   * @return 武器のタプル(メイン, サブ, 近接, アイテム)
   */
  override def getActiveWeapon(uuid: String): (String, String, String, String) = {
    val c = hikari.getConnection
    val ps = c.prepareStatement("SELECT name FROM weapon WHERE use=? and uuid=? and type=?")
    try {
      ps.setInt(1, 1)
      ps.setString(2, uuid)
      val gr = ((rs: ResultSet) => if (rs.next()) rs.getString(1) else "")
      ps.setString(3, WeaponDB.PRIMARY)
      val primary = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.SECONDARY)
      val secondary = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.MELEE)
      val melee = gr(ps.executeQuery())
      ps.setString(3, WeaponDB.ITEM)
      val item = gr(ps.executeQuery())
      (primary, secondary, melee, item)
    } catch {
      case _: SQLException =>
        ("", "", "", "")
    } finally {
      ps.close()
      c.close()
    }
  }

  /**
   * 武器をセットする
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @param name 武器のデータ
   */
  override def setWeapon(uuid: String, t: String, name: String): Unit = {
    val c = hikari.getConnection
    val s = c.createStatement()
    try {
      s.addBatch(s"UPDATE weapon SET use=0 WHERE uuid='$uuid' and type='$t'")
      s.addBatch(s"UPDATE weapon SET use=1 WHERE uuid='$uuid' and type='$t' and name='$name'")
      s.executeBatch()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * 武器を追加する
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @param name 武器のデータ
   */
  override def addWeapon(uuid: String, t: String, name: String, amount: Int = 1): Unit = {
    val c = hikari.getConnection
    val s = c.createStatement()
    try {
      s.executeUpdate(
        s"INSERT INTO weapon (uuid, type, name, amount) VALUES('$uuid', '$t', '$name', $amount) ON CONFLICT(uuid, type, name) DO UPDATE SET amount = amount+$amount"
      )
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * アイテムを追加する。タイプはitemに固定される。さらに非同期！
   * @param uuid 対象のUUID
   * @param item アイテム
   */
  override def addItem(uuid: String, item: Item*): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.prepareStatement("INSERT INTO weapon (uuid, type, name, amount) VALUES(?, ?, ?, ?) ON CONFLICT(uuid, type, name) DO UPDATE SET amount = amount+?")
        try {
          s.setString(1, uuid)
          s.setString(2, WeaponDB.ITEM)
          item.foreach(i => {
            s.setString(3, i.name)
            s.setInt(4, i.amount)
            s.setInt(5, i.amount)
            s.addBatch()
          })
          s.executeBatch()
        } catch {
          case e: SQLException =>
            e.printStackTrace()
        } finally {
          s.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  def buylWeapon(uuid: String, t: String, shop: ShopUtil.Shop, callback: Callback[String]): Unit = {
    val c = hikari.getConnection
    val s = c.createStatement()
    try {
      val text = shop.price.map(shopItem => s"(name='${shopItem.name}' AND amount>=${shopItem.amount})").mkString(" OR ")
      val rs = s.executeQuery(s"SELECT CASE WHEN $text THEN 1 ELSE 0 FROM weapon WHERE uuid='$uuid'")
      if(rs.next() && rs.getBoolean(1)) {
        addWeapon(uuid, shop.`type`, shop.product.name, shop.product.amount)
        delWeapon(uuid, shop.price)
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * 武器を削除する
   *
   * @param uuid
   * @param price
   */
  override def delWeapon(uuid: String, price: Array[Item]): Unit = {
    val c = hikari.getConnection
    val s = c.createStatement()
    try {
      price.foreach(f => {
        s.addBatch(s"UPDATE weapon SET amount=amount-${f.amount} WHERE uuid='$uuid' and name='${f.name}'")
      })
      s.addBatch(s"DELETE FROM weapon WHERE uuid='$uuid' and 0>=amount")
      s.executeBatch()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * 実際にプレイやーが所持しているアイテムを付け加えて返す。非同期で使う
   * @param uuid UUID
   * @param item Shop.priceで獲得できる
   * @return Itemと実際に所持しているアイテムの組
   */
  override def getRequireItemsAmount(uuid: String, item: Array[Item]): Map[String, Int] = {
    val c = hikari.getConnection
    val s = c.createStatement()
    var map = Map.empty[String, Int]
    try {
      val rs = s.executeQuery(s"SELECT name, amount FROM weapon WHERE uuid='$uuid' and name in (${item.map(f => s"'${f.name}'").mkString(", ")})")
      while(rs.next()) {
        map += rs.getString("name") -> rs.getInt("amount")
      }
      rs.close()
    } catch {
      case e: SQLException =>
        e.printStackTrace()
    } finally {
      s.close()
      c.close()
    }
    map
  }
}
