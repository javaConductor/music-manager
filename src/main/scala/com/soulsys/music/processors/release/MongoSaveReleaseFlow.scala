package com.soulsys.music.processors.release

/**
  * Created by lee on 5/15/17.
  */

import java.util.concurrent.Executors

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream._
import com.soulsys.music.domain.Release
import com.soulsys.music.persistence.MusicRepository

import scala.concurrent.ExecutionContext
import scalaz.{-\/, \/-}

final class MongoSaveReleaseFlow()
  extends GraphStage[FlowShape[ Release , Release]] {
  val in: Inlet[Release] = Inlet.create("mongo_release.in")
  val out: Outlet[Release] = Outlet.create("mongo_release.out")
  val repo: MusicRepository = new MusicRepository

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    override def preStart(): Unit = { // initiate the flow of data by issuing a first pull on materialization:
      println("MongoSaveReleaseFlow preStart: pulling")
      pull(in)
    }

    import akka.stream.stage.AbstractInHandler

    setHandler(in, new AbstractInHandler() with OutHandler {
      override def onPush(): Unit = { // We grab the element from the input port.
        val element = grab(in)
        println("MongoSaveReleaseFlow onPush: grabbed.")
        push(out, process(element))
        complete(out)
//
//        if (!hasBeenPulled(in) && !isClosed(in)) {
//          println("MongoSaveReleaseFlow onPush: pulling")
//          pull(in)
//        }
      }

      override def onPull(): Unit = {
        println(s"MongoSaveReleaseFlow onPull : pulling from $in")
        pull(in)
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        println("MongoSaveReleaseFlow onUpstreamFailure")
        super.onUpstreamFailure(ex)
      }

      override def onUpstreamFinish(): Unit = {
        println("MongoSaveReleaseFlow onUpstreamFinish")
        super.onUpstreamFinish()
      }

    })
  }

  override def shape: FlowShape[ Release, Release ] = FlowShape.of(in, out)

  val executorService = Executors.newFixedThreadPool(4)
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  def process(r: Release): Release  = {
    println(s"process(): Saving release: $r")
      val saved = repo.save(r) match {

        case -\/(t) => {
          println(s"Error ${t.getLocalizedMessage} saving release: $r")
          t.printStackTrace()
          throw t
        }
        case \/-(id) => {
          println(s"Release saved: $r")
          List(r.copy(id = Some(id)) )
        }
      }
    saved.head
  }
}
