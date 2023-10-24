package org.openelisglobal.security.login;

import org.openelisglobal.common.log.LogEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationListener {


    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
            LogEvent.logInfo(this.getClass().getName(), "onSuccess", "Successful login attempt for " + success.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failures) {
            LogEvent.logInfo(this.getClass().getName(), "onFailure", "Unsuccessful login attempt for " + failures.getAuthentication().getName());
    }
}
