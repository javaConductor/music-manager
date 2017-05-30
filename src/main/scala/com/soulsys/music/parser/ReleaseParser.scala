package com.soulsys.music.parser

import com.soulsys.music.domain._
import org.json4s.JsonAST._

/**
  * Created by lee on 5/28/17.
  */
class ReleaseParser {
  private def parseReleaseTrack(artistFields: List[Tuple2[String, JValue]]): ReleaseTrack = {
    artistFields.foldLeft(ReleaseTrack("",List(),List(), "",""))((rt: ReleaseTrack, nameAndjvalue:Tuple2[String, JValue]) => {
      nameAndjvalue match {
        case ("title", JString(ttl)) => rt.copy(title  = ttl)
        case ("duration", JString(duration)) => rt.copy(durationStr  = duration)
        case ("position", JString(position)) => rt.copy(position = position)
        case ("artists", JArray(artistList)) =>
          //println(artistList.map(_.values))
          val artists = parseReleaseArtists( nameAndjvalue._2 )
          rt.copy(artists = artists)

        case ("extraartists", JArray(artistList)) =>
          //println(artistList.map(_.values))
          val artists = parseReleaseArtists( nameAndjvalue._2 )
          rt.copy(extraArtists = artists)

        case _ => rt
      }
    })
  }
  def parseTrackList(trackList: List[JValue]): List[ReleaseTrack] = {
    for {
      JObject(trackFields) <- trackList
    } yield parseReleaseTrack(trackFields)
  }

   def parseReleaseArtists(jsonArtists: JValue): List[ReleaseArtist] = {
    for {
      JArray(arr) <- jsonArtists
      JArray(artistFields) <- arr
    } yield parseReleaseArtist(new JArray(artistFields))
  }

  private def parseReleaseArtist(artistFields: JArray ): ReleaseArtist = {

    artistFields.values.foldLeft(ReleaseArtist(None, "", "", ""))((ra: ReleaseArtist, nameAndjvalue) => {
      nameAndjvalue match {
        case ("name", JString(nm)) => ra.copy(name = nm)
        case ("role", JString(role)) => ra.copy(role = role)
        case ("id", JInt(id)) => ra.copy(discogsId = id.toString)
        case _ => ra
      }
    })
  }

   def parseReleaseCompanies(jsonCompanies: List[JValue]): List[Company] = {
    for {
      JObject(companyFields) <- jsonCompanies
    } yield parseReleaseCompany(companyFields)
  }

  private def parseReleaseCompany(artistFields: List[Tuple2[String, JValue]]): Company = {
    artistFields.foldLeft(Company(None, "", "", "", "", ""))((co: Company, nameAndjvalue) => {
      nameAndjvalue match {
        case ("name", JString(nm)) => co.copy(name = nm)
        case ("entity_type", JString(entityType)) => co.copy(entityType = entityType)
        case ("entity_type_name", JString(entityTypeName)) => co.copy(entityTypeName = entityTypeName)
        case ("resource_url", JString(discogsUrl)) => co.copy(discogsUrl = discogsUrl)
        case ("id", JInt(id)) => co.copy(discogsId = id.toString)
        case _ => co
      }
    })
  }

   def parseReleaseLabels(jsonReleaseLabels: List[JValue]): List[ReleaseLabel] = {
    for {
      JObject(fields) <- jsonReleaseLabels
    } yield parseReleaseLabel(fields)
  }

  /**
    *
    * @param fields
    * @return
    */
  private def parseReleaseLabel(fields: List[Tuple2[String, JValue]]): ReleaseLabel = {
    fields.foldLeft(ReleaseLabel(None, "", ""))((rl: ReleaseLabel, nameAndjvalue) => {
      nameAndjvalue match {
        case ("name", JString(nm)) => rl.copy(name = nm)
        case ("id", JInt(id)) => rl.copy(discogsId = id.toString)
        case _ => rl
      }
    })
  }

}
