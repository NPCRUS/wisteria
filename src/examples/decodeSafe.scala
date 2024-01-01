/*
    Wisteria, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

import wisteria._

import gossamer.*

import unsafeExceptions.canThrowAny

/** decoder for converting strings to other types providing good error messages */
trait DecoderSafe[T] { def decode(str: String): Either[String, T] }

/** derivation object (and companion object) for [[DecoderSafe]] instances */
object DecoderSafe extends Derivation[DecoderSafe]:

  /** decodes strings */
  given DecoderSafe[String] = Right(_)

  /** decodes ints */
  given DecoderSafe[Int] = k =>
    try Right(k.toInt) catch case _: NumberFormatException => Left(s"illegal number: $k")

  /** defines how new [[DecoderSafe]]s for case classes should be constructed */
  def join[T](ctx: CaseClass[DecoderSafe, T]): DecoderSafe[T] = value =>
    val (_, values) = parse(value)
    
    ctx.constructEither { param =>
      param.typeclass.decode(values(param.label))
    }.left.map(_.reduce(_+"\n"+_))

  /** defines how to choose which subtype of the sealed trait to use for decoding */
  override def split[T](ctx: SealedTrait[DecoderSafe, T]): DecoderSafe[T] = param =>
    val (name, _) = parse(param)
    val subtype = ctx.subtypes.find(_.typeInfo.full == name).get
    
    subtype.typeclass.decode(param)

  /** very simple extractor for grabbing an entire parameter value, assuming matching parentheses */
  private def parse(value: String): (String, Map[String, String]) =
    val end = value.indexOf('(')
    val name = value.substring(0, end).nn

    def parts(value: String, idx: Int = 0, depth: Int = 0, collected: List[String] = List(""))
        : List[String] =
      def plus(char: Char): List[String] = collected.head + char :: collected.tail

      if idx == value.length then collected else value(idx) match
        case '('  => parts(value, idx + 1, depth + 1, plus('('))
        case ')'  => if depth == 1 then plus(')') else parts(value, idx + 1, depth - 1, plus(')'))
        case ','  => if depth == 0 then parts(value, idx + 1, depth, "" :: collected)
                     else parts(value, idx + 1, depth, plus(','))
        case char => parts(value, idx + 1, depth, plus(char))

    def keyValue(str: String): (String, String) =
      val List(label, value) = str.show.cut(t"=", 2).to(List).map(_.s)
      (label, value)

    (name, parts(value.substring(end + 1, value.length - 1).nn).map(keyValue).to(Map))
