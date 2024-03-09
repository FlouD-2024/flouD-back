package floud.demo.service;

import floud.demo.common.exception.NotFoundException;
import floud.demo.common.response.ApiResponse;
import floud.demo.common.response.Error;
import floud.demo.common.response.Success;
import floud.demo.domain.Community;
import floud.demo.domain.Users;
import floud.demo.domain.enums.PostType;
import floud.demo.dto.community.CommunityDetailResponseDto;
import floud.demo.dto.community.CommunityResponseDto;
import floud.demo.dto.community.Post;
import floud.demo.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommunityService {
    private final AuthService authService;
    private final CommunityRepository communityRepository;
    public ApiResponse<?> getCommunity(String authorizationHeader, PostType postType){
        //Get user
        Users users = authService.findUserByToken(authorizationHeader);

        List<Community> communityList =  communityRepository.findTop30ByPostType(postType.toString());
        List<Post> postList = communityList.stream().
                map(community -> Post.builder()
                        .community_id(community.getId())
                        .title(community.getTitle())
                        .content(community.getContent())
                        .postType(community.getPostType())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(Success.GET_COMMUNITY_SUCCESS, CommunityResponseDto.builder()
                .nickname(users.getNickname())
                .postType(postType)
                .postList(postList)
                .build());
    }

    public ApiResponse<?> getCommunityDetail(String authorizationHeader, Long community_id){
        //Get user
        Users users = authService.findUserByToken(authorizationHeader);

        Community community = communityRepository.findById(community_id)
                .orElseThrow(() -> new NotFoundException(Error.COMMUNITY_POST_NOT_FOUND));

        return ApiResponse.success(Success.GET_COMMUNITY_DETAIL_SUCCESS, CommunityDetailResponseDto.builder()
                        .my_nickname(users.getNickname())
                        .community_id(community.getId())
                        .writer_nickname(community.getUsers().getNickname())
                        .isMine(checkMyPost(users, community))
                        .title(community.getTitle())
                        .content(community.getContent())
                        .created_at(community.getCreated_at())
                        .updated_at(community.getUpdated_at())
                .build());
    }

    private boolean checkMyPost(Users users, Community community){
        return users.equals(community.getUsers());
    }

}
