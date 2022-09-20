package hm.moe.pokkedoll.warscore.utils

import java.io.{DataOutputStream, File, FileOutputStream, IOException}
import java.nio.file.{Files, Path}
import java.util.zip.ZipFile
import hm.moe.pokkedoll.warscore.{Callback, WarsCore}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, World, WorldCreator}

import scala.jdk.CollectionConverters.*
import reflect.Selectable.reflectiveSelectable
import scala.language.reflectiveCalls

/**
 * Warsから持ってきた.<br>
 * 単体でも利用可能.
 *
 * @author Emorard
 */
@Deprecated("")
object WorldLoader {

  private def load(name: String): World = {
    val wc = new WorldCreator(name).generateStructures(false).environment(World.Environment.NORMAL)
    val w = Bukkit.createWorld(wc)
    w.setKeepSpawnInMemory(false)
    w.setAutoSave(false)
    w
  }

  private def unload(name: String): Boolean = {
    Option(Bukkit.getWorld(name)) match {
      case Some(world) =>
        if (world.getPlayers.size() != 0) {
          world.getPlayers.forEach(p => {
            p.teleport(Bukkit.getWorlds.get(0).getSpawnLocation)
          })
        }
        Bukkit.unloadWorld(world, false)
      case None =>
        false
    }
  }

  private def unload(world: World): Boolean = {
    if (world.getPlayerCount != 0) {
      world.getPlayers.forEach(p => p.teleport(Bukkit.getWorlds.get(0).getSpawnLocation))
    }
    Bukkit.unloadWorld(world, false)
  }


  def using[T <: {def close(): Unit}, U](resource: T)(block: T => U): U = {
    try {
      block(resource)
    } finally {
      if (resource != null) {
        resource.close()
      }
    }
  }

  def unzip2(zipPath: Path, outputPath: Path): Unit = {
    using(new ZipFile(zipPath.toFile)) { zipFile =>
      for (entry <- zipFile.entries().asScala) {
        val path = outputPath.resolve(entry.getName)
        if (entry.isDirectory) {
          Files.createDirectories(path)
        } else {
          Files.createDirectories(path.getParent)
          Files.copy(zipFile.getInputStream(entry), path)
        }
      }
    }
  }

  private def delete(file: File): Boolean = {
    if (file.exists()) {
      if (file.isFile) {
        if (file.delete()) {
          WarsCore.instance.getLogger.info("Successed delete file")
        } else {
          WarsCore.instance.getLogger.info("Failed delete file")
        }
      } else if (file.isDirectory) {
        file.listFiles.foreach(f => delete(f))
        if (file.delete()) {
          WarsCore.instance.getLogger.info("Successed delete directory")
        } else {
          WarsCore.instance.getLogger.info("Failed delete directory")
        }
      }
      true
    } else false
  }

  private def reSession(session: File): Unit = {
    session.delete()
    var dataoutputstream: DataOutputStream = null
    try {
      session.createNewFile()
      dataoutputstream = new DataOutputStream(new FileOutputStream(session))
      dataoutputstream.writeLong(System.currentTimeMillis())
    } catch {
      case e: IOException =>
        e.printStackTrace()
    } finally {
      if (dataoutputstream != null) dataoutputstream.close()
    }
  }

  /**
   * 非同期的にワールドの解凍、読み込みの処理をする<br>
   * メモリリークの可能性を減らした？
   *
   * @param world    解凍するワールドの名前
   * @param worldId   解凍先のワールドの名前
   * @param callback ワールド
   */
  def asyncLoadWorld(world: String, worldId: String, callback: Callback[World]): Unit = {
    asyncUnloadWorld(worldId)
    new BukkitRunnable {
      override def run(): Unit = {
        unzip2(new File(s"./worlds/$world.zip").toPath, new File(s"./$worldId").toPath)
        reSession(new File(s"./$worldId/session.lock"))
        new BukkitRunnable {
          override def run(): Unit = {
            callback.success(load(worldId))
          }
        }.runTask(WarsCore.instance)
      }
    }.runTaskAsynchronously(WarsCore.instance)
  }

  def asyncUnloadWorld(world: String): Unit = {
    val file = new File(s"./$world")
    if ((Option(Bukkit.getWorld(world)) match {
      case Some(w) => unload(w)
      case _ => false
    }) || file.exists()) {
      new BukkitRunnable {
        override def run(): Unit = {
          delete(file)
        }
      }.runTaskAsynchronously(WarsCore.instance)
    }
  }
}
