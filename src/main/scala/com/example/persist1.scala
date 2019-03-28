package com.example

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import akka.actor.typed._
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.persistence.typed.scaladsl._
import akka.persistence.typed._
import akka.util.Timeout

object Persist1a {

  sealed trait Msg[R] extends ExpectingReply[R]
  final case class Get(key: String)(val replyTo: ActorRef[Option[String]]) extends Msg[Option[String]]
  final case class Cas(key: String, ov: String, nv: String)(val replyTo: ActorRef[Boolean]) extends Msg[Boolean]
  final case class PutIfAbsent(key: String, value: String)(val replyTo: ActorRef[Option[String]]) extends Msg[Option[String]]
  final case object Stop extends Msg[Nothing] {
    val replyTo: ActorRef[Nothing] = null // TODO
  }

  final case class Ev(key: String, newValue: String)

  final case class St(map: Map[String, String])

  val b: Behavior[Msg[_]] = EventSourcedBehavior.withEnforcedReplies[Msg[_], Ev, St](
    persistenceId = PersistenceId("foo"),
    emptyState = St(Map.empty),
    commandHandler = { (state, msg) =>
      msg match {
        case g @ Get(k) =>
          Effect.reply(g)(state.map.get(k))
        case c @ Cas(k, ov, nv) =>
          state.map.get(k) match {
            case None =>
              Effect.reply(c)(false)
            case Some(cv) =>
              if (cv == ov) {
                Effect.persist(Ev(k, nv)).thenReply(c)(_ => true)
              } else {
                Effect.reply(c)(false)
              }
          }
        case p @ PutIfAbsent(k, v) =>
          state.map.get(k) match {
            case None =>
              Effect.persist(Ev(k, v)).thenReply(p)(_ => None)
            case Some(cv) =>
              Effect.reply(p)(Some(cv))
          }
        case Stop =>
          Effect.stop().thenNoReply()
      }
    },
    eventHandler = { (state, ev) => state.copy(map = state.map + (ev.key -> ev.newValue)) }
  )
}

object Main {

  import Persist1a._

  implicit val to: Timeout = Timeout(1.second)

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem(Persist1a.b, "foo")
    implicit val sch: Scheduler = sys.scheduler
    implicit val ec: ExecutionContext = sys.executionContext
    val fut = for {
      opt <- sys ? PutIfAbsent("alma", "99")
      _ = assert(opt.isEmpty)
      gr <- sys ? Get("alma")
      _ = assert(gr == Some("99"), gr.toString)
      ok <- sys ? Cas("alma", "99", "100")
      _ = assert(ok)
    } yield ()
    try {
      scala.concurrent.Await.result(fut, atMost = 30.seconds)
    } finally {
      sys ! Stop
    }
  }
}
