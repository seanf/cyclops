package cyclops.collections.scala;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.aol.cyclops.scala.collections.HasScalaCollection;
import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.data.collections.extensions.lazy.immutable.LazyLinkedListX;
import com.aol.cyclops2.types.Unwrapable;
import com.aol.cyclops2.types.foldable.Evaluation;
import cyclops.collections.immutable.LinkedListX;
import cyclops.collections.immutable.PersistentQueueX;
import cyclops.function.Reducer;
import cyclops.stream.ReactiveSeq;
import org.jooq.lambda.tuple.Tuple2;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;



import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.Wither;
import scala.collection.GenTraversableOnce;
import scala.collection.generic.CanBuildFrom;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;
import scala.collection.Seq;
import scala.collection.mutable.Builder;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScalaListX<T> extends AbstractList<T>implements PStack<T>, HasScalaCollection<T>, Unwrapable {

    public static <T> LinkedListX<T> listX(ReactiveSeq<T> stream){
        return fromStream(stream);
    }
    @Override
    public <R> R unwrap() {
        return (R)list;
    }

    public LazyLinkedListX<T> plusLoop(int max, IntFunction<T> value) {

        List<T> toUse = list;
        for (int i = 0; i < max; i++) {
            toUse = toUse.$colon$colon(value.apply(i));
        }
        return lazyList(toUse);

    }

    public LazyLinkedListX<T> plusLoop(Supplier<Optional<T>> supplier) {
        List<T> toUse = list;
        Optional<T> next = supplier.get();
        while (next.isPresent()) {
            toUse = toUse.$colon$colon(next.get());
            next = supplier.get();
        }
        return lazyList(toUse);
    }
    /**
     * Create a LazyLinkedListX from a Stream
     * 
     * @param stream to construct a LazyQueueX from
     * @return LazyLinkedListX
     */
    public static <T> LazyLinkedListX<T> fromStream(Stream<T> stream) {
        return new LazyLinkedListX<T>(null,ReactiveSeq.fromStream(stream), toPStack(), Evaluation.LAZY);
    }

    /**
     * Create a LazyLinkedListX that contains the Integers between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyLinkedListX<Integer> range(int start, int end) {
        return fromStream(ReactiveSeq.range(start, end));
    }

    /**
     * Create a LazyLinkedListX that contains the Longs between start and end
     * 
     * @param start
     *            Number of range to start from
     * @param end
     *            Number for range to end at
     * @return Range ListX
     */
    public static LazyLinkedListX<Long> rangeLong(long start, long end) {
        return fromStream(ReactiveSeq.rangeLong(start, end));
    }

    /**
     * Unfold a function into a ListX
     * 
     * <pre>
     * {@code 
     *  LazyLinkedListX.unfold(1,i->i<=6 ? Optional.of(Tuple.tuple(i,i+1)) : Optional.empty());
     * 
     * //(1,2,3,4,5)
     * 
     * }</pre>
     * 
     * @param seed Initial value 
     * @param unfolder Iteratively applied function, terminated by an empty Optional
     * @return ListX generated by unfolder function
     */
    public static <U, T> LazyLinkedListX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return fromStream(ReactiveSeq.unfold(seed, unfolder));
    }

    /**
     * Generate a LazyLinkedListX from the provided Supplier up to the provided limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param s Supplier to generate ListX elements
     * @return ListX generated from the provided Supplier
     */
    public static <T> LazyLinkedListX<T> generate(long limit, Supplier<T> s) {

        return fromStream(ReactiveSeq.generate(s)
                                     .limit(limit));
    }

    /**
     * Create a LazyLinkedListX by iterative application of a function to an initial element up to the supplied limit number of times
     * 
     * @param limit Max number of elements to generate
     * @param seed Initial element
     * @param f Iteratively applied to each element to generate the next element
     * @return ListX generated by iterative application
     */
    public static <T> LazyLinkedListX<T> iterate(long limit, final T seed, final UnaryOperator<T> f) {
        return fromStream(ReactiveSeq.iterate(seed, f)
                                     .limit(limit));
    }

    /**
     * <pre>
     * {@code 
     * PStack<Integer> q = PStack.<Integer>toPStack()
                                     .mapReduce(Stream.of(1,2,3,4));
     * 
     * }
     * </pre>
     * @return Reducer for PStack
     */
    public static <T> Reducer<PStack<T>> toPStack() {
        return Reducer.<PStack<T>> of(ScalaListX.emptyPStack(), (final PStack<T> a) -> b -> a.plusAll(b),
                                      (final T x) -> ScalaListX.singleton(x));
    }

    public static <T> ScalaListX<T> fromList(List<T> list) {
        return new ScalaListX<>(
                                 list);
    }
    public static <T> LazyLinkedListX<T> lazyList(List<T> vector){
        return fromPStack(fromList(vector), toPStack());
    }

    private static <T> LazyLinkedListX<T> fromPStack(PStack<T> s, Reducer<PStack<T>> pStackReducer) {
        return new LazyLinkedListX<T>(s,null, pStackReducer,Evaluation.LAZY);
    }


    public static <T> ScalaListX<T> emptyPStack() {

        return new ScalaListX<>(
                                 List$.MODULE$.empty());
    }

    public static <T> LazyLinkedListX<T> empty() {
        return fromPStack(new ScalaListX<>(
                                                        List$.MODULE$.empty()),
                                      toPStack());
    }

    public static <T> LazyLinkedListX<T> singleton(T t) {
        List<T> result = List$.MODULE$.empty();
        return fromPStack(new ScalaListX<>(
                                                        result.$colon$colon(t)),
                                      toPStack());
    }

    public static <T> LazyLinkedListX<T> of(T... t) {

        Builder<T, List<T>> lb = List$.MODULE$.newBuilder();
        for (T next : t)
            lb.$plus$eq(next);
        List<T> vec = lb.result();
        return fromPStack(new ScalaListX<>(
                                                        vec),
                                      toPStack());
    }

    public static <T> LazyLinkedListX<T> PStack(List<T> q) {
        return fromPStack(new ScalaListX<T>(
                                                         q),
                                      toPStack());
    }

    @SafeVarargs
    public static <T> LazyLinkedListX<T> PStack(T... elements) {
        return fromPStack(of(elements),  toPStack());
    }

    @Wither
    final List<T> list;

    @Override
    public ScalaListX<T> plus(T e) {
        return withList(list.$colon$colon(e));
    }

    @Override
    public ScalaListX<T> plusAll(Collection<? extends T> l) {
        if(l instanceof ScalaListX){ //if a ScalaList is passed in use ScalaTypes diretly
            final CanBuildFrom<List<?>, T, List<T>> builder = List.<T> canBuildFrom();
            final CanBuildFrom<List<T>, T, List<T>> builder2 = (CanBuildFrom) builder;
            List<T> toAdd = ((ScalaListX)l).list;
            
            return withList(list.$plus$plus(toAdd, builder2));
        }
        List<T> vec = list;
        for (T next : l) {
            vec = vec.$colon$colon(next);
        }

        return withList(vec);
    }

    @Override
    public ScalaListX<T> with(int i, T e) {
        if (i < 0 || i > size())
            throw new IndexOutOfBoundsException(
                                                "Index " + i + " is out of bounds - size : " + size());

        return withList(list.drop(i + 1)
                            .$colon$colon(e)
                            .$colon$colon$colon((List<T>)list.dropRight(size()-i)));
    }

    @Override
    public ScalaListX<T> plus(int i, T e) {
        if (i < 0 || i > size()+1)
            throw new IndexOutOfBoundsException(
                                                "Index " + i + " is out of bounds - size : " + size());
        if (i == 0)
            return withList(list.$colon$colon(e));
        final CanBuildFrom<List<?>, T, List<T>> builder = List.<T> canBuildFrom();
        final CanBuildFrom<Seq<T>, T, List<T>> builder2 = (CanBuildFrom) builder;
        if (i == size()) {

            return withList(list.<T,List<T>>$colon$plus(e, builder2));
        }

        val frontBack = list.splitAt(i);

        val back = frontBack._2.$colon$colon(e);
        return withList(back.$colon$colon$colon(frontBack._1));

    }

    @Override
    public ScalaListX<T> plusAll(int i, Collection<? extends T> l) {

        if (i < 0 || i > size()+1)
            throw new IndexOutOfBoundsException(
                                                "Index " + i + " is out of bounds - size : " + size());
        
      
        if(l instanceof ScalaListX){ //if a ScalaList is passed in use ScalaTypes diretly
            final CanBuildFrom<List<?>, T, List<T>> builder = List.<T> canBuildFrom();
            final CanBuildFrom<List<T>, T, List<T>> builder2 = (CanBuildFrom) builder;
            List<T> toAdd = ((ScalaListX)l).list;
            
            return withList(list.$plus$plus(toAdd, builder2));
        }
        List<T> l2 = l instanceof ScalaListX ? ((ScalaListX)l).list : List$.MODULE$.empty();
        if(!(l instanceof ScalaListX)){
            for (T next : l) {
                l2 = l2.$colon$colon(next);
            }
        }
        if (i == 0)
            return withList(list.$colon$colon$colon(l2));
        final CanBuildFrom<List<?>, T, List<T>> builder = List.<T> canBuildFrom();
        final CanBuildFrom<List<T>, T, List<T>> builder2 = (CanBuildFrom) builder;
        if (i == size()) {

            return withList(l2.$colon$colon$colon(list));
        }

        val frontBack = list.splitAt(i);

        val back = frontBack._2.$colon$colon$colon(l2);
        return withList(back.$colon$colon$colon(frontBack._1));
    }

    @Override
    public ScalaListX<T> minus(Object e) {
        if (size() == 0)
            return this;
        if (head().equals(e)){
            List<T> tail = (List<T>)list.tail();
            if(tail==null)
                return emptyPStack();
            return withList(tail);
        }
        List<T> newRest = tail().minus(e).list;

        if (newRest == (List<T>)list.tail())
            return this;
       
        return withList(newRest.$colon$colon(head()));
    }

    @Override
    public ScalaListX<T> minusAll(Collection<?> l) {
        if (size() == 0)
            return this;
       
        if (l.contains(head())){
            List<T> res1 = (List<T>)list.tail();
            if(res1==null)
                return withList(List$.MODULE$.empty());
            return tail().minusAll(l);
        }
       
        List<T> res2 = (List<T>)list.tail();
        if(res2==null)
            return withList(List$.MODULE$.empty());
        List<T> newRest = tail().minusAll(l).list;
        if (newRest == list.tail())
            return this;
        return withList(newRest.$colon$colon(head()));
        
    }

    public ScalaListX<T> tail() {
        return withList((List<T>) list.tail());
    }

    public T head() {
        return list.head();
    }

    @Override
    public ScalaListX<T> minus(int i) {

        if (i < 0 || i > size())
            throw new IndexOutOfBoundsException(
                                                "Index " + i + " is out of bounds - size : " + size());
        if (i == 0)
            return withList(list.drop(1));
        if (i == size() - 1)
            return withList(list.dropRight(1)
                                .toList());
        val frontBack = list.splitAt(i);
        final CanBuildFrom<List<?>, T, List<T>> builder = List.<T> canBuildFrom();
        final CanBuildFrom<List<T>, T, List<T>> builder2 = (CanBuildFrom) builder;
        val front = frontBack._1;
        
        List<T> result = (List<T>) front.$plus$plus(frontBack._2.drop(1), builder2);

        return withList(result);
    }

    @Override
    public ScalaListX<T> subList(int start, int end) {

        return withList(list.drop(start)
                            .take(end - start));
    }

    @Override
    public T get(int index) {
        return list.apply(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public ScalaListX<T> subList(int start) {
        return withList(list.drop(start));
    }

    @Override
    public GenTraversableOnce<T> traversable() {
       return list;
    }

    @Override
    public CanBuildFrom canBuildFrom() {
       return List.canBuildFrom();
    }
    public static <T> LinkedListX<T> copyFromCollection(CollectionX<T> vec) {
        List<T> list = from(vec.iterator(),0);
        return fromPStack(fromList(list),toPStack());

    }
    private static <E> List<E> from(final Iterator<E> i, int depth) {

        if(!i.hasNext())
            return List$.MODULE$.empty();
        E e = i.next();
        return  from(i,depth++).$colon$colon(e);
    }
}