package uk.ac.ebi.interpro.common;

import uk.ac.ebi.interpro.common.collections.*;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * get/set beaninfo
 */
public class IntrospectiveBeanInfo extends SimpleBeanInfo {


    PropertyDescriptor[] propertyDescriptors;
    Map<String,PropertyDescriptor> propertyIndex=new HashMap<String, PropertyDescriptor>();
    Map<String,Exception> errors=new HashMap<String,Exception>();

    public Map<String,Exception> getErrors() {
        return errors;
    }



    public void set(Object bean,String name,Object value) throws IllegalAccessException, InvocationTargetException, IntrospectionException, NoSuchMethodException, InstantiationException {
        PropertyDescriptor pd=(PropertyDescriptor) propertyIndex.get(name);
        if (pd==null) throw new IntrospectionException("Property "+name+" not found");
        Method writeMethod = pd.getWriteMethod();
        if (writeMethod==null) throw new IntrospectionException("Property "+name+" not writeable");
        if (!pd.getPropertyType().isAssignableFrom(value.getClass())) {
            value=pd.getPropertyType().getConstructor(value.getClass()).newInstance(value);
        }
        writeMethod.invoke(bean,value);
    }

    public Object get(Object bean,String name) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        PropertyDescriptor pd=(PropertyDescriptor) propertyIndex.get(name);
        if (pd==null) throw new IntrospectionException("Property "+name+" not found");
        Method readMethod = pd.getReadMethod();
        if (readMethod==null) throw new IntrospectionException("Property "+name+" not readable");
        return readMethod.invoke(bean,new Object[0]);
    }



    public Map get(Object bean,Map<String,Exception> errors) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        Map<String,Object> map=new HashMap<String,Object>();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
            String name=propertyDescriptor.getName();
            try {
                map.put(name,get(bean,name));
            } catch (Exception e) {
                errors.put(name,e);
            }
        }
        return map;
    }

    public void set(Object bean,Map<String,? extends Object> values,Map<String,Exception> errors)  {

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            Object value = values.get(name);
            if (value == null) continue;
            try {
                set(bean, name, value);
            } catch (Exception e) {
                errors.put(name, e);
            }
        }
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }


    //getXyzA->xyzA
    public String methodNameToProperty(String method) {
        char[] c = method.toCharArray();
        c[3] = Character.toLowerCase(c[3]);
        return new String(c, 3, c.length - 3);
    }



    public IntrospectiveBeanInfo(Class parameter)  {

        Method[] m = parameter.getMethods();

        Map<String,Method[]> pd = new AutoMap<String,Method[]>(new HashMap<String,Method[]>(), new Creator.Factory<Method[]>() {
            public Method[] make() {return new Method[2];}
        });

        for (int i = 0; i < m.length; i++) {
            Method method = m[i];
            Class[] parameterTypes = method.getParameterTypes();
            if (method.getName().startsWith("set") && parameterTypes.length==1) {
                Method[] ms = (Method[]) pd.get(methodNameToProperty(method.getName()));
                ms[1] = method;
            }
            if (method.getName().startsWith("get") && parameterTypes.length==0) {
                Method[] ms = (Method[]) pd.get(methodNameToProperty(method.getName()));
                ms[0] = method;
            }
        }

        for (String name : pd.keySet()) {
            Method[] ms = (Method[]) pd.get(name);
            try {
                propertyIndex.put(name, new PropertyDescriptor(name, ms[0], ms[1]));
            } catch (IntrospectionException e) {
                errors.put(name, e);

            }
        }

        propertyDescriptors = (PropertyDescriptor[]) propertyIndex.values().toArray(new PropertyDescriptor[propertyIndex.values().size()]);
    }

    public static Object createBean(String clazz) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return IntrospectiveBeanInfo.class.getClassLoader().loadClass(clazz).newInstance();
    }

}

