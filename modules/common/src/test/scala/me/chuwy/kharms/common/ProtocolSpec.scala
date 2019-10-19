package me.chuwy.kharms.common

import org.specs2.mutable.Specification

class ProtocolSpec extends Specification {
  "Numbers" should {
    "add up" >> {
      val result = 1 + 1
      val expected = 2
      result must beEqualTo(expected)
    }
  }
}
