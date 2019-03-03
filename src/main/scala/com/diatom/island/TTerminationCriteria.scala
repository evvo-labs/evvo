package com.diatom.island

import scala.concurrent.duration.Duration

trait TTerminationCriteria {
  def time: Duration
}

case class TerminationCriteria(time: Duration) extends TTerminationCriteria
