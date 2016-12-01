package com.aol.cyclops.reactor.collections.extensions.persistent;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jooq.lambda.tuple.Tuple2;
import org.junit.Test;

import com.aol.cyclops.control.LazyReact;
import com.aol.cyclops.data.collections.extensions.FluentCollectionX;
import com.aol.cyclops.data.collections.extensions.persistent.PBagX;
import com.aol.cyclops.data.collections.extensions.persistent.PStackX;
import com.aol.cyclops.reactor.collections.extensions.AbstractOrderDependentCollectionXTest;

import reactor.core.publisher.Flux;

public class LazyPStackXTest extends AbstractOrderDependentCollectionXTest  {

    @Override
    public <T> FluentCollectionX<T> of(T... values) {
        LazyPStackX<T> list = LazyPStackX.empty();
        for (T next : values) {
            list = list.plus(list.size(), next);
        }
        System.out.println("List " + list);
        return list.efficientOpsOff();

    }

    @Test
    public void onEmptySwitch() {
        assertThat(PStackX.empty()
                          .onEmptySwitch(() -> PStackX.of(1, 2, 3)),
                   equalTo(PStackX.of(1, 2, 3)));
    }

    @Test
    public void testWith(){
        LazyPStackX.of(1,2,3).with(1, 10).printOut();
    }
    AtomicInteger executing = new AtomicInteger(-1);
    @Test
    public void threadSpinLockTest(){
        LazyReact react = new LazyReact(20,20);
        for (int x = 0; x < 100; x++) {
            executing.set(-1);
            System.out.println("------------------------------");
            final int run = x;
            LazyPStackX<Integer> list = LazyPStackX.of(1, 2, 3)
                                                   .map(i -> i + 2)
                                                   .limit(1)
                                                   .peek(c->{
                                                       if(executing.get()!=-1)
                                                           fail("already set! " + executing.get() + " run is " + run);
                                                       executing.set(run);
                                                   });
            
            react.ofAsync(()->list)
                    .cycle(20)
                    .map(s -> Thread.currentThread()
                                  .getId()
                          + " " + s.size())
                  .forEach(System.out::println);
            System.out.println("------------------------------");
        }
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops.functions.collections.extensions.AbstractCollectionXTest#
     * empty()
     */
    @Override
    public <T> FluentCollectionX<T> empty() {
        return LazyPStackX.empty();
    }

    

    @Test
    public void remove() {

        LazyPStackX.of(1, 2, 3)
               .minusAll(PBagX.of(2, 3))
               .flatMapPublisher(i -> Flux.just(10 + i, 20 + i, 30 + i));

    }

    @Override
    public FluentCollectionX<Integer> range(int start, int end) {
        return LazyPStackX.range(start, end);
    }

    @Override
    public FluentCollectionX<Long> rangeLong(long start, long end) {
        return LazyPStackX.rangeLong(start, end);
    }

    @Override
    public <T> FluentCollectionX<T> iterate(int times, T seed, UnaryOperator<T> fn) {
        return LazyPStackX.iterate(times, seed, fn);
    }

    @Override
    public <T> FluentCollectionX<T> generate(int times, Supplier<T> fn) {
        return LazyPStackX.generate(times, fn);
    }

    @Override
    public <U, T> FluentCollectionX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return LazyPStackX.unfold(seed, unfolder);
    }
}
