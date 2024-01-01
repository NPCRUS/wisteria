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

package wisteria

import rudiments.*

import scala.deriving.Mirror
import scala.compiletime.*
import scala.reflect.*
import Wisteria.*

trait CommonDerivation[TypeClass[_]]:
  type Typeclass[T] = TypeClass[T]
  def join[T](ctx: CaseClass[Typeclass, T]): Typeclass[T]

  inline def derivedMirrorProduct[A](product: Mirror.ProductOf[A]): Typeclass[A] =
    val parameters = IArray(getParams[A, product.MirroredElemLabels, product.MirroredElemTypes](
        paramAnns[A].to(Map), paramTypeAnns[A].to(Map), repeated[A].to(Map))*)
    
    val caseClass = new CaseClass[Typeclass, A](typeInfo[A], isObject[A], isValueClass[A],
        parameters, IArray(anns[A]*), IArray[Any](typeAnns[A]*)):
      
      def construct[PType](makeParam: Param => PType)(using ClassTag[PType]): A =
        product.fromProduct(Tuple.fromArray(this.params.map(makeParam(_)).to(Array)))

      def rawConstruct(fieldValues: Seq[Any]): A =
        product.fromProduct(Tuple.fromArray(fieldValues.to(Array)))

      def constructEither[Err, PType: ClassTag]
                         (makeParam: Param => Either[Err, PType]): Either[List[Err], A] =
        params.map(makeParam(_)).to(Array).foldLeft[Either[List[Err], Array[PType]]]
            (Right(Array())) {
          case (Left(errs), Left(err))    => Left(errs ++ List(err))
          case (Right(acc), Right(param)) => Right(acc ++ Array(param))
          case (errs@Left(_), _)          => errs
          case (_, Left(err))             => Left(List(err))
        }.map { params => product.fromProduct(Tuple.fromArray(params)) }

      def constructMonadic[M[_]: Monadic, PType](using ClassTag[PType])(makeParam: Param => M[PType]): M[A] =
        summon[Monadic[M]].map {
          params.map(makeParam(_)).to(Array).foldLeft(summon[Monadic[M]].point(Array())) {
            (accM, paramM) => summon[Monadic[M]].flatMap(accM) { acc =>
              summon[Monadic[M]].map(paramM)(acc ++ List(_))
            }
          }
        } { params => product.fromProduct(Tuple.fromArray(params)) }

    join(caseClass)

  inline def getParams[T, Labels <: Tuple, Params <: Tuple]
                      (annotations: Map[String, List[Matchable]], typeAnnotations: Map[String, List[Any]],
                          repeated: Map[String, Boolean], idx: Int = 0)
                      : List[CaseClass.Param[Typeclass, T]] =

    inline erasedValue[(Labels, Params)] match
      case _: (EmptyTuple, EmptyTuple) =>
        Nil
      
      case _: ((l *: ltail), (p *: ptail)) =>
        val label = constValue[l].asInstanceOf[String]
        val typeclass = CallByNeed(summonInline[Typeclass[p]])

        CaseClass.Param[Typeclass, T, p](label, idx, repeated.getOrElse(label, false), typeclass,
            CallByNeed(None), IArray.from(annotations.getOrElse(label, List())),
            IArray.from(typeAnnotations.getOrElse(label, List()))) ::
            getParams[T, ltail, ptail](annotations, typeAnnotations, repeated, idx + 1)

trait ProductDerivation[TypeClass[_]] extends CommonDerivation[TypeClass]:
  inline def derivedMirror[A](using mirror: Mirror.Of[A]): Typeclass[A] = inline mirror match
    case product: Mirror.ProductOf[A] => derivedMirrorProduct[A](product)

  inline given derived[A](using Mirror.Of[A]): Typeclass[A] = derivedMirror[A]

trait Derivation[TypeClass[_]] extends CommonDerivation[TypeClass]:
  def split[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T]

  transparent inline def subtypes[T, SubtypeTuple <: Tuple](sum: Mirror.SumOf[T], idx: Int = 0)
      : List[SealedTrait.Subtype[Typeclass, T, ?]] =
    inline erasedValue[SubtypeTuple] match
      case _: EmptyTuple =>
        Nil
      
      case _: (s *: tail) =>
        new SealedTrait.Subtype(typeInfo[s], IArray[Matchable](), IArray.from(paramTypeAnns[T]), idx,
            CallByNeed(summonInline[Typeclass[s]]), x => sum.ordinal(x) == idx,
            _.asInstanceOf[s & T]) :: subtypes[T, tail](sum, idx + 1)

  inline def derivedMirrorSum[A](sum: Mirror.SumOf[A]): Typeclass[A] =
    val sealedTrait = SealedTrait(typeInfo[A], IArray(subtypes[A, sum.MirroredElemTypes](sum)*),
        IArray[Matchable](), IArray(paramTypeAnns[A]*))
    
    split(sealedTrait)

  inline def derivedMirror[A](using mirror: Mirror.Of[A]): Typeclass[A] = inline mirror match
    case sum: Mirror.SumOf[A]         => derivedMirrorSum[A](sum)
    case product: Mirror.ProductOf[A] => derivedMirrorProduct[A](product)

  inline given derived[A](using Mirror.Of[A]): Typeclass[A] = derivedMirror[A]
