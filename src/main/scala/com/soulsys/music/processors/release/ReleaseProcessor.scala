package com.soulsys.music.processors.release

import java.util.concurrent.BlockingQueue

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import com.soulsys.music.RequestProcessor
import com.soulsys.music.domain._
import com.soulsys.music.persistence.MusicRepository
import com.soulsys.music.processors.QueueSource

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/-}
import scala.concurrent.duration._

/**
  * Created by lee on 5/28/17.
  */
class ReleaseProcessor(releaseQ: BlockingQueue[ReleaseRequest],
                       labelQ: BlockingQueue[LabelRequest],
                       artistQ: BlockingQueue[ArtistRequest]) {

  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()

  def createSource(q: BlockingQueue[ReleaseRequest]) =
    new QueueSource[ReleaseRequest]("release", q)

  def queueLabels(labels: List[ReleaseLabel], labelQ: BlockingQueue[LabelRequest]) = {
    labels.foreach ( (lab) => {
      labelQ.put( LabelRequest(Some(lab.discogsId), None) )
    }
    )


  }

  def start() = {
    Future {
      while (true) {
        Thread.sleep(1000)
        val s = Source.fromIterator[ReleaseRequest](() => List(releaseQ.take()).iterator)
          .map((c) => {
          println(s"Request From Graph -> $c");
          c
        })
          .map[Release]((c: ReleaseRequest) => {
          Await.result(
            new RequestProcessor()
              .processReleaseRequest(c), (1 minute ))
          })
          .map((c: Release) => {
                new MusicRepository().save(c) match {
                  case -\/(t) => {
                    println(s"Error ${t.getLocalizedMessage} saving release: $c")
                    t.printStackTrace()
                    throw t
                  }
                  case \/-(id) => {
                    val saved = c.copy(id = Some(id))
                    println(s"Release saved: ${saved.id} ${saved.title}")
                    saved
                  }
                }
              })
          .map((c) => {
                        queueLabels(c.labels, labelQ)
                        println(s"Queued Labels from Release From Graph -> $c");
                        c
          })
        val ret = s.runFold[List[Release]](List())((l, r) => r :: l)
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

    def releaseFilter = (releaseObj: Release) => {

      ///// do the MUST-HAVEs
      val styles: List[String] = releaseObj.styles
      val genres: List[String] = releaseObj.genres

      val badYear = releaseObj.year < 1971 || (releaseObj.year > 1983)

      val isDisco = (styles.isEmpty ||
        !styles.intersect(List("Soul", "Disco", "Funk", "Rhythm & Blues")).isEmpty &&
          !genres.intersect(List("Funk / Soul", "Electronic")).isEmpty)

      val isReggae = styles.size == 1 && styles.head == "Reggae" &&
        !genres.intersect(List("Reggae")).isEmpty

      val isJazzFunk = !styles.intersect(List("Soul-Jazz", "Disco", "Jazz-Funk", "Funk")).isEmpty && !genres.intersect(List("Funk / Soul", "Electronic")).isEmpty
      val isFunkyStuff = isDisco || isJazzFunk //|| isReggae

      ///// do the exclusions
      val isRap = !styles.intersect(List(
        "Thug Rap",
        "Gansta",
        "Pop Rap",
        "Hip Hop",
        "RnB/Swing"
      )).isEmpty ||
        (!genres.intersect(List("Hip Hop")).isEmpty)

      val isCountry = !styles.intersect(List(
        "Country",
        "Schlager",
        "Country Rock"
      )).isEmpty

      val isRock = !styles.intersect(List(
        "Heavy Metal",
        "Hard Rock",
        "Glam",
        "Arena Rock",
        "Rock & Roll"
      )).isEmpty &&
        !genres.intersect(List("Rock")).isEmpty

      (isFunkyStuff && !badYear) || isDisco //|| (!(isRap || isCountry || isRock))
    }
  }


