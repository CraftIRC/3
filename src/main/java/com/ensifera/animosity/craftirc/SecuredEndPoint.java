package com.ensifera.animosity.craftirc;

public interface SecuredEndPoint extends EndPoint {
    enum Security {
        /**
         * Paths can be established automatically by auto-paths (default if EndPoint or BasePoint are used)
         */
        UNSECURED,
        /**
         * Paths must be defined manually in config.yml, but automatic targeting can be used
         */
        REQUIRE_PATH,
        /**
         * Paths must be defined manually in config.yml and messages must be specifically targeted at this endpoint.
         */
        REQUIRE_TARGET
    }

    Security getSecurity();
}
