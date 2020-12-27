package hm.moe.pokkedoll.warscore.db

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.SQLException
import java.util.UUID

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.ui.WeaponUI
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.apache.commons.lang.SerializationUtils
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
      s.addBatch(s"INSERT INTO player(uuid) VALUES('$uuid')'")
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
                ps2.setBytes(3, f._2.serializeAsBytes())
                plugin.getLogger.info(s"REPLACE INTO ${baseSlot + f._1}, ${f._2}")
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
    }.runTaskAsynchronously(plugin)
  }

  /**
   * すべてのアイテムを読み込む！！
   * @param uuid UUID
   */
  override def getWeaponStorage(uuid: String): Vector[ItemStack] = {
    val c = hikari.getConnection
    val s = c.createStatement()
    val sql = s"SELECT data FROM weapon WHERE uuid='$uuid'"
    var vec = Vector.empty[ItemStack]
    try {
      val rs = s.executeQuery(sql)
      while (rs.next()) {
        vec :+= ItemStack.deserializeBytes(rs.getBytes("data"))
      }
      vec
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        vec
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * 武器を設定する
   *
   * @since v1.3.4
   * @param uuid     対象のUUID
   * @param slot     新しく設定するスロット
   * @param usedSlot 以前設定していた純粋なスロット(ベースページとかインベントリ上段の処理を考える必要がない)
   */
  override def setPagedWeapon(uuid: String, slot: Int, usedSlot: Int, usedType: Int, callback: Callback[Unit]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        s.addBatch(s"UPDATE weapon SET use=0 WHERE uuid='$uuid' and use=$usedType and slot=$usedSlot")
        s.addBatch(s"UPDATE weapon SET use=$usedType WHERE uuid='$uuid' and slot=$slot")
        try {
          s.executeBatch()
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
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  /**
   * 現在使用している(use>0)の武器を読み込む
   *
   * @param uuid
   * @param callback (アイテムのバイトデータ, 使用タイプ)
   */
  override def getWeapon(uuid: String, callback: Callback[mutable.Buffer[(Array[Byte], Int)]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val ps = c.prepareStatement("SELECT data, use FROM weapon WHERE uuid=? and use>0")
        try {
          ps.setString(1, uuid)
          var buffer = mutable.Buffer.empty[(Array[Byte], Int)]
          val rs = ps.executeQuery()
          while (rs.next()) {
            buffer.+=((rs.getBytes("data"), rs.getInt("use")))
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
   * アクティブなマイセットを獲得する
   *
   * @param uuid     対象のUUID
   * @param callback コールバック
   */
  override def getActiveMySet(uuid: String, callback: Callback[Array[Array[Byte]]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        val sql = s"SELECT * FROM myset WHERE uuid='$uuid' and use=1"
        try {
          val rs = s.executeQuery(sql)
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(
                if (rs.next()) {
                  Array(
                    rs.getBytes("main"),
                    rs.getBytes("sub"),
                    rs.getBytes("melee"),
                    rs.getBytes("item"))
                } else {
                  Array.empty[Array[Byte]]
                }
              )
            }
          }.runTask(plugin)
          rs.close()
        } catch {
          case e: SQLException =>
            e.printStackTrace()
        } finally {
          s.close()
          c.close()
        }
      }
    }
  }

  override def checkMySet(uuid: String, slot: Int): Boolean = {
    val c = hikari.getConnection
    val s = c.createStatement()
    val sql = "SELECT CASE WHEN " +
      s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.main and myset.use=1) IS NOT NULL and " +
      s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.sub and myset.use=1) IS NOT NULL and " +
      s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.melee and myset.use=1) IS NOT NULL and " +
      s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.item and myset.use=1) IS NOT NULL THEN 1" +
      "ELSE 0 FROM myset"
    try {
      val rs = s.executeQuery(sql)
      if(rs.next()) {
        rs.getBoolean(0)
      } else {
        false
      }
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    } finally {
      s.close()
      c.close()
    }
  }

  /**
   * マイセットを読み込む
   *
   * @since v1.4.3
   * @param uuid     対象のUUID
   * @param callback 順にslot, title, main, sub, melee, itemのタプル
   */
  override def getMySet(uuid: String, callback: Callback[mutable.Buffer[WeaponUI.MySet]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        val sql = "SELECT myset.slot, myset.title, " +
          s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.main) as main, " +
          s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.sub) as sub, " +
          s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.melee) as melee, " +
          s"(SELECT data FROM weapon WHERE uuid='$uuid' and data=myset.item) as item " +
          s"FROM myset WHERE uuid='$uuid'"

        try {
          val rs = s.executeQuery(sql)
          val buffer = mutable.Buffer.empty[WeaponUI.MySet]
          while (rs.next()) {
            val slot = rs.getInt("slot")
            val title = rs.getString("title")
            val main = rs.getBytes("main")
            val sub = rs.getBytes("sub")
            val melee = rs.getBytes("melee")
            val item = rs.getBytes("item")

            // println(s"${slot} + $title")
            buffer.+=(
              new WeaponUI.MySet(
                slot,
                title,
                Option(main),
                Option(sub),
                Option(melee),
                Option(item)
              )
            )
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
          s.close()
          c.close()
        }
      }
    }
  }.runTaskAsynchronously(plugin)

  /**
   * マイセットを設定する
   *
   * @since v1.4.3
   * @param uuid
   * @param slot
   * @param callback
   */
  override def setMySet(uuid: String, slot: Int, callback: Callback[Unit]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        val sql =
          s"REPLACE INTO myset(uuid, slot, title, main, sub, melee, item) VALUES('$uuid', $slot, '無題のマイセット$slot', " +
            s"(SELECT data FROM weapon WHERE uuid='$uuid' and use=1), " +
            s"(SELECT data FROM weapon WHERE uuid='$uuid' and use=2), " +
            s"(SELECT data FROM weapon WHERE uuid='$uuid' and use=3), " +
            s"(SELECT data FROM weapon WHERE uuid='$uuid' and use=4))"
        try {
          s.executeUpdate(sql)
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
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  /**
   * マイセットを適用する
   *
   * @since v1.4.18
   */
  override def applyMySet(uuid: String, slot: Int, callback: Callback[Unit]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        s"(SELECT data FROM weapon WHERE uuid='$uuid' and use=4))"
        try {
          s.addBatch(s"UPDATE weapon SET use=0 WHERE uuid='${uuid}' and use>0")
          s.addBatch(s"UPDATE weapon SET use=1 WHERE uuid='${uuid}' and data=(SELECT main FROM myset WHERE uuid='${uuid}' and slot=$slot)")
          s.addBatch(s"UPDATE weapon SET use=2 WHERE uuid='${uuid}' and data=(SELECT sub FROM myset WHERE uuid='${uuid}' and slot=$slot)")
          s.addBatch(s"UPDATE weapon SET use=3 WHERE uuid='${uuid}' and data=(SELECT melee FROM myset WHERE uuid='${uuid}' and slot=$slot)")
          s.addBatch(s"UPDATE weapon SET use=4 WHERE uuid='${uuid}' and data=(SELECT item FROM myset WHERE uuid='${uuid}' and slot=$slot)")
          s.executeBatch()
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
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  def checkMySet(uuid: String, slot: Int, callback: Callback[Array[Boolean]]): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        try {
          val main = s.executeQuery(s"SELECT data FROM weapon WHERE uuid='$uuid' and data=(SELECT main FROM myset WHERE uuid='$uuid' and slot=$slot").next()
          val sub = s.executeQuery(s"SELECT data FROM weapon WHERE uuid='$uuid' and data=(SELECT sub FROM myset WHERE uuid='$uuid' and slot=$slot").next()
          val melee = s.executeQuery(s"SELECT data FROM weapon WHERE uuid='$uuid' and data=(SELECT melee FROM myset WHERE uuid='$uuid' and slot=$slot").next()
          val item = s.executeQuery(s"SELECT data FROM weapon WHERE uuid='$uuid' and data=(SELECT item FROM myset WHERE uuid='$uuid' and slot=$slot").next()
          new BukkitRunnable {
            override def run(): Unit = {
              callback.success(Array(main, sub, melee, item))
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
          c.close()
        }
      }
    }
  }.runTaskAsynchronously(plugin)

  override def addItem(uuid: String, array: Array[Byte]*): Unit = {
    new BukkitRunnable {
      override def run(): Unit = {
        val c = hikari.getConnection
        val s = c.createStatement()
        val ps = c.prepareStatement("INSERT INTO weapon(uuid, slot, data) VALUES(?, ?, ?)")
        try {
          val rs = s.executeQuery(s"SELECT MIN(slot), MAX(slot) FROM weapon WHERE uuid=${uuid}")
          if (rs.next()) {
            ps.setString(1, uuid)
            var min = rs.getInt(0)
            var max = rs.getInt(0)
            array.foreach(f => {
              /**
               * https://www.techscore.com/tech/Java/JavaEE/JDBC/2-3/
               *
               * 「PreparedStatement」の場合は、値をセットしてから（10〜13行目）、「addBatch」メソッドで追加します。この時点でSQL文ができているので引数は必要ありません。繰り返す事により（16〜20行目）、同じようなSQL文をバッチ処理リストに追加していく事ができます。
               *
               * また「PreparedStatement」は「Statement」クラスを拡張したクラスなので、「Statement」クラスと同じような使用（22〜23行目）の仕方もできます。ただし複数の「PreparedStatement」オブジェクトを組み合わせて1つのバッチ処理を作り上げる事はできません。
               */
              if (min != 0) {
                min -= 1
                ps.setInt(2, min)
                ps.setBytes(3, f)
                ps.addBatch()
              } else {
                max += 1
                ps.setInt(2, max)
                ps.setBytes(3, f)
                ps.addBatch()
              }
            })
            ps.executeBatch()
          }
        } catch {
          case e: SQLException =>
            e.printStackTrace()
        } finally {
          ps.close()
          s.close()
          c.close()
        }
      }
    }.runTaskAsynchronously(plugin)
  }

  override def deleteMySet(uuid: String, slot: Int): Unit = ???
}
