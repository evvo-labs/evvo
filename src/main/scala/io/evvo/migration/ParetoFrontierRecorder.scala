package io.evvo.migration

import io.evvo.island.population.ParetoFrontier

trait ParetoFrontierRecorder[Sol] {

  def record(pf: ParetoFrontier[Sol])
}
