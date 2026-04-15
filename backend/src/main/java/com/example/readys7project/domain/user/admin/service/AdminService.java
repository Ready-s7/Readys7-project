package com.example.readys7project.domain.user.admin.service;

import com.example.readys7project.domain.user.admin.dto.request.UpdateAdminStatusRequestDto;
import com.example.readys7project.domain.user.admin.dto.response.AdminSummaryResponseDto;
import com.example.readys7project.domain.user.admin.dto.response.GetAllAdminListResponseDto;
import com.example.readys7project.domain.user.admin.dto.response.UpdateAdminStatusResponseDto;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor

public class AdminService {

    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public GetAllAdminListResponseDto getAllPendingAdminList(CustomUserDetails customUserDetails, Pageable pageable) {

        // customUserDetails 에서 로그인한 User 정보 꺼내오기
        User user = customUserDetails.getUser();

        // 해당 유저 중 Admin이 존재하는지 DB에 찾아보기
        Admin admin = adminRepository.findByUser(user)
                .orElseThrow(() -> new AdminException(ErrorCode.ADMIN_NOT_FOUND));

        // 관리자 역할이 슈퍼어드민이 아니면 예외던지기
        if(!admin.getAdminRole().equals(AdminRole.SUPER_ADMIN)) {
            throw new AdminException(ErrorCode.INVALID_ADMIN_ROLE);
        }

        /* PageRequest.of() -> PageRequest 객체를 만들어주는 정적 팩토리 메서드 (Spring Data가 제공)
         getPageNumber -> 몇 번째 페이지인지 (페이지 번호)
         getPageSize -> 한 페이지에 몇개씩 보여줄건지 (페이지 크기)
         컴퓨터의 시작은 0이므로, DB에 넘겨주기 전에 페이지 번호 -1 빼주기
         반환할 때는 다시 +1 해서 사용자에게는 1페이지부터 보여주기
         */
        Pageable convertPageable = PageRequest.of(
                pageable.getPageNumber() - 1,
                pageable.getPageSize()
        );

        // PENDING 상태 관리자 목록 조회
        Page<Admin> adminPage = adminRepository.findAllByStatus(AdminStatus.PENDING, convertPageable);

        // 각 Admin을 AdminSummaryResponseDto로 변환 (adminPage가 Admin이니까)
        List<AdminSummaryResponseDto> adminList = adminPage.getContent().stream()
                .map(AdminSummaryResponseDto::new)
                .toList();

        // ResponseDto로 리턴
        return new GetAllAdminListResponseDto(
                adminList,
                adminPage.getNumber() + 1,
                adminPage.getSize(),
                adminPage.getTotalElements(),
                adminPage.getTotalPages()
        );
    }

    @Transactional
    public UpdateAdminStatusResponseDto updateAdminStatus(
            Long adminId,
            UpdateAdminStatusRequestDto updateAdminStatusRequestDto,
            CustomUserDetails customUserDetails
    ) {

        // customUserDetails에서 User 꺼내오기
        User user = customUserDetails.getUser();

        // adminRepository에 User 정보로 관리자 조회하기
        Admin admin = adminRepository.findByUser(user)
                .orElseThrow(() -> new AdminException(ErrorCode.ADMIN_NOT_FOUND));

        // SuperAdmin인지 체크
        if (admin.getAdminRole() != AdminRole.SUPER_ADMIN || admin.getStatus() != AdminStatus.APPROVED) {
            throw new AdminException(ErrorCode.USER_FORBIDDEN);
        }

        // 승인받아야하는 관리자 조회
        Admin targetAdmin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminException(ErrorCode.ADMIN_NOT_FOUND));

        // Pending 상태가 맞는지 검증
        if(!(targetAdmin.getStatus().equals(AdminStatus.PENDING))) {
            throw new AdminException(ErrorCode.ADMIN_STATUS_NOT_MATCH);
        }

        // 승인 메서드 호출
        if (updateAdminStatusRequestDto.adminStatus() == AdminStatus.APPROVED) {
            targetAdmin.AdminStatusApprove();
        } else if (updateAdminStatusRequestDto.adminStatus() == AdminStatus.REJECTED) {
            targetAdmin.AdminStatusReject();
        } else {
            throw new AdminException(ErrorCode.INVALID_ADMIN_STATUS);
        }

        /* saveAndFlush -> Spring Data JPA가 제공하는 메서드,
         그냥 save만 하게되면, 변경 사항을 1차 캐시에만 저장해놓고, 실제 DB에는 반영이 안됨,
         트랜잭션 커밋 시점에 실제 DB에 반영이 됨,
         그러다보니 return 시점에는 approveAdmin를 호출하기 전 값으로 반영될 수 있기 때문에,
         saveAndFlush로 DB에 당장 반영해버리기
         (save() + flush()를 한번에 처리)
         */
        adminRepository.saveAndFlush(targetAdmin);

        return new UpdateAdminStatusResponseDto(
                targetAdmin.getId(),
                targetAdmin.getUser().getName(),
                targetAdmin.getAdminRole(),
                targetAdmin.getStatus(),
                targetAdmin.getUpdatedAt()
        );

        /* 전체 흐름
         customUserDetails -> Spring Security의 @AuthenticationPrincipal을 사용하기 위해  UserDetails 인터페이스를 구현한 클래스인데,
         Jwt 토큰 검증 후 SecurityContextHolder에 저장된 로그인한 유저의 정보를 꺼내올 수 있음.
         -> Controller 에서 꺼냄

         1. customUserDetails 안에 저장되어있는 User 정보를 getUser로 가져옴,
         2. User 중에서 관리자가 있는지 adminRepository에 조회 해보기
         3. 해당 User가 SuperAdmin이 맞는지 체크
         4. 승인받아야하는 관리자가 있는지 조회
         5. 있으면 승인 메서드 호출해서 넘기기 */

    }
}
