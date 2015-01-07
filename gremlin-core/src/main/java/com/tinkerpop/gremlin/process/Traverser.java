package com.tinkerpop.gremlin.process;

import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.detached.Attachable;

import java.io.Serializable;

/**
 * A {@link Traverser} represents the current state of an object flowing through a {@link Traversal}.
 * A traverser maintains a reference to the current object, a traverser-local "sack", a traversal-global sideEffect, a bulk count, and a path history.
 *
 * Different types of traverser can exist depending on the semantics of the traversal and the desire for
 * space/time optimizations of the developer.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Traverser<T> extends Serializable, Comparable<Traverser<T>> {

    /**
     * Get the object that the traverser is current at.
     *
     * @return The current object of the traverser
     */
    public T get();

    /**
     * Get the sack local sack object of this traverser.
     *
     * @param <S> the type of the sack object
     * @return the sack object
     */
    public <S> S sack();

    /**
     * Set the traversers sack object to the provided value ("sack the value").
     *
     * @param object the new value of the traverser's sack
     * @param <S>    the type of the object
     */
    public <S> void sack(final S object);

    /**
     * Get the current path of the traverser.
     *
     * @return The path of the traverser
     */
    public Path path();

    /**
     * Get the object associated with the specified step-label in the traverser's path history.
     *
     * @param stepLabel the step-label in the path to access
     * @param <A>       the type of the object
     * @return the object associated with that path label (if more than one object occurs at that step, a list is returned)
     */
    public default <A> A path(final String stepLabel) {
        return this.path().get(stepLabel);
    }

    /**
     * Return the number of times the traverser has gone through a looping section of a traversal.
     *
     * @return The number of times the traverser has gone through a loop
     */
    public short loops();

    /**
     * A traverser may represent a grouping of traversers to allow for more efficient data propagation.
     *
     * @return the number of traversers represented in this traverser.
     */
    public long bulk();

    /**
     * Get the sideEffects associated with the traversal of the traverser.
     *
     * @return the traversal sideEffects of the traverser
     */
    public Traversal.SideEffects sideEffects();

    /**
     * Get a particular value from the sideEffects of the traverser.
     *
     * @param sideEffectKey the key of the value to get from the sideEffects
     * @param <A>           the type of the returned object
     * @return the object in the sideEffects of the respective key
     */
    public default <A> A sideEffects(final String sideEffectKey) {
        return this.sideEffects().get(sideEffectKey);
    }

    /**
     * If the underlying object of the traverser is comparable, compare it with the other traverser.
     *
     * @param other the other traverser that presumably has a comparable internal object
     * @return the comparison of the two objects of the traversers
     * @throws ClassCastException if the object of the traverser is not comparable
     */
    @Override
    public default int compareTo(final Traverser<T> other) throws ClassCastException {
        final T a = this.get();
        if (a instanceof Comparable)
            return ((Comparable) a).compareTo(other.get());
        else
            throw new ClassCastException("The traverser does not contain a comparable object: " + a.getClass());
    }

    /**
     * Typecast the traverser to a "system traverser" so {@link com.tinkerpop.gremlin.process.Traverser.Admin} methods can be accessed.
     * Used as a helper method to avoid the awkwardness of <code>((Traverser.Administrative)traverser)</code>.
     *
     * @return The type-casted traverser
     */
    public default Admin<T> asAdmin() {
        return (Admin<T>) this;
    }

    /**
     * The methods in System.Traverser are useful to underlying Step and Traversal implementations.
     * They should not be accessed by the user during lambda-based manipulations.
     */
    public interface Admin<T> extends Traverser<T>, Attachable<Admin<T>> {

        public static final String HALT = Graph.Hidden.hide("halt");

        /**
         * When two traversers are {@link Traverser#equals} to each other, then they can be merged.
         * This method is used to merge the traversers into a single traverser.
         * This is used for optimization where instead of enumerating all traversers, they can be counted.
         *
         * @param other the other traverser to merge into this traverser. Once merged, the other can be garbage collected.
         */
        public void merge(final Admin<?> other);

        /**
         * Generate a child traverser of the current traverser for current as step and new object location.
         * The child has the path history, future, and loop information of the parent.
         * The child extends that path history with the current as and provided R-object.
         *
         * @param label The current label of the child
         * @param r     The current object of the child
         * @param <R>   The current object type of the child
         * @return The split traverser
         */
        public <R> Admin<R> split(final String label, final R r);

        /**
         * Generate a sibling traverser of the current traverser with a full copy of all state within the sibling.
         *
         * @return The split traverser
         */
        public Admin<T> split();


        /**
         * Set the current object location of the traverser.
         *
         * @param t The current object of the traverser
         */
        public void set(final T t);

        /**
         * Increment the number of times the traverser has gone through a looping section of traversal.
         */
        public void incrLoops();

        /**
         * Set the number of times the traverser has gone through a loop back to 0.
         * When a traverser exits a looping construct, this method should be called.
         */
        public void resetLoops();

        /**
         * Return the future step of the traverser as signified by the steps as-label.
         * This is typically used in multi-machine systems that require the movement of
         * traversers between different traversal instances.
         *
         * @return The future step for the traverser
         */
        public String getFuture();

        /**
         * Set the future of the traverser as signified by the step's label.
         * If the future is {@link Traverser.Admin#HALT}, then {@link Traverser.Admin#isHalted()} is true.
         *
         * @param label The future labeled step of the traverser
         */
        public void setFuture(final String label);

        /**
         * Set the number of traversers represented by this traverser.
         *
         * @param count the number of traversers
         */
        public void setBulk(final long count);

        /**
         * If the traverser has "no future" then it is done with its lifecycle.
         * This does not mean that the traverser is "dead," only that it has successfully passed through the {@link Traversal}.
         *
         * @return Whether the traverser is done executing or not
         */
        public default boolean isHalted() {
            return getFuture().equals(HALT);
        }

        /*
          A helper that sets the future of the traverser to {@link Traverser.Admin#HALT}.
        public default void halt() {
            this.setFuture(HALT);
        } */

        /**
         * Prepare the traverser for migration across a JVM boundary.
         *
         * @return The deflated traverser
         */
        public Admin<T> detach();

        /**
         * Regenerate the detached traverser given its location at a particular vertex.
         *
         * @param hostVertex The vertex that is hosting the traverser
         * @return The inflated traverser
         */
        @Override
        public Admin<T> attach(final Vertex hostVertex);

        /**
         * Traversers can not attach to graphs and thus, an {@link UnsupportedOperationException} is thrown.
         *
         * @param graph the graph to attach the traverser to, which it can't.
         * @return nothing as an exception is thrown
         * @throws UnsupportedOperationException is always thrown as it makes no sense to attach a traverser to a graph
         */
        @Override
        public default Admin<T> attach(final Graph graph) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("A traverser can only exist a vertices, not the graph");
        }

        /**
         * Set the sideEffects of the {@link Traversal}. Given that traversers can move between machines,
         * it may be important to re-set this when the traverser crosses machine boundaries.
         *
         * @param sideEffects the sideEffects of the traversal.
         */
        public void setSideEffects(final Traversal.SideEffects sideEffects);

    }
}
