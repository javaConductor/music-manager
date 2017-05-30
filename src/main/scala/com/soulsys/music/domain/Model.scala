package com.soulsys.music.domain

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.soulsys.music.ClassUtils

sealed trait DomainObject {
  def asMap: Map[String, Any] = ClassUtils.toMap(this)

  def toDbObject: DBObject = {
    val builder = MongoDBObject.newBuilder
    asMap.foldLeft(builder)((bldr, fldValue) =>
      // handle nested
      if (fldValue._2.isInstanceOf[DomainObject]) {
        bldr += (fldValue._1 -> fldValue._2.asInstanceOf[DomainObject].toDbObject)
      } else if (fldValue._2.isInstanceOf[List[Any]] && (
        !(fldValue._2.asInstanceOf[List[Any]].isEmpty) &&
          fldValue._2.asInstanceOf[List[Any]](0).isInstanceOf[DomainObject])) {
        val arr = fldValue._2.asInstanceOf[List[Any]].map(_.asInstanceOf[DomainObject].toDbObject)
        bldr += (fldValue._1 -> arr.asInstanceOf[Any])
      }
      else
        bldr += fldValue
    ).result()
  }
}

case class Release(id: Option[String], discogsId: String, title: String,
                   styles: List[String], genres: List[String],
                   main_release: String, artist: List[ReleaseArtist], extraArtist: List[ReleaseArtist],
                   companies: List[Company], labels: List[ReleaseLabel],
                   trackList: List[ReleaseTrack], year: Int) extends DomainObject

case class ArtistMember(artistId: String, name: String, role: String) extends DomainObject

case class ReleaseArtist(artistId: Option[String], discogsId: String, name: String, role: String) extends DomainObject

case class ReleaseTrack(title: String,
                        artists: List[ReleaseArtist],
                        extraArtists: List[ReleaseArtist],
                        durationStr: String,
                        position: String) extends DomainObject

case class ReleaseLabel(companyId: Option[String], discogsId: String,
                        name: String) extends DomainObject

case class ReleaseFormat(name: String, descriptions: List[String],
                         qty: Int) extends DomainObject

case class Artist(id: Option[String], name: String, discogsId: String,
                  d_releases_url: String, members: List[ArtistMember],
                  imageUrls: List[String], profile: String) extends DomainObject

case class Label(id: Option[String], discogsId: String, name: String, uri: String,
                 contactInfo: Option[String], subLabels: List[SubLabel],
                 additionalUrls: List[String],
                 imageUrls: List[String], profile: Option[String]) extends DomainObject

case class SubLabel(id: Option[String], discogsId: String, name: String) extends DomainObject

case class RejectedReleases(releaseIds: List[String]) extends DomainObject

case class Company(id: Option[String], discogsId: String,
                   name: String, entityTypeName: String, entityType: String,
                   discogsUrl: String) extends DomainObject
