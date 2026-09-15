package com.example.jejugilmoa.domain.user.converter;

import com.example.jejugilmoa.domain.user.dto.BlockedUserResponse;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserBlock;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserBlockConverter {

    public BlockedUserResponse toBlockedUserResponse(UserBlock block) {
        User blocked = block.getBlocked();
        return new BlockedUserResponse(
                blocked.getId(),
                blocked.getNickname(),
                blocked.getProfileImageUrl()
        );
    }

    public List<BlockedUserResponse> toBlockedUserResponseList(List<UserBlock> blocks) {
        return blocks.stream()
                .map(this::toBlockedUserResponse)
                .toList();
    }
}
