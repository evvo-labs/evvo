package com

package object diatom {
  type CreatorFunctionType[Sol] = () => Set[Sol]
  type MutatorFunctionType[Sol] = Set[TScored[Sol]] => Set[Sol]
  type DeletorFunctionType[Sol] = Set[TScored[Sol]] => Set[TScored[Sol]]
  type FitnessFunctionType[Sol] = Sol => Double
}