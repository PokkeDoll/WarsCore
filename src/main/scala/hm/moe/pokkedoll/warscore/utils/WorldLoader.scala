package hm.moe.pokkedoll.warscore.utils

import java.io.{DataOutputStream, File, IOException}

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.{Bukkit, World, WorldCreator}

import scala.util.control.Breaks

/**
 * Warsから持ってきた
 */
object WorldLoader {

  def load(name: String): World = {
    val wc = if(name.startsWith("!"))
      new WorldCreator(name.substring(1)).generateStructures(false).environment(World.Environment.THE_END)
    else if(name.startsWith("^"))
      new WorldCreator(name.substring(1)).generateStructures(false).environment(World.Environment.NETHER)
    else
      new WorldCreator(name).generateStructures(false).environment(World.Environment.NORMAL)
    val w = Bukkit.createWorld(wc)
    w.setKeepSpawnInMemory(false)
    w.setAutoSave(false)
    w
  }

  def unload(name: String): Boolean = {
    Option(Bukkit.getWorld(name)) match {
      case Some(world) =>
        if (world.getPlayers.size() != 0) {
          world.getPlayers.forEach(p => {
            p.teleport(Bukkit.getWorlds.get(0).getSpawnLocation)
          })
        }
        return Bukkit.unloadWorld(world, false)
      case None =>
        false
    }
  }

  import java.io.{BufferedInputStream, BufferedOutputStream, FileOutputStream}
  import java.util.zip.ZipFile

  def unzip(zipFileFullPath: String, unzipPath: String): Boolean = {
    val breaks = new Breaks
    import breaks.{break, breakable}
    var zipFile: ZipFile = null
    try {
      zipFile = new ZipFile(zipFileFullPath)
      val enumZip = zipFile.entries()
      while (enumZip.hasMoreElements) {
        breakable {
          val zipEntry = enumZip.nextElement
          val unzipFile = new File(unzipPath)
          val outFile = new File(unzipFile.getAbsolutePath, zipEntry.getName)
          if (zipEntry.isDirectory) {
            outFile.mkdir
            break()
          }
          val in = new BufferedInputStream(zipFile.getInputStream(zipEntry))
          if (!outFile.getParentFile.exists) outFile.getParentFile.mkdirs
          val out = new BufferedOutputStream(new FileOutputStream(outFile))
          val buffer = new Array[Byte](1024)
          var readSize = 0
          while ( {
            readSize = in.read(buffer); readSize != -1
          }) out.write(buffer, 0, readSize)
          try out.close()
          catch {
            case e: Exception => e.printStackTrace()
          }
          try in.close()
          catch {
            case e: Exception => e.printStackTrace()
          }
        }
      }
      // session
      reSession(new File(Bukkit.getWorldContainer, s"$unzipPath/session.lock"))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    } finally if (zipFile != null) try zipFile.close()
    catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def delete(file: File): Boolean = {
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

  def reSession(session: File): Boolean = {
    session.delete()
    try {
      session.createNewFile()
      val dataoutputstream = new DataOutputStream(new FileOutputStream(session))
      dataoutputstream.writeLong(System.currentTimeMillis())
      dataoutputstream.close()
      true
    } catch {
      case e: IOException =>
        e.printStackTrace()
        false
    }
  }

  /**
   * 同期的にワールドの解凍=>読み込みの処理をおこなう
   * "/server/warfare/sample.zip"のように
   *
   * より安全に読み込むようになった
   *
   * @param path 解凍したいワールドのパス
   * @return
   */
  def syncLoadWorld(path: String, world: String): Option[World] = {
    // すでにワールドが読み込まれている場合
    Option(Bukkit.getWorld(world)) match {
      // 不幸にもワールドが存在する場合, 安全にワールドを削除
      case Some(_) =>
        syncUnloadWorld(world)
      case _ =>
        // 読み込まれてないだけで存在するかもしれない
        val w = new File(s"./$world")
        if(w.exists()) {
          delete(w)
        }
    }
    // 読み込む
    if (unzip(s"/server/$path.zip", world)) Option(load(world)) else None
  }

  def syncUnloadWorld(name: String): Unit = if (Bukkit.getWorld(name) != null) {
    if(unload(name)) delete(new File(s"./$name"))
  }
}
