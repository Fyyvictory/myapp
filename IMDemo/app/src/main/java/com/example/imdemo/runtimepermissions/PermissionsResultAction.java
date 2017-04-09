package com.example.imdemo.runtimepermissions;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by SH on 2016/12/29.
 */

public abstract class PermissionsResultAction {

    private final Set<String> mPermissions = new HashSet<String>(1);
    private Looper mLooper = Looper.getMainLooper();

    public PermissionsResultAction(){}
    @SuppressWarnings("unused")
    public PermissionsResultAction(@NonNull Looper looper) {mLooper = looper;}

    /**
     * This method is called when ALL permissions that have been
     * requested have been granted by the user. In this method
     * you should put all your permissions sensitive code that can
     * only be executed with the required permissions.
     */
    public abstract void onGranted();

    /**
     * This method is called when a permission has been denied by
     * the user. It provides you with the permission that was denied
     * and will be executed on the Looper you pass to the constructor
     * of this class, or the Looper that this object was created on.
     *
     * @param permission the permission that was denied.
     */
    public abstract void onDenied(String permission);

    /**
     * This method is used to determine if a permission not
     * being present on the current Android platform should
     * affect whether the PermissionsResultAction should continue
     * listening for events. By default, it returns true and will
     * simply ignore the permission that did not exist. Usually this will
     * work fine since most new permissions are introduced to
     * restrict what was previously allowed without permission.
     * If that is not the case for your particular permission you
     * request, override this method and return false to result in the
     * Action being denied.
     *
     * @param permission the permission that doesn't exist on this
     *                   Android version
     * @return return true if the PermissionsResultAction should
     * ignore the lack of the permission and proceed with exection
     * or false if the PermissionsResultAction should treat the
     * absence of the permission on the API level as a denial.
     */
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    public synchronized boolean shouldIgnorePermissionNotFound(String permission) {
        Log.d("notfound", "Permission not found: " + permission);
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    @CallSuper
    protected synchronized final boolean onResult(final @NonNull String permission, int result) {
        if (result == PackageManager.PERMISSION_GRANTED) {
            return onResult(permission, Permissions.GRANTED);
        } else {
            return onResult(permission, Permissions.DENIED);
        }

    }

    /**
     * This method is called when a particular permission has changed.
     * This method will be called for all permissions, so this method determines
     * if the permission affects the state or not and whether it can proceed with
     * calling onGranted or if onDenied should be called.
     *
     * @param permission the permission that changed.
     * @param result     the result for that permission.
     * @return this method returns true if its primary action has been completed
     * and it should be removed from the data structure holding a reference to it.
     */
    @SuppressWarnings("WeakerAccess")
    @CallSuper
    protected synchronized final boolean onResult(final @NonNull String permission, Permissions result) {
        mPermissions.remove(permission);
        if (result == Permissions.GRANTED) {
            if (mPermissions.isEmpty()) {
                new Handler(mLooper).post(new Runnable() {
                    @Override
                    public void run() {
                        onGranted();
                    }
                });
                return true;
            }
        } else if (result == Permissions.DENIED) {
            new Handler(mLooper).post(new Runnable() {
                @Override
                public void run() {
                    onDenied(permission);
                }
            });
            return true;
        } else if (result == Permissions.NOT_FOUND) {
            if (shouldIgnorePermissionNotFound(permission)) {
                if (mPermissions.isEmpty()) {
                    new Handler(mLooper).post(new Runnable() {
                        @Override
                        public void run() {
                            onGranted();
                        }
                    });
                    return true;
                }
            } else {
                new Handler(mLooper).post(new Runnable() {
                    @Override
                    public void run() {
                        onDenied(permission);
                    }
                });
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("WeakerAccess")
    @CallSuper
    protected synchronized final void registerPermissions(@NonNull String[] perms) {
        Collections.addAll(mPermissions, perms);
    }
}
