package com.soulsys.music.processors

/**
  * Created by lee on 5/28/17.
  */
import java.util.concurrent.BlockingQueue

import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.AbstractOutHandler
import akka.stream.stage.GraphStage
import akka.stream.stage.GraphStageLogic

class QueueSource[T](name: String, q: BlockingQueue[T]) extends GraphStage[SourceShape[T]] {
  final val out: Outlet[T] = Outlet.create(s"$name.queue.out")
  override def shape: SourceShape[T] = SourceShape.of(out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    {
      setHandler(out, new AbstractOutHandler() {
        @Override
        def onPull() {
          println(s"QueueSource($name).onPull. QSize:${q.size()}")
          val v:T = q.take()
          println(s"QueueSource($name).onPull: pushing to $out : $v.")
          push(out, v)
        }

        override def onDownstreamFinish(): Unit = {
          println(s"QueueSource($name).onDownstreamFinish")
          super.onDownstreamFinish()
        }

      })
    }
  }
}