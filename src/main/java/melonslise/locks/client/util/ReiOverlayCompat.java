package melonslise.locks.client.util;

import melonslise.locks.Locks;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReiOverlayCompat {
    private static final String REI_MOD_ID = "rei";
    private static final String REI_ALT_MOD_ID = "roughlyenoughitems";
    private static final String REI_RUNTIME = "me.shedaniel.rei.api.client.REIRuntime";
    private static boolean loggedPresence;

    private ReiOverlayCompat() {}

    @Nullable
    public static Boolean captureAndHide() {
        if (!isReiPresent())
            return null;
        Boolean visible = getOverlayVisible();
        if (visible == null)
            return null;
        if (visible) {
            toggleOverlay();
        }
        return visible;
    }

    public static void restore(@Nullable Boolean previousVisible) {
        if (previousVisible == null)
            return;
        Boolean visible = getOverlayVisible();
        if (visible == null)
            return;
        if (!previousVisible.equals(visible)) {
            toggleOverlay();
        }
    }

    @Nullable
    private static Object getRuntime() {
        try {
            Class<?> runtimeClass = Class.forName(REI_RUNTIME);
            Method getInstance = runtimeClass.getMethod("getInstance");
            return getInstance.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static boolean isReiPresent() {
        FabricLoader loader = FabricLoader.getInstance();
        boolean present = loader.isModLoaded(REI_MOD_ID) || loader.isModLoaded(REI_ALT_MOD_ID);
        if (present && !loggedPresence) {
            Locks.LOGGER.info("[Locks/REI] REI detected, disabling overlay during lockpicking.");
            loggedPresence = true;
        }
        return present;
    }

    @Nullable
    private static Boolean getOverlayVisible() {
        Object runtime = getRuntime();
        if (runtime == null)
            return null;
        Boolean visible = readBoolean(runtime, "isOverlayVisible");
        return visible;
    }

    private static void toggleOverlay() {
        Object runtime = getRuntime();
        if (runtime == null)
            return;
        invokeVoid(runtime, "toggleOverlayVisible");
    }

    @Nullable
    private static Boolean readBoolean(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object result = method.invoke(target);
                if (result instanceof Boolean value)
                    return value;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return null;
    }

    private static boolean invokeVoid(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                method.invoke(target);
                return true;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return false;
    }
}
