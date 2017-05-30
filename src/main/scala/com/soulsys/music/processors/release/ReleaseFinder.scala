package com.soulsys.music.processors.release

import java.util.concurrent.Executors

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.soulsys.music.RequestProcessor
import com.soulsys.music.domain.{Release, ReleaseRequest}

import scala.concurrent.{ExecutionContext, Future, Promise}

class ReleaseFinder extends GraphStage[FlowShape[ReleaseRequest, Release]] {
  val executorService = Executors.newFixedThreadPool(4)
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  val in = Inlet[ReleaseRequest]("releases.in")
  val out = Outlet[Release]("releases.out")
  val requestProcessor = RequestProcessor()
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    setHandlers(in, out, new InHandler with OutHandler {
      override def onDownstreamFinish(): Unit = {
        println(s"ReleaseFinder onDownstreamFinish.")

      }

      override def onUpstreamFinish(): Unit = {
        println(s"ReleaseFinder onUpstreamFinish.")
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        println(s"ReleaseFinder onUpstreamFailure.")

      }
      override def onPush(): Unit = {
        val request = grab(in)
        //pull(in)
        println(s"ReleaseFinder onPush grabbed : $request.")
        requestProcessor
          .processReleaseRequest(request)
          .foreach[Unit]({
          case r: Release => {
            println(s"ReleaseFinder onPush pushing to: $out:  $r.")
            push(out, r)
            complete(out)
            completeStage()
          }
          case _ => {
            println(s"ReleaseFinder onPush grabbed : $request but could not process.")
          }
        })
      }

      override def onPull(): Unit = {
        if (!hasBeenPulled(in) && !isClosed(in)) {
          println(s"ReleaseFinder onPull: pulling from: $in.")
          pull(in)
        }
      }
    })

    override def postStop(): Unit = {
      println(s"ReleaseFinder postStop.")
    }
  }

  def process(rr: ReleaseRequest): Future[Release] = {
    try {
      println(s"ReleaseFinder process: $rr.")
      requestProcessor.processReleaseRequest(rr)
    } catch {
      case t: Throwable =>
        println(s"Error: ${t.getLocalizedMessage}")
        Promise[Release].failure(t).future
    }
  }

}
