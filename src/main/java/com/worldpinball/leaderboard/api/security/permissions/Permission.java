package com.worldpinball.leaderboard.api.security.permissions;

public enum Permission {
    MachinesProvision("MachinesProvision"),
    MachinesReissueToken("MachinesReissueToken"),
    ShipmentsDispatch("ShipmentsDispatch"),
    ShipmentsCancel("ShipmentsCancel"),
    SessionsPlay("SessionsPlay"),
    CertificatesIssueSoftware("CertificatesIssueSoftware");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
