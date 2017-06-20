package com.yulong.oaspclient;

public enum OASPStatus {
    // Status determined locally
    UNSUPPORTED(-102),
    COMM_ERROR(-101),
    CORRUPTED(-100),
    // Status determined remotely
    INVALID(-3),
    UNKNOWN(-2),
    BAD(-1),
    REDIRECT(0),
    OK(1);

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
        if (value <= OK.getValue() && value >= UNSUPPORTED.getValue()) {
            return true;
        } else {
            return false;
        }
    }
}