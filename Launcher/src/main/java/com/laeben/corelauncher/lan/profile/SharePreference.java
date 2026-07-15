package com.laeben.corelauncher.lan.profile;

import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.lan.entity.LANRecipient;

import java.util.Set;

public record SharePreference(
        Set<Profile> targetProfiles,
        boolean sendCompleteSetup,
        Set<LANRecipient> targetRecipients
) {

}
