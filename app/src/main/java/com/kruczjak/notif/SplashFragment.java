package com.kruczjak.notif;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

import java.util.List;

public class SplashFragment extends Fragment {
    private static final String TAG = "SplashFragment";
    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.splash,
                container, false);

        LoginButton authButton = (LoginButton) view.findViewById(R.id.login_button);
        authButton.setFragment(this);
        authButton.setReadPermissions(new String[]{"xmpp_login", "read_mailbox", "user_friends"});
        return view;
    }

    /**
     * Called after session changed
     * @param session active session
     * @param state active session state
     * @param exception error
     */
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        //((Starter)getActivity()).onSessionStateChange(state);
        ((Starter)getActivity()).enableMenu(false);
        if (state.isOpened()) {
            if (!isExtendedPermissionAdded(session)) {
                setNewView(session);
            }   else {
                startLogin();
            }
        }   else if (state.isClosed())  {
            //not logged, we are in splash, so just give a Toast
            Toast.makeText(getActivity(), "Not authenticated", Toast.LENGTH_SHORT).show();
        }
        }

    /**
     * Starts logging actions if first run
     */
    private void startLogin() {
        ((Starter)getActivity()).removeSplashFragment();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!preferences.getBoolean("log", false)) {
            preferences.edit().putBoolean("log", true).commit();
            if (preferences.getBoolean("firstRun",true))
                ((Starter) getActivity()).loggedInActions();
        }
    }

    /**
     * This method sets a view with buttons for extended permissions
     * @param session
     */
    private void setNewView(final Session session) {
        ViewGroup container = (ViewGroup) getView();
        container.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.splash_buttons, container, true);
        Button buttonGo = (Button) view.findViewById(R.id.button_go_ahead);
        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "clicked to add");
                addFacebookPermission(session);
            }
        });
    }

    /**
     * Fucking Async adding permissions -_- it's TODO, ok but add check
     * @param session
     */
    private void addFacebookPermission(Session session) {
        Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, new String[]{"manage_notifications"})
                .setDefaultAudience(SessionDefaultAudience.ONLY_ME);
        session.requestNewPublishPermissions(newPermissionsRequest);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    /**
     * Checks if extended permissions are added
     * @param session active session
     * @return true if exists
     */
    private boolean isExtendedPermissionAdded(Session session) {
        List<String> permissions = session.getPermissions();
        return permissions.contains("manage_notifications");
    }
}
