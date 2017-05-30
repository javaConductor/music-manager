package com.soulsys.music

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import akka.actor.ActorSystem
import akka.stream._
import com.soulsys.music.domain._
import com.soulsys.music.processors.label.LabelProcessor
import com.soulsys.music.processors.release.ReleaseProcessor

import scala.concurrent.ExecutionContext

/**
  * Created by lee on 5/16/17.
  */


object MusicProcessor {
}

class MusicProcessor {

  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()

  def start() = {
    /// create Qs

    val labelQ = new ArrayBlockingQueue[Label](1000)
    val releaseQ = new ArrayBlockingQueue[Release](1000)
    val labelRequestQ = new ArrayBlockingQueue[LabelRequest](1000)
    val releaseRequestQ = new ArrayBlockingQueue[ReleaseRequest](1000)
    val artistRequestQ = new ArrayBlockingQueue[ArtistRequest](1000)

    startLabelStream(labelRequestQ, releaseRequestQ, labelQ)
    //startArtistStream(artistQ, releaseQ)
    startReleaseStream(releaseRequestQ, labelRequestQ, artistRequestQ)

    //Thread.sleep(15000)
  }

  def startLabelStream(labelRequestQ: BlockingQueue[LabelRequest],
                       releaseQ: BlockingQueue[ReleaseRequest],
                       labelQ: BlockingQueue[Label) = {
    new LabelProcessor(labelRequestQ, releaseQ, labelQ).start()
    List("681","1060","12913","688","26972","37062")
      .map( (id) => LabelRequest( Some(id), None))
      .foreach( labelRequestQ put _)
  }

  def startArtistStream(artistQ: BlockingQueue[ArtistRequest], releaseQ: BlockingQueue[ReleaseRequest]) = {

  }


  def startReleaseStream(releaseQ: BlockingQueue[ReleaseRequest], labelQ:BlockingQueue[LabelRequest], artistQ: BlockingQueue[ArtistRequest]) = {
    List(
      ReleaseRequest(Some("2474325"), None),
      ReleaseRequest(Some("2632748"), None),
      ReleaseRequest(Some("134775"), None),
      ReleaseRequest(Some("79515"), None),
      ReleaseRequest(Some("1286951"), None),
      ReleaseRequest(Some("5743455"), None)
    ).foreach( releaseQ put _)
    new ReleaseProcessor( releaseQ, labelQ, artistQ).start()
  }

}
