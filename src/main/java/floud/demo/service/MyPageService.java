package floud.demo.service;

import floud.demo.common.response.ApiResponse;
import floud.demo.common.response.Error;
import floud.demo.common.response.Success;
import floud.demo.domain.*;
import floud.demo.domain.enums.AlarmType;
import floud.demo.dto.mypage.MypageUpdateRequestDto;
import floud.demo.dto.mypage.*;
import floud.demo.dto.mypage.dto.*;
import floud.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MyPageService {
    private final AuthService authService;
    private final UsersRepository usersRepository;
    private final GoalRepository goalRepository;
    private final FriendshipRepository friendshipRepository;
    private final AlarmRepository alarmRepository;
    private final CommunityRepository communityRepository;


    /**
     * 내 정보
     **/
    @Transactional
    public ApiResponse<?> getMyPage(String authorizationHeader){
        //Get user
        Users users = authService.findUserByToken(authorizationHeader);

        //set GoalList
        List<MyGoal> goalList = setGoalList(users.getId());

        return ApiResponse.success(Success.GET_MYPAGE_SUCCESS, MypageResponseDto.builder()
                .nickname(users.getNickname())
                .introduction(users.getIntroduction())
                .goalList(goalList)
                .build());
    }

    @Transactional
    public ApiResponse<?> checkDuplicatedName(String authorizationHeader, String nickname){
        return ApiResponse.success(Success.CHECK_NICKNAME_DUPLICATED, Map.of("isDuplicated", checkNicknameDuplicated(nickname)));
    }

    @Transactional
    public ApiResponse<?> updateMyPage(String authorizationHeader, MypageUpdateRequestDto requestDto){
        //Get user
        Users users = authService.findUserByToken(authorizationHeader);

        //Check Nickname Duplicated
        if(checkNicknameDuplicated(requestDto.getNickname()))
                return ApiResponse.failure(Error.NICKNAME_ALREADY_EXIST);

        //Update User info
        users.updateUserInfo(requestDto);
        usersRepository.save(users);

        //Update Goals
        updateGoal(users, requestDto.getGoalList());

        //set GoalList
        List<MyGoal> updatedGoalList = setGoalList(users.getId());

        //MypageResonsedto 리턴하기
        return ApiResponse.success(Success.UPDATE_MYPAGE_SUCCESS, MypageResponseDto.builder()
                .nickname(users.getNickname())
                .introduction(users.getIntroduction())
                .goalList(updatedGoalList)
                .build());
    }

    /**
     * 내가 쓴 글
     **/
    @Transactional
    public ApiResponse<?> getMyCommunity(String authorizationHeader){
        //Get user
        Users users = authService.findUserByToken(authorizationHeader);

        return ApiResponse.success(Success.GET_MYPAGE_COMMUNITY_SUCCESS, CommunityResponseDto.builder()
                .nickname(users.getNickname())
                .postList(setMyPosts(users.getId()))
                .build());
    }


    /**
     * 친구 관리
     **/
    @Transactional
    public ApiResponse<?> getFriendList(String authorizationHeader){
        //Get user
        Users users = authService.findUserByToken(authorizationHeader);

        //Find Friend List
        MypageFriendListResponseDto responseDto = findFriends(users);

        return ApiResponse.success(Success.GET_FRIEND_LIST_SUCCESS,responseDto);
    }





    private List<MyGoal> setGoalList(Long users_id){
        List<Goal> goals = goalRepository.findAllByUserId(users_id);
        return goals.stream()
                .map(goal -> MyGoal.builder()
                        .goal_id(goal.getId())
                        .content(goal.getContent())
                        .deadline(goal.getDeadline())
                        .build())
                .collect(Collectors.toList());
    }

    private void updateGoal(Users users, List<UpdateGoal> requestDtoGoalList){
        List<Goal> goals = goalRepository.findAllByUserId(users.getId());

        // Delete all existing goals
        goalRepository.deleteAll(goals);

        // Add new goals
        for (UpdateGoal requestGoal: requestDtoGoalList) {
            log.info("수정된 디데이 -> {}", requestGoal.getContent());
            log.info("수정된 데드라인 -> {}", requestGoal.getDeadline());
            UpdateGoal goal = UpdateGoal.builder()
                    .content(requestGoal.getContent())
                    .deadline(requestGoal.getDeadline())
                    .build();
            Goal newGoal = goal.toEntity(users);
            goalRepository.save(newGoal);
        }
    }

    private boolean checkNicknameDuplicated(String nickname){
        return usersRepository.existsByNickname(nickname);
    }


    private List<MyPost> setMyPosts(Long user_id){
        //Get My Posts of Community
        List<Community> communityList = communityRepository.findAllByUser(user_id);
        return communityList.stream()
                .map(community -> MyPost.builder()
                        .community_id(community.getId())
                        .title(community.getTitle())
                        .content(community.getContent())
                        .postType(community.getPostType())
                        .written_at(community.getUpdated_at())
                        .build()).toList();
    }

    private MypageFriendListResponseDto findFriends(Users me){
        List<Friendship> waitingFriends = friendshipRepository.findAllByWaitingToUser(me.getId());
        List<Friendship> acceptedFriends = friendshipRepository.findAllByUsersId(me.getId());

        List<MyWaiting> myWaitingList = waitingFriends.stream()
                .map(friendship -> buildMyWaiting(friendship, me))
                .collect(Collectors.toList());

        List<MyFriend> myFriendList = acceptedFriends.stream()
                .map(friendship -> buildMyFriend(friendship, me))
                .collect(Collectors.toList());

        return MypageFriendListResponseDto.builder()
                .waitingList(myWaitingList)
                .myFriendList(myFriendList)
                .build();
    }

    private MyWaiting buildMyWaiting(Friendship friendship, Users me) {
        Users friend = getFriend(friendship, me);
        return MyWaiting.builder()
                .friendship_id(friendship.getId())
                .nickname(friend.getNickname())
                .friendshipStatus(friendship.getFriendshipStatus())
                .introduction(friend.getIntroduction())
                .build();
    }

    private MyFriend buildMyFriend(Friendship friendship, Users me) {
        Users friend = getFriend(friendship, me);
        return MyFriend.builder()
                .friendship_id(friendship.getId())
                .nickname(friend.getNickname())
                .friendshipStatus(friendship.getFriendshipStatus())
                .introduction(friend.getIntroduction())
                .build();
    }


    private Users getFriend(Friendship friendship, Users me) {
        if (friendship.getTo_user().equals(me)) {
            return friendship.getFrom_user();
        } else {
            return friendship.getTo_user();
        }
    }

    private void createAlarm(Users from_user, Users to_user){
        String message = "친구 신청이 수락되었습니다.";
        alarmRepository.save(new Alarm(from_user, to_user.getNickname(), AlarmType.FRIEND, message));
    }


}
