package de.fu_berlin.inf.ag_se.utils;

public interface ParametrizedCallable<T, V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @param object value
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    public V call(T object) throws Exception;
}
