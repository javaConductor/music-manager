package com.soulsys.music.processors.label

import java.util.concurrent.Executors

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.soulsys.music.RequestProcessor
import com.soulsys.music.domain.{Label, LabelRequest}

import scala.concurrent.{ExecutionContext, Future, Promise}

class LabelFinder extends GraphStage[FlowShape[LabelRequest, Label]] {
  val executorService = Executors.newFixedThreadPool(4)
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  val in = Inlet[LabelRequest]("labels.in")
  val out = Outlet[Label]("labels.out")
  val requestProcessor = RequestProcessor()
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    override def preStart(): Unit = {
      pull(in)
    }

    setHandlers(in, out, new InHandler with OutHandler {

      override def onPush(): Unit = {
        val request = grab(in)
        println(s"LabelFinder onPush grabbed : $request.")
        requestProcessor
          .processLabelRequest(request)
          .foreach[Unit]({
            case r: Label => {
              println(s"LabelFinder onPush pushing to: $out:  ${r.name}.")

              push(out, r)
              if (!hasBeenPulled(in)) {
                println(s"LabelFinder onPush pulling from $in.")
                pull(in)
              }
            }
          })

      }

      override def onPull(): Unit = {
        if (!hasBeenPulled(in)) {
          println(s"LabelFinder onPull: pulling.")
          pull(in)
        }
      }
    })
  }

  def process(rr:LabelRequest) : Future[Label] = {
    try {
      requestProcessor.processLabelRequest(rr)
    } catch {
      case t:Throwable =>
        println(s"Error: ${t.getLocalizedMessage}")
        Promise[Label].failure(t).future
    }
  }

}
