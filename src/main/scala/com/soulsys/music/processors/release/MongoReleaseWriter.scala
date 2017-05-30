package com.soulsys.music.processors.release

/**
  * Created by lee on 5/15/17.
  */

import java.util.concurrent.Executors

import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.soulsys.music.domain.Release
import com.soulsys.music.persistence.MusicRepository

import scala.concurrent.ExecutionContext
import scalaz.{-\/, \/-}

final class MongoReleaseWriter()
  extends GraphStage[SinkShape[Release]] {
  val in: Inlet[Release] = Inlet.create("mongo_release.in")
  val repo: MusicRepository = new MusicRepository

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    override def preStart(): Unit = { // initiate the flow of data by issuing a first pull on materialization:
      println("MongoReleaseWriter preStart: pulling")
      pull(in)
    }

    import akka.stream.stage.AbstractInHandler

    setHandler(in, new AbstractInHandler() {
      override def onPush(): Unit = { // We grab the element from the input port.
        val element = grab(in)
        println("MongoReleaseWriter onPush: grabbed.")
        process(element)
        if (!hasBeenPulled(in)) {
          println("MongoReleaseWriter onPush: pulling")
          pull(in)
        }
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        println("MongoReleaseWriter onUpstreamFailure")
        super.onUpstreamFailure(ex)
      }

      override def onUpstreamFinish(): Unit = {
        println("MongoReleaseWriter onUpstreamFinish")
        super.onUpstreamFinish()
      }

    })
  }

  override def shape: SinkShape[Release] = SinkShape.of(in)

  val executorService = Executors.newFixedThreadPool(4)
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  def process(r: Release): Unit = {
    println(s"process(): Saving release: $r")
    repo.save(r) match {

      case -\/(t) => {
        println(s"Error ${t.getLocalizedMessage} saving release: $r")
        t.printStackTrace()
      }
      case \/-(rel) => {
        println(s"Release saved: $rel")
      }
    }
  }
}
