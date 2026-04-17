package com.example.readys7project.global.aop;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProjectException;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class CheckOwnerOrAdminAspect {

    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;
    private final ReviewRepository reviewRepository;

    @Before("@annotation(checkOwnerOrAdmin)")
    public void checkOwnership(JoinPoint joinPoint, CheckOwnerOrAdmin checkOwnerOrAdmin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new ProjectException(ErrorCode.USER_NOT_FOUND);
        }

        User currentUser = userDetails.getUser();

        // 1. 관리자(ADMIN)인 경우 즉시 통과
        if (currentUser.getUserRole() == UserRole.ADMIN) {
            return;
        }

        // 2. 파라미터에서 ID 추출
        Long resourceId = extractResourceId(joinPoint, checkOwnerOrAdmin.idParam());

        // 3. 타입별 소유권 검증
        boolean isOwner = switch (checkOwnerOrAdmin.type()) {
            case PROJECT -> validateProjectOwner(resourceId, currentUser.getId());
            case PROPOSAL -> validateProposalOwner(resourceId, currentUser.getId());
            case REVIEW -> validateReviewOwner(resourceId, currentUser.getId());
        };

        if (!isOwner) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private Long extractResourceId(JoinPoint joinPoint, String paramName) {
        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                return (Long) args[i];
            }
        }
        throw new ProjectException(ErrorCode.INVALID_INPUT);
    }

    private boolean validateProjectOwner(Long id, Long userId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));
        return project.getClient().getUser().getId().equals(userId);
    }

    private boolean validateProposalOwner(Long id, Long userId) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROPOSAL_NOT_FOUND));
        
        // 제안서 작성자이거나, 프로젝트 소유자여야 함
        boolean isProposalAuthor = proposal.getDeveloper().getUser().getId().equals(userId);
        boolean isProjectOwner = proposal.getProject().getClient().getUser().getId().equals(userId);
        
        return isProposalAuthor || isProjectOwner;
    }

    private boolean validateReviewOwner(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.REVIEW_NOT_FOUND));

        if (review.getWriterRole() == ReviewRole.CLIENT) {
            return review.getClient().getUser().getId().equals(userId);
        } else {
            return review.getDeveloper().getUser().getId().equals(userId);
        }
    }
}
