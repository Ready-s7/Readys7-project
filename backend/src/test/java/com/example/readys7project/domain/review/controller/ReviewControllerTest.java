package com.example.readys7project.domain.review.controller;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequestDto;
import com.example.readys7project.domain.review.dto.request.ReviewUpdateRequestDto;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.review.service.ReviewService;
import com.example.readys7project.domain.review.service.ReviewTransactionService;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.ReviewException;
import com.example.readys7project.global.security.CustomUserDetails;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ReviewController reviewController;

    @Mock
    private ReviewService reviewService;
    @Mock
    private ReviewTransactionService reviewTransactionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder().email("test@test.com").userRole(UserRole.CLIENT).build();
        mockUserDetails = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockUserDetails;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("리뷰 생성 API 테스트 - SuccessDto 구조 검증")
    void createReview_Api_Success() throws Exception {
        // given
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, "매우 만족합니다.");
        ReviewDto responseDto = ReviewDto.builder()
                .id(1L).developerId(10L).developerName("Dev").clientId(20L).clientName("Client")
                .projectId(1L).projectTitle("Project").writerRole(ReviewRole.CLIENT).rating(5)
                .comment("매우 만족합니다.").createdAt(LocalDateTime.now()).build();

        given(reviewTransactionService.createReviewWithRatingUpdate(any(ReviewRequestDto.class), anyLong(), anyString()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/v1/reviews")
                        .param("targetUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @DisplayName("개발자 기준 리뷰 조회 API 테스트")
    void getReviewsByDeveloper_Api_Success() throws Exception {
        // given
        ReviewDto responseDto = ReviewDto.builder()
                .id(1L).developerId(10L).developerName("Dev").clientId(20L).clientName("Client")
                .projectId(1L).projectTitle("Project").writerRole(ReviewRole.CLIENT).rating(5)
                .comment("Great").createdAt(LocalDateTime.now()).build();
        
        PageRequest pageRequest = PageRequest.of(0, 5);
        PageImpl<ReviewDto> page = new PageImpl<>(List.of(responseDto), pageRequest, 1);

        given(reviewService.getReviewsByDeveloper(eq(10L), any(), any(), any(), anyInt(), anyInt(), anyString()))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/reviews")
                        .param("developerId", "10")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1L));
    }

    @Test
    @DisplayName("클라이언트 기준 리뷰 조회 API 테스트")
    void getReviewsByClient_Api_Success() throws Exception {
        // given
        ReviewDto responseDto = ReviewDto.builder()
                .id(1L).developerId(10L).developerName("Dev").clientId(20L).clientName("Client")
                .projectId(1L).projectTitle("Project").writerRole(ReviewRole.DEVELOPER).rating(5)
                .comment("Great").createdAt(LocalDateTime.now()).build();
        
        PageRequest pageRequest = PageRequest.of(0, 5);
        PageImpl<ReviewDto> page = new PageImpl<>(List.of(responseDto), pageRequest, 1);

        given(reviewService.getReviewsByClient(eq(20L), any(), any(), any(), anyInt(), anyInt(), anyString()))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/reviews")
                        .param("clientId", "20")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1L));
    }

    @Test
    @DisplayName("리뷰 수정 API 테스트")
    void updateReview_Api_Success() throws Exception {
        // given
        ReviewUpdateRequestDto request = new ReviewUpdateRequestDto(4, "수정된 평점");
        ReviewDto responseDto = ReviewDto.builder().id(1L).rating(4).comment("수정된 평점").build();

        given(reviewService.updateReview(eq(1L), any(ReviewUpdateRequestDto.class), anyString()))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/v1/reviews/{reviewId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.rating").value(4));
    }

    @Test
    @DisplayName("리뷰 수정 실패 API 테스트 - 모든 필드 null (ErrorDto 구조 검증)")
    void updateReview_AllFieldsNull_Fail() throws Exception {
        // given
        ReviewUpdateRequestDto request = new ReviewUpdateRequestDto(null, null);
        given(reviewService.updateReview(anyLong(), any(), anyString()))
                .willThrow(new ReviewException(ErrorCode.REVIEW_UPDATE_DATA_NULL));

        // when & then
        mockMvc.perform(patch("/v1/reviews/{reviewId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_UPDATE_DATA_NULL"))
                .andExpect(jsonPath("$.message").value(ErrorCode.REVIEW_UPDATE_DATA_NULL.getMessage()));
    }

    @Test
    @DisplayName("리뷰 삭제 API 테스트")
    void deleteReview_Api_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/v1/reviews/{reviewId}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("@Valid 유효성 검증 실패 시 ErrorDto 응답 확인")
    void createReview_ValidationError_ReturnsErrorDto() throws Exception {
        // given - rating이 범위를 벗어남 (1~5)
        ReviewRequestDto invalidRequest = new ReviewRequestDto(1L, 10, ""); 

        // when & then
        mockMvc.perform(post("/v1/reviews")
                        .param("targetUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("리뷰 중복 작성 시 예외 처리 테스트")
    void createReview_AlreadyExists_Fail() throws Exception {
        // given
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, "매우 만족합니다.");
        given(reviewTransactionService.createReviewWithRatingUpdate(any(), anyLong(), anyString()))
                .willThrow(new ReviewException(ErrorCode.REVIEW_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/v1/reviews")
                        .param("targetUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
    }

    // controller에서 @Valid 검증. 100자를 초과하면 controller에 진입전에 validation 실패
    @Test
    @DisplayName("리뷰 생성 실패 - comment 길이가 100자를 초과하면 400 Bad Request를 반환한다")
    void createReview_comment길이초과_400응답() throws Exception {
        // given
        String longComment = "a".repeat(101);
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, longComment);

        // when & then
        mockMvc.perform(post("/v1/reviews")
                        .param("targetUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.data.comment").exists());
    }



    @Test
    @DisplayName("개발자 기준 리뷰 조회 성공 - 검색 결과가 없으면 빈 페이지를 반환한다")
    void getReviewsByDeveloper_검색결과없음_빈페이지반환() throws Exception {
        // given
        PageRequest pageRequest = PageRequest.of(0, 5);
        PageImpl<ReviewDto> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(reviewService.getReviewsByDeveloper(eq(10L), any(), any(), any(), anyInt(), anyInt(), anyString()))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/v1/reviews")
                        .param("developerId", "10")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

}
