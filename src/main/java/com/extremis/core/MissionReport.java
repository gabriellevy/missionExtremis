package com.extremis.core;

import java.util.List;

public record MissionReport(
        String missionId,
        List<String> casualties,
        long survivors,
        boolean teamWiped,
        String log) {}
