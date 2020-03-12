package io.evvo.island

import java.util.UUID

import com.typesafe.scalalogging.Logger

// TODO: This implementation assumes we only have one island per JVM, is that a fair assumption?
object utilities {
  val islandId: UUID = UUID.randomUUID()
  val log: Logger = Logger(utilities.islandId.toString.substring(0, 7))
}
