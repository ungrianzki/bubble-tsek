package com.edison.bubbletsek;

import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.ClearTokenRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import java.util.Arrays;
import java.util.List;

/**
 * Bubble Tsek: Google Drive sign-in for the Android app.
 * Uses Google Identity Services AuthorizationClient to get a short-lived access token
 * for the drive.file scope. The web code (index.html) does the Drive calls itself.
 *
 * Needs an "Android" OAuth client in Google Cloud with
 *   package name  com.edison.bubbletsek
 *   SHA-1         20:87:10:8B:F6:49:C3:FD:B2:4C:BA:6D:F5:ED:7B:1D:EC:C0:16:70
 */
@CapacitorPlugin(name = "DriveAuth")
public class DriveAuthPlugin extends Plugin {

    private static final String DRIVE_FILE = "https://www.googleapis.com/auth/drive.file";

    private ActivityResultLauncher<IntentSenderRequest> launcher;
    private PluginCall pending;

    @Override
    public void load() {
        launcher = getActivity().registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                PluginCall call = pending;
                pending = null;
                if (call == null) return;
                if (result.getResultCode() != Activity.RESULT_OK) {
                    call.reject("Sign-in was cancelled.", "CANCELED");
                    return;
                }
                try {
                    AuthorizationResult r = Identity.getAuthorizationClient(getActivity())
                        .getAuthorizationResultFromIntent(result.getData());
                    deliver(call, r);
                } catch (ApiException e) {
                    call.reject(message(e), String.valueOf(e.getStatusCode()));
                } catch (Exception e) {
                    call.reject(message(e), "FAILED");
                }
            });
    }

    /** JS: DriveAuth.authorize({ interactive: true|false }) -> { accessToken } */
    @PluginMethod
    public void authorize(PluginCall call) {
        final boolean interactive = Boolean.TRUE.equals(call.getBoolean("interactive", false));
        List<Scope> scopes = Arrays.asList(new Scope(DRIVE_FILE), new Scope("openid"), new Scope("email"));
        AuthorizationRequest request = AuthorizationRequest.builder().setRequestedScopes(scopes).build();

        Identity.getAuthorizationClient(getActivity())
            .authorize(request)
            .addOnSuccessListener(r -> {
                if (r.hasResolution()) {
                    if (!interactive || r.getPendingIntent() == null) {
                        call.reject("Sign-in needed", "NEEDS_CONSENT");
                        return;
                    }
                    if (pending != null) pending.reject("Sign-in was cancelled.", "CANCELED");
                    pending = call;
                    try {
                        launcher.launch(new IntentSenderRequest.Builder(r.getPendingIntent().getIntentSender()).build());
                    } catch (Exception e) {
                        pending = null;
                        call.reject(message(e), "FAILED");
                    }
                } else {
                    deliver(call, r);
                }
            })
            .addOnFailureListener(e -> {
                String code = (e instanceof ApiException) ? String.valueOf(((ApiException) e).getStatusCode()) : "FAILED";
                call.reject(message(e), code);
            });
    }

    /** JS: DriveAuth.clearToken({ token }) — forget a token Google rejected, so the next authorize gets a fresh one. */
    @PluginMethod
    public void clearToken(PluginCall call) {
        String token = call.getString("token");
        if (token == null || token.isEmpty()) { call.resolve(); return; }
        try {
            Identity.getAuthorizationClient(getActivity())
                .clearToken(ClearTokenRequest.builder().setToken(token).build())
                .addOnSuccessListener(v -> call.resolve())
                .addOnFailureListener(e -> call.resolve());
        } catch (Exception e) {
            call.resolve();
        }
    }

    private void deliver(PluginCall call, AuthorizationResult r) {
        List<String> granted = r.getGrantedScopes();
        if (granted == null || !granted.contains(DRIVE_FILE)) {
            call.reject("Google Drive permission was not given.", "NO_SCOPE");
            return;
        }
        String token = r.getAccessToken();
        if (token == null || token.isEmpty()) {
            call.reject("Google did not return an access token.", "NO_TOKEN");
            return;
        }
        JSObject out = new JSObject();
        out.put("accessToken", token);
        call.resolve(out);
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return (m == null || m.isEmpty()) ? e.getClass().getSimpleName() : m;
    }
}
