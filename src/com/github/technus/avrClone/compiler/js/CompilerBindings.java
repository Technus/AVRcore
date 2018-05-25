package com.github.technus.avrClone.compiler.js;

import com.github.technus.avrClone.compiler.Binding;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CompilerBindings extends HashMap<String,Object> implements Bindings {
    @Override
    public Binding get(Object key) {
        return (Binding) super.get(key);
    }

    @Override
    public Binding getOrDefault(Object key, Object defaultValue) {
        if (defaultValue instanceof Number) {
            defaultValue = new Binding(Binding.NameType.SET, ((Number) defaultValue).doubleValue());
        }
        if (defaultValue instanceof Binding) {
            return (Binding) super.getOrDefault(key, defaultValue);
        }
        return null;
    }

    public boolean putCheck(String name,Object value){
        //if (value instanceof Number) {
        //    value = new Binding(Binding.NameType.SET, ((Number) value).doubleValue());
        //}
        if (value instanceof Binding) {
            Binding old = get(name);
            if (old == null) {
                super.put(name, value);
                return true;
            } else {
                switch (old.type) {
                    case SET:
                    case DEF:{
                        super.put(name, value);
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public Binding put(String name, Object value) {
        //if (value instanceof Number) {
        //    value = new Binding(Binding.NameType.SET, ((Number) value).doubleValue());
        //}
        if (value instanceof Binding) {
            Binding old = get(name);
            if (old == null) {
                return (Binding) super.put(name, value);
            } else {
                switch (old.type) {
                    case SET:
                    case DEF:
                        return (Binding) super.put(name, value);
                }
            }
            return old;
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ?> toMerge) {
        toMerge.forEach((BiConsumer<String, Object>) this::put);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        throw new NoSuchMethodError();
    }

    @Override
    public Binding merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new NoSuchMethodError();
    }

    @Override
    public Binding putIfAbsent(String key, Object value) {
        //if (value instanceof Number) {
        //    value = new Binding(Binding.NameType.SET, ((Number) value).doubleValue());
        //}
        if (value instanceof Binding) {
            return (Binding) super.putIfAbsent(key, value);
        }
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (value instanceof Number) {
            value = new Binding(Binding.NameType.SET, ((Number) value).doubleValue());
        }
        if (value instanceof Binding) {
            switch (((Binding) value).type) {
                case DEF:
                case SET:
                    return super.remove(key, value);
            }
        }
        return false;
    }

    @Override
    public Binding remove(Object key) {
        Binding binding = get(key);
        if (binding == null) {
            return null;
        }
        switch (binding.type) {
            case SET:
            case DEF:
                return (Binding) super.remove(key);
        }
        return null;
    }

    public Binding removeDef(Object key) {
        Binding binding = get(key);
        if (binding == null) {
            return null;
        }
        switch (binding.type) {
            case DEF:
                return (Binding) super.remove(key);
        }
        return null;
    }

    public Binding removeAllUnsafely(Binding.NameType... type) {
        nextKey:
        for(String key:keySet().toArray(new String[0])){
            Binding v=get(key);
            for (int i = 0; i < type.length; i++) {
                if(v.type==type[i]){
                    super.remove(key);
                    continue nextKey;
                }
            }
        }
        return null;
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        if (oldValue instanceof Number) {
            oldValue = new Binding(Binding.NameType.SET, ((Number) oldValue).doubleValue());
        }
        if (newValue instanceof Number) {
            newValue = new Binding(Binding.NameType.SET, ((Number) newValue).doubleValue());
        }
        if (oldValue instanceof Binding && newValue instanceof Binding) {
            switch (((Binding) oldValue).type) {
                case DEF:
                case SET:
                    return super.replace(key, oldValue, newValue);
            }
        }
        return false;
    }

    @Override
    public Binding replace(String key, Object value) {
        Binding binding = get(key);
        if (value instanceof Number) {
            value = new Binding(Binding.NameType.SET, ((Number) value).doubleValue());
        }
        if (value instanceof Binding) {
            if(binding==null){
                return null;
            }else {
                switch (binding.type) {
                    case SET:
                    case DEF:
                        return (Binding) super.replace(key, value);
                }
            }
        }
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof Number) {
            value = new Binding(Binding.NameType.SET, ((Number) value).doubleValue());
        }
        return super.containsValue(value);
    }

    public boolean containsNotDefs(String... keys) {
        for(Object key:keys){
            if(!containsKey(key) || get(key).type==Binding.NameType.DEF){
                return false;
            }
        }
        return true;
    }

    public boolean lacksNotDefs(String... keys) {
        for(Object key:keys){
            if(containsKey(key) && get(key).type!=Binding.NameType.DEF){
                return false;
            }
        }
        return true;
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        throw new NoSuchMethodError();
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        throw new NoSuchMethodError();
    }
}
