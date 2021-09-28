/*
    Wisteria, version 2.4.0. Copyright 2018-21 Jon Pretty, Propensive OÜ.

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

import scala.language.experimental.macros
import wisteria._

trait SemiDefault[A]:
  def default: A

object SemiDefault extends Derivation[SemiDefault]:
  inline def apply[A](using A: SemiDefault[A]): SemiDefault[A] = A

  def join[T](ctx: CaseClass[SemiDefault, T]): SemiDefault[T] = new SemiDefault[T]:
    def default = ctx.construct(p => p.default.getOrElse(p.typeclass.default))

  override def split[T](ctx: SealedTrait[SemiDefault, T]): SemiDefault[T] = new SemiDefault[T]:
    def default = ctx.subtypes.head.typeclass.default
  
  given SemiDefault[String] with
    def default = ""
  
  given SemiDefault[Int] with
    def default = 0
