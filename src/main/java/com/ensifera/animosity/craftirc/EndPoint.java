package com.ensifera.animosity.craftirc;

import java.util.List;

public interface EndPoint {
    /**
     * The type is used to format the message and such.
     */
    enum Type {
        MINECRAFT,
        IRC,
        PLAIN
    }

    Type getType();

    /**
     * This is called when a message is sent to all users in this endpoint.
     *
     * @param msg message
     */
    void messageIn(RelayedMessage msg);

    /**
     * This is called when a message is sent to a specific user in this endpoint.
     *
     * @param username username targeted
     * @param msg message
     * @return false if the message could not be delivered.
     */
    boolean userMessageIn(String username, RelayedMessage msg);

    /**
     * This is called when a message is sent to administrators in this endpoint. The definition of administrator is up to the endpoint.
     *
     * @param msg message
     * @return false if not delivered to anyone
     */
    boolean adminMessageIn(RelayedMessage msg);

    /**
     * Return a list of online users at this endpoint, if possible, or null otherwise.
     * The list can be unsorted and all items must be valid usernames in the endpoint.
     *
     * @return online users at this endpoint
     */
    List<String> listUsers();

    /**
     * Returns a list of users for display purposes; Each entry may contain extra information that makes it unusable as a username.
     * Can be identical to listUsers().
     *
     * @return online users at this endpoint
     */
    List<String> listDisplayUsers();
}
