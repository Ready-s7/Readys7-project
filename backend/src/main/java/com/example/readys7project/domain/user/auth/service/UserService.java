package com.example.readys7project.domain.user.auth.service;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.dto.request.UpdateUserInformationRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.GetUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.UpdateUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import com.example.readys7project.global.exception.domain.ClientException;
import com.example.readys7project.global.exception.domain.DeveloperException;
import com.example.readys7project.global.exception.domain.UserException;
import com.example.readys7project.global.security.CustomUserDetails;
import com.example.readys7project.global.security.refreshtoken.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final DeveloperRepository developerRepository;
    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 유저 정보 조회 (내 정보 조회)
    @Transactional(readOnly = true)
    public GetUserInformationResponseDto getUserInformation(CustomUserDetails customUserDetails) {

        // 로그인한 유저의 정보 꺼내오기
        User user = customUserDetails.getUser();

        /* 유저가 존재하는지 확인
         findById로 DB에 조회할 때 @SoftDelete 어노테이션이 탈퇴한 유저를 자동으로 필터링 해줌,
         원래는 findByIdAndActiveTrue 이런식으로 쿼리메서드를 만들어서 사용해야하지만
         @SoftDelete이 있으므로, findById로 충분*/

        // DB를 한번 더 조회하는 이유
        // 1. 최신 데이터 보장 -> 토큰 발급 이후 수정된 정보 반영
        // 2. 탈퇴 유저 필터링 ->
        User targetUser = userRepository.findById(user.getId())
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        // Dto 반환
        return new GetUserInformationResponseDto(
                targetUser.getId(),
                targetUser.getEmail(),
                targetUser.getUserRole(),
                targetUser.getDescription(),
                targetUser.getCreatedAt()
        );
    }

    // 유저 정보 수정 (내 정보 수정)
    @Transactional
    public UpdateUserInformationResponseDto updateUserInformation(
            CustomUserDetails customUserDetails,
            UpdateUserInformationRequestDto updateUserInformationRequestDto
    ) {

        // 로그인한 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 유저 존재하는지 확인
        User targetUser = userRepository.findById(user.getId())
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 유저 정보 업데이트 메서드에게 넘기기
        targetUser.updateUserInformation(
                updateUserInformationRequestDto.name(),
                updateUserInformationRequestDto.phoneNumber(),
                updateUserInformationRequestDto.description());

        /* DB에 수정한 유저 정보 저장
         updatedAt을 DB에 바로 반영해주기 위해서 saveAndFlush 사용*/
        userRepository.saveAndFlush(targetUser);

        // Dto 리턴
        return new UpdateUserInformationResponseDto(
                targetUser.getId(),
                targetUser.getEmail(),
                targetUser.getName(),
                targetUser.getPhoneNumber(),
                targetUser.getDescription(),
                targetUser.getUpdatedAt()
        );
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(CustomUserDetails customUserDetails) {

        // 로그인한 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 삭제할 유저가 존재하는지 확인
        User targetUser = userRepository.findById(user.getId())
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

         /*
         1. 역할별 연관 데이터를 같이 삭제할건지?
         -> User를 SoftDelete = true,
         Client, Developer, Admin도 같이 SoftDelete = true
         장점 : 데이터 정합성이 깔끔, 탈퇴한 유저의 프로필이 남아있지 않음
         -> if문으로 분기처리하여 Client, Developer, Admin도 레포에 삭제


         User만 Soft Delete 하고 나머지는 그대로 둘건지?
         -> User = SoftDelete = true
         Client, Developer, Admin은 SoftDelete = false
         장점 : 탈퇴 후 재가입 시 이전 데이터 복구 가능, 단순 구현으로 가능
         -> if문 분기처리 할 필요 없이 user레포에 delete하면 됨
         */

        // Client인 경우, clientRepository에서 찾아보고 회원 삭제
        if (user.getUserRole() == UserRole.CLIENT) {
            Client client = clientRepository.findByUser(targetUser)
                            .orElseThrow( () -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

            clientRepository.deleteById(client.getId());
        }

        // Developer인 경우, clientRepository에서 찾아보고 회원 삭제
        if (user.getUserRole() == UserRole.DEVELOPER) {
            Developer developer = developerRepository.findByUser(targetUser)
                            .orElseThrow( () -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));

            developerRepository.deleteById(developer.getId());
        }

        // Admin인 경우, adminRepository에서 찾아보고 회원 삭제
        if (user.getUserRole() == UserRole.ADMIN) {
            Admin admin = adminRepository.findByUser(targetUser)
                            .orElseThrow( () -> new AdminException(ErrorCode.ADMIN_NOT_FOUND));

            adminRepository.deleteById(admin.getId());
        }


        // RefreshToken 삭제
        refreshTokenRepository.deleteByEmail(targetUser.getEmail());

        /* @SoftDelete 어노테이션이 있으므로, DB에서 실제 DELETE 쿼리가 아니라,
         UPDATE users SET deleted = true 이런식으로 동작함*/
        userRepository.delete(targetUser);
    }
}
