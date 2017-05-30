package com.soulsys.music.processors.label

import java.util.concurrent.BlockingQueue

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import com.soulsys.music.RequestProcessor
import com.soulsys.music.domain._
import com.soulsys.music.persistence.MusicRepository
import com.soulsys.music.processors.{MongoWriter, QueueSource}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/-}
import scala.concurrent.duration._
/**
  * Created by lee on 5/28/17.
  */
class LabelProcessor( labelRequestQ:BlockingQueue[LabelRequest],
                      releaseRequestQ:BlockingQueue[ReleaseRequest],
                      processedLabelQ: BlockingQueue[Label]
                     ) {

  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()

  def createSource(q : BlockingQueue[ LabelRequest ]) =
    new QueueSource[LabelRequest]( "labelRequest", q )

  def start(): Unit ={
    Future {
      while (true) {
        Thread.sleep(1000)
        val s = Source.fromIterator[LabelRequest](() => List(labelRequestQ.take()).iterator)
          .map((c) => {
            println(s"Request From Graph -> $c");
            c
          })
          .map[Label]((c: LabelRequest) => {
          Await.result(
            new RequestProcessor()
              .processLabelRequest(c), (1 minute ))
        })
          .filter(labelFilter)
          .map((c: Label) => {
            new MusicRepository().save(c) match {
              case -\/(t) => {
                println(s"Error ${t.getLocalizedMessage} saving label: $c")
                t.printStackTrace()
                throw t
              }
              case \/-(id) => {
                val saved = c.copy(id = Some(id))
                println(s"Label saved: ${saved.id} ${saved.name}")
                saved
              }
            }
          })
          .map((c) => {
            queueSubLabels(c.subLabels, labelRequestQ)
            println(s"Saved Label From Graph -> $c");
            c
          })
        val ret = s.runFold[List[Label]](List())((l, r) => r :: l)
          .onComplete {
            case Success(l) => {
              println(s"Processed: $l");

              l.reverse
            }
            case Failure(e) => {
              println(s"Error Not Processed: ${e.getLocalizedMessage}")
              Nil
            }
          }

      }
    }


  }


  def queueSubLabels(labels: List[SubLabel], labelQ: BlockingQueue[LabelRequest]) = {
    labels.foreach((lab) => {
      labelQ.put(LabelRequest(Some(lab.discogsId), None))
    }
    )
  }

  def queueReleases(label: Label, labelQ: BlockingQueue[LabelRequest]) = {
    labels.foreach ( (lab) => {
      labelQ.put( LabelRequest(Some(lab.discogsId), None) )
    }
    )


  }

  val musicRepository:MusicRepository = new MusicRepository
  /**
    * make sure label not already processed
    * @return true is  not yet processed
    */
  def labelFilter = (labelObj: Label ) => {
    !musicRepository.labelExists(labelObj.discogsId)
  }
}


