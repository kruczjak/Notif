package com.kruczjak.notif;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.SessionLoginBehavior;
import com.facebook.widget.LoginButton;

public class SplashFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.splash,
                container, false);

        LoginButton authButton = (LoginButton) view.findViewById(R.id.login_button);
        authButton.setReadPermissions(new String[]{"xmpp_login", "read_mailbox", "user_friends"});
        authButton.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO);
        return view;
    }

}
