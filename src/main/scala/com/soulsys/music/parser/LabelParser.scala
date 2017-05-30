package com.soulsys.music.parser

import com.soulsys.music.domain.SubLabel
import org.json4s.JsonAST.{JArray, JInt, JString, JValue}

class LabelParser {
  def parseSubLabels(subLabels : JValue): scala.List[SubLabel] = {
    for {
      JArray(arr) <- subLabels
      JArray(slFields) <- arr
    } yield parseSubLabel(new JArray(slFields))


  }

  def parseSubLabel(fields: JArray): SubLabel = {

    fields.values.foldLeft(SubLabel(None, "", ""))((subLabel: SubLabel, nameAndjvalue) => {
      nameAndjvalue match {
        case ("name", JString(nm)) => subLabel.copy(name = nm)
        case ("id", JInt(d_id)) => subLabel.copy(discogsId = d_id.toString)
        case _ => subLabel
      }
    })
  }

}

