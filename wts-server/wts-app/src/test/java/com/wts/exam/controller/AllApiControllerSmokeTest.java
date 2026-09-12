package com.wts.exam.controller;

import com.wts.auth.controller.AuthController;
import com.wts.auth.controller.HealthController;
import com.wts.auth.controller.OrganizationController;
import com.wts.auth.controller.UserController;
import com.wts.auth.dto.LoginDTO;
import com.wts.auth.dto.LoginVO;
import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysOrganization;
import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.service.AuthService;
import com.wts.auth.service.OrganizationService;
import com.wts.auth.service.PermissionService;
import com.wts.auth.service.UserService;
import com.wts.common.result.PageResult;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
import com.wts.exam.dto.CardSubmitDTO;
import com.wts.exam.dto.ExamPaperVO;
import com.wts.exam.dto.JudgeDTO;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.dto.RandomItemDTO;
import com.wts.exam.dto.RoomDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.dto.SubjectQueryDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamPaperChapter;
import com.wts.exam.entity.ExamPaperSubject;
import com.wts.exam.entity.ExamRandomItem;
import com.wts.exam.entity.ExamRandomStep;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.entity.ExamRoomPaper;
import com.wts.exam.entity.ExamRoomUser;
import com.wts.exam.entity.ExamSubject;
import com.wts.exam.entity.ExamSubjectType;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.mapper.ExamCardMapper;
import com.wts.exam.mapper.ExamPaperMapper;
import com.wts.exam.mapper.ExamRoomMapper;
import com.wts.exam.mapper.ExamSubjectMapper;
import com.wts.exam.service.CardService;
import com.wts.exam.service.PaperService;
import com.wts.exam.service.RandomService;
import com.wts.exam.service.RoomService;
import com.wts.exam.service.SubjectImportService;
import com.wts.exam.service.SubjectService;
import com.wts.exam.service.SubjectTypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllApiControllerSmokeTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private SubjectTypeService subjectTypeService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private SubjectImportService subjectImportService;
    @Mock
    private PaperService paperService;
    @Mock
    private RoomService roomService;
    @Mock
    private CardService cardService;
    @Mock
    private RandomService randomService;
    @Mock
    private ExamSubjectMapper subjectMapper;
    @Mock
    private ExamPaperMapper paperMapper;
    @Mock
    private ExamRoomMapper roomMapper;
    @Mock
    private ExamCardMapper cardMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PermissionService permissionService;

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    @BeforeEach
    void setUp() {
        authenticate("admin-1", "admin", "Admin One", "1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void healthAndAuthEndpointsAreCallable() {
        AuthController authController = new AuthController(authService);
        HealthController healthController = new HealthController();
        LoginUserDetails loginUser = loginUser("admin-1", "admin", "Admin One", "1");
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken("access-token");
        loginVO.setRefreshToken("refresh-token");

        when(authService.login(any(LoginDTO.class))).thenReturn(loginVO);
        when(authService.refreshToken("refresh-token")).thenReturn(loginVO);
        when(authService.getCurrentUser("admin-1")).thenReturn(sysUser("admin-1"));
        when(authService.getUserMenus("admin-1")).thenReturn(List.of());

        assertOk(healthController.health());
        assertOk(authController.login(new LoginDTO()));
        assertOk(authController.refresh(Map.of("refreshToken", "refresh-token")));
        assertOk(authController.logout());
        assertOk(authController.getCurrentUser(loginUser));
        assertOk(authController.getMenus(loginUser));
    }

    @Test
    void userEndpointsAreCallable() {
        UserController controller = new UserController(userService, permissionService, currentUserProvider);
        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.ms-excel", "content".getBytes());
        com.wts.auth.dto.BatchIdsDTO batchIds = authBatchIds("user-1", "user-2");

        when(userService.listUsers(1, 20, "kw", "1", null, null, null)).thenReturn(PageResult.of(List.of(sysUser("user-1")), 1, 1, 20));
        when(permissionService.visibleUserIds(any())).thenReturn(null);
        when(userService.createUser(any(UserDTO.class), eq("admin-1"))).thenReturn(sysUser("user-1"));
        when(userService.importStudents(any(InputStream.class), eq("admin-1"))).thenReturn(new StudentImportResult());
        when(userService.updateUser(eq("user-1"), any(UserDTO.class), eq("admin-1"))).thenReturn(sysUser("user-1"));

        assertOk(controller.list(1, 20, "kw", "1", null, null));
        assertOk(controller.create(new UserDTO()));
        assertOk(controller.importStudents(file));
        assertOk(controller.update("user-1", new UserDTO()));
        assertOk(controller.delete("user-1"));
        assertOk(controller.batchDisable(batchIds));
        assertOk(controller.hardDelete("user-1"));
        assertOk(controller.batchHardDelete(batchIds));
        assertOk(controller.resetPassword("user-1"));
        assertOk(controller.changePassword(Map.of("oldPassword", "old-pass", "newPassword", "new-pass")));

        verify(userService).disableUsers(List.of("user-1", "user-2"), "admin-1");
        verify(userService).hardDeleteUsers(List.of("user-1", "user-2"), "admin-1");
    }

    @Test
    void organizationEndpointsAreCallable() {
        OrganizationController controller = new OrganizationController(organizationService, permissionService, currentUserProvider);
        LoginUserDetails loginUser = loginUser("admin-1", "admin", "Admin One", "1");

        when(permissionService.visibleOrgIds(any())).thenReturn(null);
        when(organizationService.getOrgTree(any())).thenReturn(List.of());
        when(organizationService.createOrganization(any(SysOrganization.class), eq("admin-1"))).thenReturn(new SysOrganization());
        when(organizationService.updateOrganization(eq("org-1"), any(SysOrganization.class), eq("admin-1"))).thenReturn(new SysOrganization());

        assertOk(controller.getTree());
        assertOk(controller.create(new SysOrganization(), loginUser));
        assertOk(controller.update("org-1", new SysOrganization(), loginUser));
        assertOk(controller.delete("org-1", loginUser));
    }

    @Test
    void subjectTypeEndpointsAreCallable() {
        SubjectTypeController controller = new SubjectTypeController(subjectTypeService, currentUserProvider);

        when(subjectTypeService.getTree()).thenReturn(List.of());

        assertOk(controller.tree());
        assertOk(controller.create(new ExamSubjectType()));
        assertOk(controller.update("type-1", new ExamSubjectType()));
        assertOk(controller.delete("type-1"));
        assertOk(controller.batchDelete(examBatchIds("type-1", "type-2")));
    }

    @Test
    void subjectEndpointsAreCallable() {
        SubjectController controller = new SubjectController(subjectService, subjectImportService, currentUserProvider, permissionService);
        MockMultipartFile file = new MockMultipartFile("file", "subjects.xlsx", "application/vnd.ms-excel", "content".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(subjectService.list(any(SubjectQueryDTO.class), isNull())).thenReturn(PageResult.of(List.of(new ExamSubject()), 1, 1, 20));
        when(permissionService.visibleOwnerIds(any())).thenReturn(null);
        when(subjectService.getDetail("subject-1")).thenReturn(new ExamSubject());
        when(subjectService.getCurrentVersion("subject-1")).thenReturn(new ExamSubjectVersion());
        when(subjectService.create(any(SubjectDTO.class), eq("admin-1"), eq("Admin One"))).thenReturn(new ExamSubject());
        when(subjectImportService.importFromExcel(any(InputStream.class), eq("type-1"), eq("admin-1"), eq("Admin One")))
                .thenReturn(Map.of("created", 1));

        assertOk(controller.list(new SubjectQueryDTO()));
        assertOk(controller.detail("subject-1"));
        assertOk(controller.create(new SubjectDTO()));
        assertOk(controller.update("subject-1", new SubjectDTO()));
        assertOk(controller.delete("subject-1"));
        assertOk(controller.batchDelete(examBatchIds("subject-1", "subject-2")));
        assertOk(controller.importExcel(file, "type-1"));
        controller.exportExcel(response);

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        verify(subjectImportService).exportToExcel(any());
    }

    @Test
    void paperEndpointsAreCallable() {
        PaperController controller = new PaperController(paperService, currentUserProvider, permissionService);

        when(paperService.list(1, 20, "paper", null)).thenReturn(PageResult.of(List.of(new ExamPaper()), 1, 1, 20));
        when(permissionService.visibleOwnerIds(any())).thenReturn(null);
        when(paperService.getDetail("paper-1")).thenReturn(new ExamPaper());
        when(paperService.create(any(PaperDTO.class), eq("admin-1"), eq("Admin One"))).thenReturn(new ExamPaper());
        when(paperService.getChapters("paper-1")).thenReturn(List.of(new ExamPaperChapter()));
        when(paperService.getPaperSubjects("paper-1")).thenReturn(List.of(new ExamPaperSubject()));

        assertOk(controller.list(1, 20, "paper"));
        assertOk(controller.detail("paper-1"));
        assertOk(controller.create(new PaperDTO()));
        assertOk(controller.update("paper-1", new PaperDTO()));
        assertOk(controller.addSubject("paper-1", "subject-1", "version-1", null, null, 1));
        assertOk(controller.getChapters("paper-1"));
        assertOk(controller.getPaperSubjects("paper-1"));
        assertOk(controller.delete("paper-1"));
        assertOk(controller.batchDelete(examBatchIds("paper-1", "paper-2")));

        verify(paperService).addSubject("paper-1", "subject-1", "version-1", null, null, 1);
    }

    @Test
    void roomEndpointsAreCallable() {
        RoomController controller = new RoomController(roomService, currentUserProvider, permissionService);

        when(roomService.list(1, 20, "room", "21", null)).thenReturn(PageResult.of(List.of(new ExamRoom()), 1, 1, 20));
        when(permissionService.visibleOwnerIds(any())).thenReturn(null);
        when(roomService.listMyRooms(1, 20, "admin-1", "room", "21")).thenReturn(PageResult.of(List.of(new ExamRoom()), 1, 1, 20));
        when(roomService.getDetail("room-1")).thenReturn(new ExamRoom());
        when(roomService.create(any(RoomDTO.class), eq("admin-1"), eq("Admin One"))).thenReturn(new ExamRoom());
        when(roomService.getRoomPapers("room-1")).thenReturn(List.of(new ExamRoomPaper()));
        when(roomService.getAssignedUsers("room-1")).thenReturn(List.of(new ExamRoomUser()));

        assertOk(controller.list(1, 20, "room", "21"));
        assertOk(controller.myRooms(1, 20, "room", "21"));
        assertOk(controller.detail("room-1"));
        assertOk(controller.create(new RoomDTO()));
        assertOk(controller.update("room-1", new RoomDTO()));
        assertOk(controller.publish("room-1"));
        assertOk(controller.batchPublish(examBatchIds("room-1", "room-2")));
        assertOk(controller.close("room-1"));
        assertOk(controller.batchClose(examBatchIds("room-1", "room-2")));
        assertOk(controller.addPaper("room-1", "paper-1", "paper", 60F));
        assertOk(controller.getRoomPapers("room-1"));
        assertOk(controller.getAssignedUsers("room-1"));
        assertOk(controller.assignUsers("room-1", List.of("user-1", "user-2")));
        assertOk(controller.delete("room-1"));
        assertOk(controller.batchDelete(examBatchIds("room-1", "room-2")));
    }

    @Test
    void cardEndpointsAreCallable() {
        CardController controller = new CardController(cardService, currentUserProvider);

        when(cardService.enterRoom("room-1", "admin-1", "Admin One", true)).thenReturn(new ExamCard());
        when(cardService.getResult("card-1", "admin-1", true)).thenReturn(new ExamCard());
        when(cardService.getCardAnswers("card-1")).thenReturn(List.of());
        when(cardService.getCardPoints("card-1")).thenReturn(List.of());
        when(cardService.submit(eq("card-1"), any(CardSubmitDTO.class), eq("admin-1"))).thenReturn(new ExamCard());
        when(cardService.getExamPaper("card-1", "admin-1")).thenReturn(new ExamPaperVO());
        when(cardService.getExamPaperForReview("card-1")).thenReturn(new ExamPaperVO());
        when(cardService.getRoomCards("room-1", 1, 20)).thenReturn(PageResult.of(List.of(new ExamCard()), 1, 1, 20));

        assertOk(controller.enterRoom("room-1"));
        assertOk(controller.detail("card-1"));
        assertOk(controller.save("card-1", new CardSubmitDTO()));
        assertOk(controller.submit("card-1", new CardSubmitDTO()));
        assertOk(controller.judge("card-1", new JudgeDTO()));
        assertOk(controller.batchJudge(examBatchIds("card-1", "card-2")));
        assertOk(controller.getExamPaper("card-1"));
        assertOk(controller.getExamPaperForReview("card-1"));
        assertOk(controller.getRoomCards("room-1", 1, 20));
    }

    @Test
    void randomEndpointsAreCallable() {
        RandomController controller = new RandomController(randomService, currentUserProvider, permissionService);
        RandomItemDTO itemDTO = new RandomItemDTO();
        RandomItemDTO.RandomStepDTO stepDTO = new RandomItemDTO.RandomStepDTO();

        when(randomService.listItems()).thenReturn(List.of(new ExamRandomItem()));
        when(randomService.createItem(any(RandomItemDTO.class), eq("admin-1"))).thenReturn(new ExamRandomItem());
        when(randomService.updateItem(eq("item-1"), any(RandomItemDTO.class))).thenReturn(new ExamRandomItem());
        when(randomService.getSteps("item-1")).thenReturn(List.of(new ExamRandomStep()));
        when(randomService.addStep(eq("item-1"), any(RandomItemDTO.RandomStepDTO.class))).thenReturn(new ExamRandomStep());
        when(randomService.updateStep(eq("step-1"), any(RandomItemDTO.RandomStepDTO.class))).thenReturn(new ExamRandomStep());
        when(randomService.generatePapers("item-1", 2, "admin-1")).thenReturn(List.of("paper-1", "paper-2"));

        assertOk(controller.listItems());
        assertOk(controller.createItem(itemDTO));
        assertOk(controller.updateItem("item-1", itemDTO));
        assertOk(controller.deleteItem("item-1"));
        assertOk(controller.batchDeleteItems(examBatchIds("item-1", "item-2")));
        assertOk(controller.getSteps("item-1"));
        assertOk(controller.addStep("item-1", stepDTO));
        assertOk(controller.updateStep("step-1", stepDTO));
        assertOk(controller.deleteStep("step-1"));
        assertOk(controller.batchDeleteSteps(examBatchIds("step-1", "step-2")));
        assertOk(controller.generate("item-1", 2));
    }

    @Test
    void dashboardEndpointIsCallable() {
        DashboardController controller = new DashboardController(
                subjectMapper,
                paperMapper,
                roomMapper,
                cardMapper,
                userMapper,
                currentUserProvider
        );

        when(subjectMapper.selectCount(any())).thenReturn(1L);
        when(paperMapper.selectCount(any())).thenReturn(2L);
        when(roomMapper.selectCount(any())).thenReturn(3L);
        when(cardMapper.countFinishedCardsWithExistingRoom()).thenReturn(4L);
        when(userMapper.selectCount(any())).thenReturn(5L);

        R<?> response = controller.stats();

        assertOk(response);
        assertNotNull(response.getData());
    }

    private static void assertOk(R<?> response) {
        assertNotNull(response);
        assertEquals(200, response.getCode());
    }

    private static LoginUserDetails loginUser(String userId, String loginName, String name, String userType) {
        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(userId);
        details.setLoginName(loginName);
        details.setName(name);
        details.setUserType(userType);
        return details;
    }

    private static void authenticate(String userId, String loginName, String name, String userType) {
        LoginUserDetails details = loginUser(userId, loginName, name, userType);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }

    private static SysUser sysUser(String id) {
        SysUser user = new SysUser();
        user.setId(id);
        return user;
    }

    private static com.wts.auth.dto.BatchIdsDTO authBatchIds(String... ids) {
        com.wts.auth.dto.BatchIdsDTO dto = new com.wts.auth.dto.BatchIdsDTO();
        dto.setIds(List.of(ids));
        return dto;
    }

    private static com.wts.exam.dto.BatchIdsDTO examBatchIds(String... ids) {
        com.wts.exam.dto.BatchIdsDTO dto = new com.wts.exam.dto.BatchIdsDTO();
        dto.setIds(List.of(ids));
        return dto;
    }
}
