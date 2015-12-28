package com.ensifera.animosity.craftirc;

import java.util.List;

/**
 * Basic null endpoint that can be extended by a plugin writer.
 */
public abstract class BasePoint implements SecuredEndPoint {
    @Override
    public Security getSecurity() {
        return Security.UNSECURED;
    }

    @Override
    public void messageIn(RelayedMessage msg) {
    }

    @Override
    public boolean userMessageIn(String username, RelayedMessage msg) {
        return false;
    }

    @Override
    public boolean adminMessageIn(RelayedMessage msg) {
        return false;
    }

    @Override
    public List<String> listUsers() {
        return null;
    }

    @Override
    public List<String> listDisplayUsers() {
        return null;
    }
}
