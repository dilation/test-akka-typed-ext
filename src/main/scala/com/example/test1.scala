package com.example

import akka.typed.{ ActorSystem, Extension, ExtensionId }
import akka.typed.scaladsl.adapter._

final class MyExtension private () extends Extension {
  println("Creating MyExtension ...")
}

object MyExtension extends ExtensionId[MyExtension] {

  override def createExtension(system: ActorSystem[_]): MyExtension = {
    new MyExtension()
  }
}

object Test1 {
  def main(args: Array[String]): Unit = {
    val sys = akka.actor.ActorSystem("test1")
    MyExtension(sys.toTyped)
    MyExtension(sys.toTyped)
    Thread.sleep(1000L)
    sys.terminate()
  }
}
