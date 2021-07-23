/*
    Wisteria, version 2.0.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package wisteria.examples

import wisteria._

class ExportedTypeclass[T]()

object ExportedTypeclass extends Derivation[ExportedTypeclass]:
  case class Exported[T]() extends ExportedTypeclass[T]
  def join[T](ctx: CaseClass[Typeclass, T]): Exported[T] = Exported()
  override def split[T](ctx: SealedTrait[Typeclass, T]): Exported[T] = Exported()

  given Typeclass[Int] = new ExportedTypeclass()
  given Typeclass[String] = new ExportedTypeclass()
  given seqInstance[T: Typeclass]: Typeclass[Seq[T]] = new ExportedTypeclass()
