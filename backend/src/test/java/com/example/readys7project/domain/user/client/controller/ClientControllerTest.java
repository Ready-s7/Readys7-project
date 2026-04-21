package com.example.readys7project.domain.user.client.controller;

import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.client.dto.request.UpdateClientProfileRequestDto;
import com.example.readys7project.domain.user.client.dto.response.ClientsResponseDto;
import com.example.readys7project.domain.user.client.dto.response.UpdateClientProfileResponseDto;
import com.example.readys7project.domain.user.client.service.ClientService;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.global.dto.PageResponseDto;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.ClientException;
import com.example.readys7project.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ClientController clientController;

    @Mock
    private ClientService clientService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@test.com")
                .name("테스트")
                .userRole(UserRole.CLIENT)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        customUserDetails = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
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
                        },
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Nested
    @DisplayName("클라이언트 목록 조회 API 테스트")
    class GetClientsApiTest {
        @Test
        @DisplayName("성공: 클라이언트 목록을 조회한다")
        void getClients_success() throws Exception {
            // given
            ClientsResponseDto dto = ClientsResponseDto.builder()
                    .id(1L)
                    .name("테스트")
                    .title("테스트 클라이언트")
                    .build();
            PageResponseDto<ClientsResponseDto> response = PageResponseDto.of(
                    new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                    List.of(dto)
            );

            given(clientService.getClients(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/v1/clients")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1L));
        }
    }

    @Nested
    @DisplayName("클라이언트 상세 조회 API 테스트")
    class GetClientDetailApiTest {
        @Test
        @DisplayName("성공: 클라이언트 상세 정보를 조회한다")
        void getClientDetail_success() throws Exception {
            // given
            ClientsResponseDto dto = ClientsResponseDto.builder()
                    .id(1L)
                    .name("테스트")
                    .title("테스트 클라이언트")
                    .build();

            given(clientService.getClientDetail(eq(1L), any())).willReturn(dto);

            // when & then
            mockMvc.perform(get("/v1/clients/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 클라이언트면 404를 반환한다")
        void getClientDetail_fail_notFound() throws Exception {
            // given
            given(clientService.getClientDetail(eq(1L), any()))
                    .willThrow(new ClientException(ErrorCode.CLIENT_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/v1/clients/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CLIENT_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("클라이언트 프로필 수정 API 테스트")
    class UpdateClientProfileApiTest {
        @Test
        @DisplayName("성공: 클라이언트 프로필을 수정한다")
        void updateClientProfile_success() throws Exception {
            // given
            UpdateClientProfileRequestDto requestDto = new UpdateClientProfileRequestDto("새 제목", ParticipateType.COMPANY);
            UpdateClientProfileResponseDto responseDto = UpdateClientProfileResponseDto.builder()
                    .clientId(1L)
                    .title("새 제목")
                    .participateType(ParticipateType.COMPANY)
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(clientService.updateClientProfile(eq(1L), any(), any())).willReturn(responseDto);

            // when & then
            mockMvc.perform(patch("/v1/clients/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("새 제목"));
        }
    }

    @Nested
    @DisplayName("내 프로젝트 목록 조회 API 테스트")
    class GetMyProjectsApiTest {
        @Test
        @DisplayName("성공: 내 프로젝트 목록을 조회한다")
        void getMyProjects_success() throws Exception {
            // given
            PageResponseDto response = PageResponseDto.builder()
                    .content(Collections.emptyList())
                    .currentPage(1)
                    .size(10)
                    .totalCount(0L)
                    .totalPage(0)
                    .build();

            given(clientService.getMyProjects(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/v1/clients/my-projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
