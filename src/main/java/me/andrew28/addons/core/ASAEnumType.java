package me.andrew28.addons.core;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.localization.Language;
import ch.njol.yggdrasil.Fields;
import me.andrew28.addons.core.annotations.DoNotRegister;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Tran
 */
@DoNotRegister
public abstract class ASAEnumType<T extends Enum<T>> extends ASAType<T> {
    private Class<T> enumClass;
    private Map<T, String> mappings = new HashMap<>();
    private Map<String, T> reverseMappings = new HashMap<>();

    public Map<T, String> getMappings() {
        return mappings;
    }

    public Map<String, T> getReverseMappings() {
        return reverseMappings;
    }

    public Class<T> getEnumClass() {
        if (enumClass == null) {
            enumClass = (Class<T>) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0];
        }
        return enumClass;
    }

    public abstract String getCodeName();

    public String getFriendlyName() {
        return getCodeName();
    }

    public String toString(T t) {
        return toString(t, 0);
    }

    public String toString(T t, int flags) {
        if (mappings.containsKey(t)) {
            String name = mappings.get(t);
            if ((flags & Language.F_PLURAL) != 0) {
                name += "s";
            }
            return name;
        }
        return null;
    }

    public void setupMappings() {
        for (T t : getEnumClass().getEnumConstants()) {
            String text = t.name().replace('_', ' ').toLowerCase();
            addMapping(text, t);
        }
    }

    public void addMapping(String from, T to) {
        mappings.put(to, from);
        reverseMappings.put(from, to);
    }

    public T fromString(String s) {
        //Remove plural and check if it is in the map
        if (s.endsWith("s")) {
            if (reverseMappings.containsKey(s.substring(0, s.length() - 1).toLowerCase())) {
                return reverseMappings.get(s.substring(0, s.length() - 1).toLowerCase());
            }
        }
        return reverseMappings.get(s.toLowerCase());
    }

    public String getUserPattern() {
        return null;
    }

    public T getDefaultValue() {
        return null;
    }

    public EventValueExpression<T> getDefaultEventValueExpression() {
        return null;
    }

    public String toVariableNameString(T t) {
        return getCodeName() + ":" + toString(t);
    }

    public String getVariableNamePattern() {
        return getCodeName() + ":.+";
    }

    @Override
    public ClassInfo<T> getClassInfo() {
        ASAEnumType<T> asaEnumType = this;
        setupMappings();
        ClassInfo<T> classInfo = new ClassInfo<>(getEnumClass(), getCodeName())
                .name(getFriendlyName())
                .user(getUserPattern() != null ? getUserPattern() : "(asa)?(-)?(enum)?" + getCodeName())
                .parser(new Parser<T>() {
                    @Override
                    public String toString(T t, int flags) {
                        return asaEnumType.toString(t, flags);
                    }

                    @Override
                    public T parse(String s, ParseContext context) {
                        return asaEnumType.fromString(s);
                    }

                    @Override
                    public String toVariableNameString(T t) {
                        return asaEnumType.toVariableNameString(t);
                    }

                    @Override
                    public String getVariableNamePattern() {
                        return asaEnumType.getVariableNamePattern();
                    }
                })
                .serializer(new Serializer<T>() {
                    @Override
                    public Fields serialize(T t) throws NotSerializableException {
                        Fields fields = new Fields();
                        fields.putObject("enum", t.name());
                        return fields;
                    }

                    @Override
                    public void deserialize(T t, Fields fields) throws StreamCorruptedException, NotSerializableException {
                        assert false;
                    }

                    @Override
                    protected T deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        String name = (String) fields.getObject("enum");
                        for (T t : asaEnumType.getEnumClass().getEnumConstants()) {
                            if (t.name().equals(name)) {
                                return t;
                            }
                        }
                        return null;
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return false;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return true;
                    }
                });
        if (getDefaultValue() != null) {
            classInfo.defaultExpression(new SimpleLiteral<T>(getDefaultValue(), true));
        } else if (getDefaultEventValueExpression() != null) {
            classInfo.defaultExpression(getDefaultEventValueExpression());
        }
        return classInfo;
    }
}
