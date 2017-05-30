package com.soulsys.music.domain

case class ReleaseRequest(d_releaseId: Option[String], fullUrl:Option[String])
case class ArtistRequest(d_artistId: Option[String], fullUrl:Option[String])
case class LabelRequest(d_labelId: Option[String], fullUrl:Option[String])
