package com

package object diatom {
  type CreatorFunc[Sol] = () => Set[Sol]
  type MutatorFunc[Sol] = Set[TScored[Sol]] => Set[Sol]
  type DeletorFunc[Sol] = Set[TScored[Sol] => TScored[Sol]]
}