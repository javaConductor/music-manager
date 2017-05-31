package com.soulsys.music.parser

import com.soulsys.music.domain.SubLabel
import org.json4s.JsonAST.{JArray, JInt, JString, JValue}

class LabelParser {
  def parseSubLabels(subLabels : JArray): scala.List[SubLabel] = {

    val sl = subLabels.arr.map((l) => {
//      println("l is "+l+ " \nl.children: "+l.children+ " \nl.values: "+l.values);
      parseSubLabel( l.values.asInstanceOf[Map[String, Any]] )}
    )// .values.map( (v) =>  {println(); parseSubLabel(v))})
    sl
  }

  def parseSubLabel(fields: JArray): SubLabel = {

    fields.values.foldLeft(SubLabel(None, "", ""))((subLabel: SubLabel, value) => {
      value match {
        case ("name", JString(nm)) => subLabel.copy(name = nm)
        case ("id", JInt(d_id)) => subLabel.copy(discogsId = d_id.toString)
        case _ => subLabel
      }
    })
  };

  def parseSubLabel(fields: Map[String, Any]): SubLabel = {

    val name:String = fields.getOrElse("name", "----").toString
    val id:String = fields.getOrElse("id", -1).toString
    SubLabel ( None, id, name)
  };

}

