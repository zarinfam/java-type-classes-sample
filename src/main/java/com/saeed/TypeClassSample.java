package com.saeed;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.saeed.Empty.emptyInt;
import static com.saeed.Functions.*;
import static com.saeed.JsonWriter.personWriter;
import static com.saeed.Monoid.*;
import static com.saeed.Show.*;

/**
 * @author Saeed Zarinfam
 */
public class TypeClassSample {

    public static void main(String[] args) {

        var xAndY = twoEmpties(emptyInt());
        System.out.println(show(xAndY, showTuple()));
        System.out.println(show(xAndY, Tuple2::toString));
        System.out.println(show(12, showInt()));

        var ints = List.of(1, 2, 3);
        System.out.println(combineAll(ints, intAdditionMonoid()));

        var pairs = List.of(new Pair<>(1, "hello"), new Pair<>(2, " "), new Pair<>(3, "world"));
        System.out.println(show(combineAll(pairs, deriveMonoidPair(intAdditionMonoid(), stringConcatMonoid())), showPair(showInt(), showString())));

        var mPairs = List.of(
                new MPair<>(1, "hello", intAdditionMonoid(), stringConcatMonoid())
                , new MPair<>(2, " ", intAdditionMonoid(), stringConcatMonoid())
                , new MPair<>(3, "world", intAdditionMonoid(), stringConcatMonoid()));
        combineAll(mPairs)
                .ifPresent(v -> System.out.println(show(v, showMPair(showInt(), showString()))));

        var p = new Person("Saeed", "zarinfam.s@gmail.com");
        var json = toJson(p, personWriter()).as(JsObject.class);
        System.out.println(json.get().get("name").as(JsString.class).get());

        System.out.println(stringConcatMonoid().combine("x", stringConcatMonoid().combine("y", "z")));
        System.out.println(stringConcatMonoid().combine(stringConcatMonoid().combine("x", "y"), "z"));

    }

}
interface JsonWriter<A> {
    Json write(A value);

    static JsonWriter<String> stringWriter() {
        return JsString::new;
    }

    static JsonWriter<Person> personWriter() {
        return v -> new JsObject(Map.of("name", new JsString(v.getName()), "email", new JsString(v.getEmail())));
    }
}
interface Empty<A> {
    A empty();

    static Empty<Integer> emptyInt() {
        return () -> 0;
    }

    static Empty<String> emptyString() {
        return () -> "";
    }
}


interface Monoid<A> extends Empty<A> {
    A combine(A x, A y);

    static Monoid<Integer> intAdditionMonoid() {
        return new Monoid<>() {
            @Override
            public Integer combine(Integer x, Integer y) {
                return x + y;
            }

            @Override
            public Integer empty() {
                return emptyInt().empty();
            }
        };
    }

    static Monoid<String> stringConcatMonoid() {
        return new Monoid<>() {
            @Override
            public String combine(String x, String y) {
                return x + y;
            }

            @Override
            public String empty() {
                return Empty.emptyString().empty();
            }
        };
    }

    static <A, B> Monoid<Pair<A, B>> deriveMonoidPair(Monoid<A> A, Monoid<B> B) {
        return new Monoid<>() {
            @Override
            public Pair<A, B> combine(Pair<A, B> x, Pair<A, B> y) {
                return new Pair<>(A.combine(x.getFirst(), y.getFirst()), B.combine(x.getSecond(), y.getSecond()));
            }

            @Override
            public Pair<A, B> empty() {
                return new Pair<>(A.empty(), B.empty());
            }
        };
    }

}

interface Show<A> {
    String show(A a);

    static <X, Y> Show<Tuple2<X, Y>> showTuple() {
        return t -> "{" + t._1() + ", " + t._2() + "}";
    }

    static Show<Integer> showInt() {
        return i -> "int: " + i;
    }

    static Show<String> showString() {
        return i -> "str: " + i;
    }

    static <A, B> Show<Pair<A, B>> showPair(Show<A> A, Show<B> B) {
        return p -> "{" + A.show(p.getFirst()) + ", " + B.show(p.getSecond()) + "}";
    }

    static <A, B> Show<MPair<A, B>> showMPair(Show<A> A, Show<B> B) {
        return p -> "{" + A.show(p.getFirst()) + ", " + B.show(p.getSecond()) + "}";
    }
}

//class IntAdditionMonoid implements Monoid<Integer> {
//
//    static IntAdditionMonoid of() {
//        return new IntAdditionMonoid();
//    }
//
//    @Override
//    public Integer empty() {
//        return 0;
//    }
//
//    @Override
//    public Integer combine(Integer x, Integer y) {
//        return x + y;
//    }
//}

final class Functions {
    static <T> Tuple2<T, T> twoEmpties(Empty<T> empty) {
        return Tuple.of(empty.empty(), empty.empty());
    }

    static <A> String show(A a, Show<A> sh) {
        return sh.show(a);
    }

    static int sumInts(List<Integer> list) {
        return list.stream().reduce(0, Integer::sum);
    }

    static String concatStrings(List<String> list) {
        return list.stream().reduce("", String::concat);
    }

    static <T> Set<T> unionSets(List<Set<T>> list) {
        return list.stream().reduce(HashSet.empty(), Set::union);
    }

    static <T> T combineAll(List<T> list, Monoid<T> monoid) {
        return list.stream().reduce(monoid.empty(), monoid::combine);
    }

    static <T extends Monoid<T>> Optional<T> combineAll(List<T> list) {
        return list.stream().reduce((a, b) -> a.combine(a, b));
    }

    static <A> Json toJson(A value, JsonWriter<A> w){
        return w.write(value);
    }

}

//class EmptyInt implements Empty<Integer>{
//
//    @Override
//    public Integer empty() {
//        return 0;
//    }
//}

class Pair<A, B> {
    private A first;
    private B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }
}

class MPair<A, B> implements Monoid<MPair<A, B>> {
    private A first;
    private B second;
    private final Monoid<A> A;
    private final Monoid<B> B;

    public MPair(A first, B second, Monoid<A> A, Monoid<B> B) {
        this.first = first;
        this.second = second;
        this.A = A;
        this.B = B;
    }

    public MPair(Monoid<A> A, Monoid<B> B) {
        this(A.empty(), B.empty(), A, B);
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public MPair<A, B> empty() {
        return new MPair<>(A, B);
    }

    @Override
    public MPair<A, B> combine(MPair<A, B> x, MPair<A, B> y) {
        return new MPair<>(A.combine(x.first, y.first), B.combine(x.second, y.second), A, B);
    }
}

interface Json{
    default <T> T as(){
        return (T) this;
    }
    default <T> T as(Class<T> clazz){
        return clazz.cast(this);
    }
}

class JsObject implements Json{
    private Map<String, Json> value;

    JsObject(Map<String, Json> map) {
        this.value = map;
    }

    public Map<String, Json> get() {
        return value;
    }
}

class JsString implements Json{
    private String value;

    JsString(String value) {
        this.value = value;
    }

    public String get() {
        return value;
    }
}

class JsNumber implements Json{
    private double value;

    JsNumber(double value) {
        this.value = value;
    }

    public double get() {
        return value;
    }
}

final class JsNull implements Json{
    private static final JsNull instance = new JsNull();
    private JsNull(){
    }

    public static JsNull get(){
        return instance;
    }
}
class Person {
    private String name;
    private String email;

    public Person(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
