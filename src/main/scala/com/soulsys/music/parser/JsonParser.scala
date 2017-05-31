package com.soulsys.music.parser

import com.soulsys.music.domain._
import org.json4s.JsonAST.{JValue, _}
import org.json4s.StringInput
import org.json4s.jackson.JsonMethods

object JsonParser {
  def apply() = new JsonParser
}

class JsonParser {

  val releaseParser = new ReleaseParser
  val labelParser = new LabelParser

  def responseToLabel(labelJson: String): Option[Label] = {
    val json: JValue = JsonMethods.parse(new StringInput(labelJson), false)

    val subLabels = (json \ "sublabels") match {
      case ja:JArray => labelParser.parseSubLabels(ja)
      case _ => List()
    }

     val profile: List[String] = for {
      JObject(rObj) <- json
      JField("profile", JString(sprofile)) <- rObj
    } yield sprofile

    val contactInfo: List[String] = for {
      JObject(rObj) <- json
      JField("contact_info", JString(cInfo)) <- rObj
    } yield cInfo

    val urls: List[String] = for {
      JObject(rObj) <- json
      JField("urls", JArray(urls)) <- rObj
      url <- toStringList(urls)
    } yield url


    (for {
      JObject(rObj) <- json
      JField("id", JInt(d_id)) <- rObj
      //JField("profile", JString(profile)) <- rObj
      JField("name", JString(name)) <- rObj
      JField("uri", JString(d_uri)) <- rObj
    //JField("sublabels", JArray(sublabels)) <- rObj
    //JField("contact_info", JString(contactInfo)) <- rObj
    //      JField("urls", JArray(urls)) <- rObj
    } yield Label(None, d_id.toString, name, d_uri, contactInfo.headOption,
      subLabels,
      urls, List(), profile.headOption
    )
      ) match {
      case Nil => None
      case List(l: Label) => Some(l)
    }
  }

  def responseToRelease(releaseJson: String): Option[Release] = {
    val json: JValue = JsonMethods.parse(new StringInput(releaseJson), false)

    (for {
      JObject(rObj) <- json
      JField("id", JInt(d_release_id)) <- rObj
      JField("master_id", JInt(d_main_release_id)) <- rObj
      JField("year", JInt(year)) <- rObj
      JField("title", JString(title)) <- rObj
      JField("country", JString(country)) <- rObj
      JField("genres", JArray(genres)) <- rObj
      JField("styles", JArray(styles)) <- rObj
      JField("formats", JArray(formats)) <- rObj
      JField("artists", JArray(artists)) <- rObj
      JField("extraartists", JArray(extraartists)) <- rObj
      JField("companies", JArray(companies)) <- rObj
      JField("labels", JArray(labels)) <- rObj
      JField("tracklist", JArray(trackList)) <- rObj
    } yield Release(None,
      d_release_id.toString,
      title,
      toStringList(styles),
      toStringList(genres),
      d_main_release_id.toString,
      releaseParser.parseReleaseArtists(new JArray(artists)),
      releaseParser.parseReleaseArtists(new JArray(extraartists)),
      releaseParser.parseReleaseCompanies(companies),
      releaseParser.parseReleaseLabels(labels),
      releaseParser.parseTrackList(trackList), year.intValue()
    )
      ) match {
      case Nil => None
      case List(l: Release) => Some(l)
    }
  }

  def toStringList(l: List[JValue]): List[String] = l.map(_.values.toString)

}