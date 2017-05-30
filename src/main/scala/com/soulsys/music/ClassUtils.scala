package com.soulsys.music

/**
  * Created by lee on 5/17/17.
  */
object ClassUtils {

  def toMap(o: Any): Map[String, Any] = {
    (Map[String, Any]() /: o.getClass.getDeclaredFields) {
      (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(o))
    }
  }

}
