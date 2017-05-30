package com.soulsys.music.persistence

import java.util.concurrent.Executors

import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.soulsys.music.domain.{Artist, DomainObject, Label, Release}
import com.soulsys.music.utils.copySyntax._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

/**
  * Created by lee on 5/15/17.
  */


class MusicRepository {
  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("musicDb")
  val releasesCollection = db("Releases")
  val labelCollection = db("Labels")
  val artistCollection = db("Artists")

  val executorService = Executors.newFixedThreadPool(4)
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  private class PersistenceException extends Exception {

  }

  def saveRelease(r: Release): Future[Release] = {
    println(s"saving release: $r")
    Future {
      val dbo = r.toDbObject
      val wr: WriteResult = releasesCollection.insert(dbo)
      r.copy(id = Some(dbo("_id").toString))
    }
  }

  def save[ValueType <: DomainObject](r: ValueType): Throwable \/ String = {
      val tname = r.getClass().getSimpleName
      println(s"saving $tname : $r")
      try {
        val dbo = r.toDbObject
        val wr: WriteResult = collection(r).insert(dbo)
        \/- (dbo("_id").toString)
      } catch {
        case t:Throwable =>  -\/ (t)
      }

  }

  def labelExists(discogsId: String): Boolean = {
    val q =  ( MongoDBObject("discogsId" -> discogsId)  )
    labelCollection.find( MongoDBObject("discogsId" -> discogsId)  )
    val labels = for(x<-labelCollection.find(q))
      yield x
    labels.hasNext
  }

  def collection[ValueType <: DomainObject](v: ValueType) = {
    v match {
      case x:Release => releasesCollection
      case x:Label => labelCollection
      case x:Artist => artistCollection
    }

  }

}
