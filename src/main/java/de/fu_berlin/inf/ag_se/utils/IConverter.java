package de.fu_berlin.inf.ag_se.utils;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link IConverter} can convert an object of arbitrary type to type <code>T</code>.
 *
 * @param <DEST> type give objects can be converted to
 * @author bjornson
 */
public interface IConverter<SRC, DEST> {

    /**
     * {@link IConverter} that converts objects to null of type {@link Void}.
     */
    public static final IConverter<Object, Void> CONVERTER_VOID = new IConverter<Object, Void>() {
        @Override
        public Void convert(Object returnValue) {
            return null;
        }
    };

    /**
     * {@link IConverter} that converts objects to {@link Boolean}s. Converts to <code>true</code> if the given object is of type {@link
     * Boolean} and of value <code>true</code>. Otherwise converts to <code>false</code>.
     */
    public static final IConverter<Object, Boolean> CONVERTER_BOOLEAN = new IConverter<Object, Boolean>() {
        @Override
        public Boolean convert(Object returnValue) {
            if (returnValue == null || !Boolean.class.isInstance(returnValue)) {
                return false;
            }
            return (Boolean) returnValue;
        }
    };

    public static final IConverter<Object, Boolean> ALWAYS_TRUE = new IConverter<Object, Boolean>() {
        @Override
        public Boolean convert(Object returnValue) {
            return true;
        }
    };

    /**
     * {@link IConverter} that converts objects to {@link String}s. Returns a {@link String} if the object is of type {@link String}.
     * Otherwise converts to <code>null</code>.
     */
    public static final IConverter<Object, String> CONVERTER_STRING = new IConverter<Object, String>() {
        @Override
        public String convert(Object returnValue) {
            if (returnValue == null || !String.class.isInstance(returnValue)) {
                return null;
            }
            return (String) returnValue;
        }
    };

    /**
     * {@link IConverter} that converts objects a {@link java.util.List} of {@link String}s. If a primitive type is returned, a list
     * containing this single element is returned. <code>null</code> is directly passed through.
     */
    public static final IConverter<Object, List<String>> CONVERTER_STRINGLIST = new IConverter<Object, List<String>>() {
        @Override
        public List<String> convert(Object returnValue) {
            if (returnValue == null) {
                return null;
            } else if (Object[].class.isInstance(returnValue)) {
                List<String> strings = new LinkedList<String>();
                for (int i = 0, m = ((Object[]) returnValue).length; i < m; i++) {
                    Object obj = ((Object[]) returnValue)[i];
                    strings.add(obj != null ? obj.toString() : null);
                }
                return strings;
            } else {
                return Arrays.asList(returnValue.toString());
            }
        }
    };

    /**
     * {@link IConverter} that converts objects to {@link org.eclipse.swt.graphics.Point}s. Returns a {@link
     * org.eclipse.swt.graphics.Point}
     * if the object is an array of two {@link Double}s. Otherwise converts to <code>null</code>.
     */
    public static final IConverter<Object, Point> CONVERTER_POINT = new IConverter<Object, Point>() {
        @Override
        public Point convert(Object returnValue) {
            if (returnValue == null || !Object[].class.isInstance(returnValue)
                    || ((Object[]) returnValue).length != 2
                    || !Double.class.isInstance(((Object[]) returnValue)[0])
                    || !Double.class.isInstance(((Object[]) returnValue)[1])) {
                return null;
            }
            Object[] pos = (Object[]) returnValue;
            return new Point((int) Math.round((Double) pos[0]),
                    (int) Math.round((Double) pos[1]));
        }
    };

    /**
     * {@link IConverter} that converts objects to {@link org.eclipse.swt.graphics.Rectangle}s. Returns a {@link
     * org.eclipse.swt.graphics.Point} if the object is an array of two {@link Double}s. Otherwise converts to <code>null</code>.
     */
    public static final IConverter<Object, Rectangle> CONVERTER_RECTANGLE = new IConverter<Object, Rectangle>() {
        @Override
        public Rectangle convert(Object returnValue) {
            if (returnValue == null || !Object[].class.isInstance(returnValue)
                    || ((Object[]) returnValue).length != 4
                    || !Double.class.isInstance(((Object[]) returnValue)[0])
                    || !Double.class.isInstance(((Object[]) returnValue)[1])
                    || !Double.class.isInstance(((Object[]) returnValue)[2])
                    || !Double.class.isInstance(((Object[]) returnValue)[3])) {
                return null;
            }
            Object[] pos = (Object[]) returnValue;
            return new Rectangle((int) Math.round((Double) pos[0]),
                    (int) Math.round((Double) pos[1]),
                    (int) Math.round((Double) pos[2]),
                    (int) Math.round((Double) pos[3]));
        }
    };

    /**
     * {@link IConverter} that converts objects to {@link Double}s. Returns a {@link Double} if the object is of type {@link Double}.
     * Otherwise converts to <code>null</code>.
     */
    public static final IConverter<Object, Double> CONVERTER_DOUBLE = new IConverter<Object, Double>() {
        @Override
        public Double convert(Object returnValue) {
            if (returnValue == null || !Double.class.isInstance(returnValue)) {
                return null;
            }
            return (Double) returnValue;
        }
    };

    public static final IConverter<Object, Integer> CONVERTER_INTEGER = new IConverter<Object, Integer>() {
        @Override
        public Integer convert(Object returnValue) {
            if (returnValue == null)
                return null;
            if (Double.class.isInstance(returnValue)) {
                return ((Double) returnValue).intValue();
            }
            if (!Integer.class.isInstance(returnValue)) {
                return null;
            }
            return (Integer) returnValue;
        }
    };

    public static final IConverter<Object, List<Double>> CONVERTER_DOUBLELIST = new IConverter<Object, List<Double>>() {
        @Override
        public List<Double> convert(Object returnValue) {
            if (returnValue == null) {
                return null;
            } else if (Object[].class.isInstance(returnValue)) {
                List<Double> doubles = new LinkedList<Double>();
                for (int i = 0, m = ((Object[]) returnValue).length; i < m; i++) {
                    Object obj = ((Object[]) returnValue)[i];
                    Double d = CONVERTER_DOUBLE.convert(obj);
                    if (d != null) {
                        doubles.add(d);
                    }
                }
                return doubles;
            } else {
                return Arrays.asList(CONVERTER_DOUBLE.convert(returnValue));
            }
        }
    };

    public static final IConverter<Object, List<Integer>> CONVERTER_INTEGERLIST = new IConverter<Object, List<Integer>>() {
        @Override
        public List<Integer> convert(Object returnValue) {
            if (returnValue == null) {
                return null;
            } else {
                List<Double> doubleList = CONVERTER_DOUBLELIST.convert(returnValue);
                ArrayList<Integer> res = new ArrayList<Integer>(doubleList.size());
                for (Double d : doubleList) {
                    res.add((int) Math.round(d));
                }
                return res;
            }
        }
    };

    public static final IConverter<Object, Object> CONVERTER_IDENT = new IConverter<Object, Object>() {
        @Override
        public Object convert(Object returnValue) {
            return returnValue;
        }
    };

    public DEST convert(SRC returnValue);
}