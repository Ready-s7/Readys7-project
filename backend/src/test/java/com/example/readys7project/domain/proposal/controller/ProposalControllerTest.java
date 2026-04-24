package com.example.readys7project.domain.proposal.controller;

import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.request.UpdateProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.response.ProposalResponseDto;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.service.ProposalService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.ProposalException;
import com.example.readys7project.global.security.CustomUserDetails;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProposalControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ProposalController proposalController;

    @Mock
    private ProposalService proposalService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("dev@test.com")
                .name("Developer")
                .userRole(UserRole.DEVELOPER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        customUserDetails = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders.standaloneSetup(proposalController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return customUserDetails;
                            }
                        }
                )
                .build();
    }

    @Test
    @DisplayName("성공: 제안서 생성 API를 호출하면 201 상태코드를 반환한다")
    void createProposal_Success() throws Exception {
        // given
        ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");
        ProposalResponseDto response = ProposalResponseDto.builder()
                .id(1L)
                .projectId(1L)
                .status("PENDING")
                .build();

        given(proposalService.createProposal(any(ProposalRequestDto.class), eq("dev@test.com"))).willReturn(response);

        // when & then
        mockMvc.perform(post("/v1/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("실패: 제안 슬롯이 가득 찬 경우 400 상태코드를 반환한다")
    void createProposal_Fail_SlotFull() throws Exception {
        // given
        ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");
        given(proposalService.createProposal(any(ProposalRequestDto.class), anyString()))
                .willThrow(new ProposalException(ErrorCode.PROPOSAL_SLOT_FULL));

        // when & then
        mockMvc.perform(post("/v1/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROPOSAL_SLOT_FULL"))
                .andExpect(jsonPath("$.message").value(ErrorCode.PROPOSAL_SLOT_FULL.getMessage()));
    }

    @Test
    @DisplayName("성공: 프로젝트별 제안서 목록 조회 API를 호출하면 200 상태코드를 반환한다")
    void getProposalsByProject_Success() throws Exception {
        // given
        ProposalResponseDto response = ProposalResponseDto.builder().id(1L).build();
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageImpl<ProposalResponseDto> page = new PageImpl<>(List.of(response), pageRequest, 1);

        given(proposalService.getProposalsByProject(eq(1L), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/proposals")
                        .param("projectId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @DisplayName("성공: 제안서 상세 조회 API를 호출하면 200 상태코드를 반환한다")
    void getProposal_Success() throws Exception {
        // given
        ProposalResponseDto response = ProposalResponseDto.builder().id(1L).build();
        given(proposalService.getProposal(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/v1/proposals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("성공: 내 제안서 목록 조회 API를 호출하면 200 상태코드를 반환한다")
    void getMyProposals_Success() throws Exception {
        // given
        ProposalResponseDto response = ProposalResponseDto.builder().id(1L).build();
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageImpl<ProposalResponseDto> page = new PageImpl<>(List.of(response), pageRequest, 1);

        given(proposalService.getMyProposals(eq("dev@test.com"), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/proposals/my-proposals")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @DisplayName("성공: 제안서 상태 변경 API를 호출하면 200 상태코드를 반환한다")
    void updateProposalStatus_Success() throws Exception {
        // given
        UpdateProposalRequestDto request = new UpdateProposalRequestDto(ProposalStatus.ACCEPTED);
        ProposalResponseDto response = ProposalResponseDto.builder()
                .id(1L)
                .status("ACCEPTED")
                .build();

        given(proposalService.updateProposalStatus(eq(1L), any(UpdateProposalRequestDto.class), eq("dev@test.com")))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/v1/proposals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }
}
