Given an ADT such as,
```scala
enum Tree[+T]:
  case class Branch(left: Tree[T], right: Tree[T])
  case class Leaf(value: T)
```
and provided an given instance of `Show[Int]` is in scope, and a Wisteria derivation for the `Show` typeclass
has been provided, we can automatically derive given typeclass instances of `Show[Tree[Int]]` on-demand, like
so,
```scala
Branch(Branch(Leaf(1), Leaf(2)), Leaf(3)).show
```
Typeclass authors may provide Wisteria derivations in the typeclass's companion object, but it is easy to create
your own.

The definition of a `Show` typeclass with generic derivation defined with Wisteria might look like this:
```scala
import wisteria.*

trait Show[T]:
  def show(value: T): String

object Show extends Derivation[Show]:
  def join[T](ctx: CaseClass[Show, T]): Show[T] =
    ctx.params.map { p =>
      s"${p.label}=${p.typeclass.show(p.dereference(value))}"
    }.mkString("{", ",", "}")

  override def split[T](ctx: SealedTrait[Show, T]): Show[T] = value =>
    ctx.dispatch(value) { sub => sub.typeclass.show(sub.cast(value))
```

The `Derivation` trait provides a `derived` method which will attempt to construct a corresponding typeclass
instance for the type passed to it. Importing `Show.derived` as defined in the example above will make generic
derivation for `Show` typeclasses available in the scope of the import.

While any object may be used to define a derivation, if you control the typeclass you are deriving for, the
companion object of the typeclass is the obvious choice since it generic derivations for that typeclass will
be automatically available for consideration during contextual search.

### Limitations

Wisteria is not currently able to access default values for case class parameters.
