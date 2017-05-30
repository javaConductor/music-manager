package com.soulsys.music.processors

/**
  * Created by lee on 5/15/17.
  */

import java.util.concurrent.Executors

import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.soulsys.music.domain.DomainObject
import com.soulsys.music.persistence.MusicRepository

import scala.concurrent.ExecutionContext
import scalaz.{-\/, \/-}

class MongoWriter[T <: DomainObject](name:String)
  extends GraphStage[SinkShape[T]] {
    final val in: Inlet[T] = Inlet.create(s"mongo_${name}_writer.in")
    val repo: MusicRepository = new MusicRepository

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      override def preStart(): Unit = { // initiate the flow of data by issuing a first pull on materialization:
        println(s"MongoWriter($name) preStart: pulling from $in")
        pull(in)
      }

      import akka.stream.stage.AbstractInHandler

      setHandler(in, new AbstractInHandler() {
        override def onPush(): Unit = { // We grab the element from the input port.
          val element = grab(in)
          pull(in)
          println(s"MongoWriter($name) onPush: grabbed: $element.")
          process(element)
          if (!hasBeenPulled(in)) {
            println(s"MongoWriter($name) onPush: pulling from $in")
            pull(in)
          }
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          println(s"MongoWriter($name) onUpstreamFailure")
          super.onUpstreamFailure(ex)
        }

        override def onUpstreamFinish(): Unit = {
          println(s"MongoWriter($name) onUpstreamFinish")
          super.onUpstreamFinish()
        }

      })
    }

    override def shape: SinkShape[T] = SinkShape.of(in)

    implicit val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

    def process(r: T ): Unit = {
      repo.save(r) match {

        case -\/(t) =>
          println(s"Error ${t.getLocalizedMessage} saving: $r")
          t.printStackTrace()

        case \/-(rel) =>
          println(s"Saved: $rel")
      }
    }
  }
