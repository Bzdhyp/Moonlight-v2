package wtf.moonlight.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("moonlight/events");
    private static final Map<Method, MethodHandle> FACTORIES = new ConcurrentHashMap<>();

    private final Map<Class<?>, Handler[]> handlers = new ConcurrentHashMap<>();
    private final Map<Object, List<Handler>> registered = new IdentityHashMap<>();

    public void register(Object owner) {
        Objects.requireNonNull(owner, "owner");
        synchronized (registered) {
            if (registered.containsKey(owner)) return;
            List<Handler> created = new ArrayList<>();
            for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                Method[] methods = type.getDeclaredMethods();
                Arrays.sort(methods, Comparator.comparing(Method::getName).thenComparing(Method::toString));
                for (Method method : methods) {
                    Subscribe subscribe = method.getAnnotation(Subscribe.class);
                    if (subscribe == null) continue;
                    Handler handler = new Handler(validate(method), subscribe.priority(), invoker(method, owner), describe(method));
                    created.add(handler);
                    handlers.compute(handler.type, (key, list) -> with(list, handler));
                }
            }
            registered.put(owner, created);
        }
    }

    public void unregister(Object owner) {
        List<Handler> removed;
        synchronized (registered) {
            removed = registered.remove(owner);
        }
        if (removed == null) return;
        for (Handler handler : removed) {
            handler.active = false;
            handlers.computeIfPresent(handler.type, (key, list) -> without(list, handler));
        }
    }

    public boolean hasListeners(Class<? extends Event> type) {
        return handlers.containsKey(type);
    }

    public <E extends Event> E post(E event) {
        Handler[] list = handlers.get(event.getClass());
        if (list == null) return event;
        for (Handler handler : list) {
            if (!handler.active) continue;
            try {
                handler.invoker.accept(event);
            } catch (VirtualMachineError error) {
                throw error;
            } catch (Throwable throwable) {
                LOGGER.error("Listener {} failed while handling {}", handler.name, event.getClass().getSimpleName(), throwable);
            }
        }
        return event;
    }

    private static Handler[] with(Handler[] list, Handler handler) {
        if (list == null) return new Handler[]{handler};
        int index = 0;
        while (index < list.length && list[index].priority <= handler.priority) index++;
        Handler[] updated = new Handler[list.length + 1];
        System.arraycopy(list, 0, updated, 0, index);
        updated[index] = handler;
        System.arraycopy(list, index, updated, index + 1, list.length - index);
        return updated;
    }

    private static Handler[] without(Handler[] list, Handler handler) {
        int index = Arrays.asList(list).indexOf(handler);
        if (index < 0) return list;
        if (list.length == 1) return null;
        Handler[] updated = new Handler[list.length - 1];
        System.arraycopy(list, 0, updated, 0, index);
        System.arraycopy(list, index + 1, updated, index, list.length - index - 1);
        return updated;
    }

    private static Class<?> validate(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        if (Modifier.isStatic(method.getModifiers())
                || method.getReturnType() != void.class
                || parameters.length != 1
                || !Event.class.isAssignableFrom(parameters[0])
                || Modifier.isAbstract(parameters[0].getModifiers())) {
            throw new IllegalArgumentException("@Subscribe method must be a non-static void method taking one concrete event: " + describe(method));
        }
        return parameters[0];
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Object> invoker(Method method, Object owner) {
        try {
            return (Consumer<Object>) FACTORIES.computeIfAbsent(method, EventBus::lambdaFactory).invoke(owner);
        } catch (Throwable lambdaFailure) {
            try {
                method.setAccessible(true);
                MethodHandle handle = MethodHandles.lookup().unreflect(method).bindTo(owner)
                        .asType(MethodType.methodType(void.class, Object.class));
                return event -> {
                    try {
                        handle.invokeExact(event);
                    } catch (RuntimeException | Error rethrown) {
                        throw rethrown;
                    } catch (Throwable checked) {
                        throw new IllegalStateException(checked);
                    }
                };
            } catch (ReflectiveOperationException | RuntimeException failure) {
                failure.addSuppressed(lambdaFailure);
                throw new IllegalStateException("Cannot create listener for " + describe(method), failure);
            }
        }
    }

    private static MethodHandle lambdaFactory(Method method) {
        try {
            Class<?> owner = method.getDeclaringClass();
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
            return LambdaMetafactory.metafactory(
                    lookup,
                    "accept",
                    MethodType.methodType(Consumer.class, owner),
                    MethodType.methodType(void.class, Object.class),
                    lookup.unreflect(method),
                    MethodType.methodType(void.class, method.getParameterTypes()[0])
            ).getTarget().asType(MethodType.methodType(Consumer.class, Object.class));
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private static final class Handler {
        final Class<?> type;
        final int priority;
        final Consumer<Object> invoker;
        final String name;
        volatile boolean active = true;

        Handler(Class<?> type, int priority, Consumer<Object> invoker, String name) {
            this.type = type;
            this.priority = priority;
            this.invoker = invoker;
            this.name = name;
        }
    }
}
