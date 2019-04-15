package com.example

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import akka.actor.typed._
import akka.actor.Scheduler
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.AskPattern._
import akka.persistence.typed.scaladsl._
import akka.persistence.typed._
import akka.cluster.typed.{ Cluster, Join }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey, EntityRef }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.util.Timeout

object Shard1a {

  sealed trait Msg[R] extends ExpectingReply[R]
  final case class Get(key: String)(val replyTo: ActorRef[Option[String]]) extends Msg[Option[String]]
  final case class Cas(key: String, ov: String, nv: String)(val replyTo: ActorRef[Boolean]) extends Msg[Boolean]
  final case class PutIfAbsent(key: String, value: String)(val replyTo: ActorRef[Option[String]]) extends Msg[Option[String]]
  final case object Stop extends Msg[Nothing] {
    val replyTo: ActorRef[Nothing] = null // TODO
  }

  final case class Ev(key: String, newValue: String)

  final case class St(map: Map[String, String])

  def b(entityId: String): Behavior[Msg[_]] = EventSourcedBehavior.withEnforcedReplies[Msg[_], Ev, St](
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
          println(p)
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

object MainShard {

  import Shard1a._

  implicit val to: Timeout = Timeout(5.second)

  val root: Behavior[Stop.type] = Behaviors.receive { (ctx, _) =>
    Behavior.stopped
  }

  val typeKey = EntityTypeKey[Msg[_]]("kvstore")

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem(root, "bar")
    Cluster(sys).manager ! Join(Cluster(sys).selfMember.address)
    val sha = ClusterSharding(sys)
    val shardRegion = sha.init(Entity(typeKey, ectx => Shard1a.b(ectx.entityId)))
    val ref: EntityRef[Msg[_]] = sha.entityRefFor(typeKey, entityId = "myNamespace")
    implicit val sch: Scheduler = sys.scheduler
    implicit val ec: ExecutionContext = sys.executionContext
    val fut = for {
      opt <- shardRegion ? { r: ActorRef[Option[String]] =>
        ShardingEnvelope("myNamespace", PutIfAbsent("alma", "99")(r))
      }
      _ = assert(opt.isEmpty)
      gr <- ref ? Get("alma")
      _ = assert(gr == Some("99"), gr.toString)
      ok <- ref ? Cas("alma", "99", "100")
      _ = assert(ok)
      gr <- shardRegion ? { r: ActorRef[Option[String]] =>
        ShardingEnvelope("myNamespace", Get("alma")(r))
      }
      _ = assert(gr == Some("100"), gr.toString)
    } yield ()
    try {
      scala.concurrent.Await.result(fut, atMost = 30.seconds)
    } finally {
      sys ! Stop
    }
  }
}
