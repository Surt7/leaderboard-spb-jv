package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants;

import java.util.List;

public sealed interface GetStreamAccessGrantsResult {
    record Success(List<StreamAccessGrantDto> grants) implements GetStreamAccessGrantsResult {}
}
