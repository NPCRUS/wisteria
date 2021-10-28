/*
    Wisteria, version 2.4.0. Copyright 2017-21 Jon Pretty, Propensive OÜ.

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

import wisteria.*
import gossamer.*

/** typeclass for providing a default value for a particular type */
trait HasDefault[T]:
  def defaultValue: Either[String, T]

/** companion object and derivation object for [[HasDefault]] */
object HasDefault extends Derivation[HasDefault]:

  /** constructs a default for each parameter, using the constructor default (if provided),
    *  otherwise using a typeclass-provided default */
  def join[T](ctx: CaseClass[HasDefault, T]): HasDefault[T] = new HasDefault[T] {
    def defaultValue = ctx.constructMonadic { param =>
      param.default match {
        case Some(arg) => Right(arg)
        case None => param.typeclass.defaultValue
      }
    }
  }

  /** chooses which subtype to delegate to */
  override def split[T](ctx: SealedTrait[HasDefault, T]): HasDefault[T] = new HasDefault[T]:
    def defaultValue = ctx.subtypes.headOption match
      case Some(sub) => sub.typeclass.defaultValue
      case None => Left("no subtypes")

  /** default value for a string; the empty string */
  given txt: HasDefault[Text] with
    def defaultValue = Right(t"")

  /** default value for ints; 0 */
  given int: HasDefault[Int] with { def defaultValue = Right(0) }

  /** oh, no, there is no default Boolean... whatever will we do? */
  given boolean: HasDefault[Boolean] with
    def defaultValue = Left("truth is a lie")

  /** default value for sequences; the empty sequence */
  given seq[A]: HasDefault[Seq[A]] with
    def defaultValue = Right(Seq.empty)
