package com.baidu.oasp;

public enum OASPStatus {
    /**************** Status determined locally ****************/
    UNSUPPORTED(-102),  // target app does not support OASP
    COMM_ERROR(-101),   // error communicating to the OASP server
    CORRUPTED(-100),    // target app seems to support OASP but not properly signed

    /**************** Status determined remotely ****************/
    INVALID(-3),        // OASP server says the query is malformed
    UNKNOWN(-2),        // OASP server says the app is unknown
    BAD(-1),            // OASP server says the app is bad
    REDIRECT(0),        // OASP server redirects to another OASP server for answer
    OK(1);              // OASP server says the app is a good one

    private int status;

    OASPStatus(int value) {
        this.status = value;
    }

    public int getValue() {
        return status;
    }

    public static OASPStatus fromInt(int value) {
        switch (value) {
            case -102:
                return UNSUPPORTED;
            case -101:
                return COMM_ERROR;
            case -100:
                return CORRUPTED;
            case -3:
                return INVALID;
            case -2:
                return UNKNOWN;
            case -1:
                return BAD;
            case 0:
                return REDIRECT;
            case 1:
                return OK;
            default:
                return null;
        }
    }

    public static boolean contains(int value) {
        return value <= OK.getValue() && value >= UNSUPPORTED.getValue();
    }
}