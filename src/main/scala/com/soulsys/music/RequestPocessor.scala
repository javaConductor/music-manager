package com.soulsys.music

import java.util.concurrent.Executors

import com.ning.http.client.{AsyncHttpClient, Response}
import com.soulsys.music.domain.{Label, LabelRequest, Release, ReleaseRequest}
import com.soulsys.music.parser.JsonParser

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object RequestProcessor{
  def apply() = new RequestProcessor
}

class RequestProcessor {
  
    //val http = HttpAsyncClientBuilder.create()
    val prefix = "https://api.discogs.com"
    val releasePrefix = prefix + "/releases/"
    val labelPrefix = prefix + "/labels/"
    val jsonParser = JsonParser()

    val executorService = Executors.newFixedThreadPool(4)
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  def processReleaseRequest(request:ReleaseRequest): Future[Release] = {
    val p = Promise[Release]();
    val url = request.fullUrl.orElse(Some(releasePrefix + request.d_releaseId.get)).get
    try {
      getRestContent(url).onComplete {
        case Success(releaseJson: String) =>
          responseToRelease(releaseJson) match {
            case Some(r) => p.success(r)
            case _ => println(s"Failed to parse: $releaseJson");p.failure(new UnknownError("Failed to parse."))
          }
          println(s"Found release: $releaseJson")
        case Failure(ex) =>
          p.failure(ex)
          println(s"ERROR finding release: ${ex.getMessage}")
      }
    }catch{
      case (t:Throwable) => p.failure(t)
    }
    p.future
  }
  
  def processLabelRequest(request:LabelRequest): Future[Label] = {
    val p = Promise[Label]();
    val url = request.fullUrl.orElse(Some(labelPrefix + request.d_labelId.get)).get
    try {
      getRestContent(url).onComplete {
        case Success(json: String) =>
          responseToLabel(json) match {
            case Some(l) => p.success(l)
            case _ => println(s"Failed to parse: $json");p.failure(new UnknownError("Failed to parse."))
          }
          println(s"Found label: $json")
        case Failure(ex) =>
          p.failure(ex)
          println(s"ERROR finding label: ${ex.getMessage}")
      }
    }catch{
      case (t:Throwable) => p.failure(t)
    }
    p.future
  }



  def responseToLabel(labelJson:String ): Option[Label] = jsonParser.responseToLabel(labelJson)
  def responseToRelease(releaseJson:String ): Option[Release] = jsonParser.responseToRelease(releaseJson)

  def getRestContent(url:String): Future[String] = {
    val p = Promise[String]()
    Future {
      val httpClient:AsyncHttpClient = new AsyncHttpClient()
      val r:Response = httpClient
        .prepareGet(url)
        .addHeader("User-Agent", "FooBarApp/3.0")
        .execute().get
      p.success( r.getResponseBody )
    }
    p.future
  }
    
}