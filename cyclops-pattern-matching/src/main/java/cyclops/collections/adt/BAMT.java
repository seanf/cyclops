package cyclops.collections.adt;

import cyclops.patterns.CaseClass1;
import cyclops.patterns.CaseClass2;
import cyclops.patterns.Sealed3;
import cyclops.patterns.Sealed4;
import cyclops.stream.ReactiveSeq;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple1;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;


public interface BAMT<V>  {

    static final int BITS = 5;
    static final int BUCKET_SIZE = 1 << BITS;
    static final int MASK = (1 << BITS) - 1;
    static final Node[] EMPTY_ARRAY = createBaseEmptyArray();

    static <V> Node<V> empty(){
        return EmptyNode.Instance;
    }
    static Node[] createBaseEmptyArray() {
        Node[] emptyArray = new Node[BUCKET_SIZE];
        Arrays.fill(emptyArray, EmptyNode.Instance);
        return emptyArray;
    }

    static <V> Node<V>[] emptyArray() {
        return Arrays.copyOf((Node<V>[]) EMPTY_ARRAY, BUCKET_SIZE);
    }




    interface Node<V> extends Sealed3<EmptyNode<V>,SingleNode<V>,ArrayNode<V>> {

        boolean isEmpty();

        int size();

        Node<V> put(int hash,int pos, V value);

        Optional<V> get(int hash, int pos);

        Node<V> minus(int hash, int pos);


    }

    static class EmptyNode<V> implements Node<V>{

        static EmptyNode Instance = new EmptyNode();
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Node<V> put(int hash, int key, V value) {
            if (hash == 0) {
                return new SingleNode<>(value);
            } else {
                int newHash = hash >>> BITS;
                int index = hash & MASK;
                Node<V>[] nodes = emptyArray();
                if (newHash == 0) {
                    nodes[0] = this;
                    nodes[index] = new SingleNode<>(value);
                } else {
                    nodes[index] = Instance.put(newHash, key, value);
                    if (index != 0) {
                        nodes[0] = this;
                    }
                }
                return new ArrayNode<>(nodes);
            }
        }

        @Override
        public Optional<V> get(int hash, int pos) {
            return Optional.empty();
        }

        @Override
        public Node<V> minus(int hash, int pos) {
            return this;
        }

        @Override
        public <R> R match(Function<? super EmptyNode<V>, ? extends R> fn1, Function<? super SingleNode<V>, ? extends R> fn2, Function<? super ArrayNode<V>, ? extends R> fn4) {
            return fn1.apply(this);
        }
    }
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class SingleNode<V> implements Node< V> {


        private final V value;



        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return 1;
        }



        @Override
        public Node<V> put(int hash, int key, V value) {
            if (hash == 0) {
                return new SingleNode<>(value);
            } else {
                int newHash = hash >>> BITS;
                int index = hash & MASK;
                Node<V>[] nodes = emptyArray();
                if (newHash == 0) {
                    nodes[0] = this;
                    nodes[index] = new SingleNode<>(value);
                } else {
                    nodes[index] = EmptyNode.Instance.put(newHash, key, value);
                    if (index != 0) {
                        nodes[0] = this;
                    } else {
                         nodes[0] = nodes[0].put(0, key, this.value);
                    }
                }
                return new ArrayNode<>(nodes);
            }
        }

        @Override
        public Optional<V> get(int hash, int key) {
            if(hash==0)
               return Optional.of(value);
            return Optional.empty();

        }



        @Override
        public Node<V> minus(int hash, int key) {
            if(hash==0)
                return EmptyNode.Instance;
            return this;
        }


        @Override
        public <R> R match(Function<? super EmptyNode<V>, ? extends R> fn1, Function<? super SingleNode<V>, ? extends R> fn2, Function<? super ArrayNode<V>, ? extends R> fn4) {
            return fn2.apply(this);
        }


    }



    static class ArrayNode<V> implements Node<V>, CaseClass1<Node<V>[]> {
        private final Node<V>[] nodes;

        private ArrayNode(Node<V>[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public Node<V> put(int hash, int key, V value) {
            int newHash = hash >>> BITS;
            int index = hash & MASK;
            Node<V>[] newNodes = Arrays.copyOf(nodes, nodes.length);
            newNodes[index] = nodes[index].put(newHash, key, value);
            return new ArrayNode<>(newNodes);
        }

        @Override
        public Optional<V> get(int hash, int key) {
            int newHash = hash >>> BITS;
            int index = hash & MASK;
            return nodes[index].get(newHash, key);
        }

        @Override
        public Node<V> minus(int hash, int key) {
            int newHash = hash >>> BITS;
            int index = hash & MASK;
            Node<V> node = nodes[index];
            if (node.isEmpty()) {
                return this;
            } else {
                Node<V> newNode = node.minus(newHash, key);
                if (newNode == node) {
                    return this;
                } else {
                    Node<V>[] newNodes = Arrays.copyOf(nodes, nodes.length);
                    newNodes[index] = newNode;
                    Node<V> branch = new ArrayNode<>(newNodes);
                    return branch.isEmpty() ? EmptyNode.Instance : branch;
                }
            }

        }
        @Override
        public boolean isEmpty() {
            return ReactiveSeq.of(nodes).anyMatch(Node::isEmpty);
        }

        @Override
        public int size() {
            return ReactiveSeq.of(nodes).sumInt(Node::size);
        }



        @Override
        public <R> R match(Function<? super EmptyNode<V>, ? extends R> fn1, Function<? super SingleNode<V>, ? extends R> fn2,  Function<? super ArrayNode<V>, ? extends R> fn4) {
            return fn4.apply(this);
        }

        @Override
        public Tuple1<Node<V>[]> unapply() {
            return Tuple.tuple(nodes);
        }
    }


}